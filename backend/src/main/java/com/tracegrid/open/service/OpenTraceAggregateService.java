package com.tracegrid.open.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tracegrid.common.exception.BusinessException;
import com.tracegrid.dist.entity.DistInoutRecord;
import com.tracegrid.dist.entity.DistLogisticsNode;
import com.tracegrid.dist.entity.DistLogisticsRecord;
import com.tracegrid.dist.entity.DistSaleRecord;
import com.tracegrid.dist.mapper.DistInoutRecordMapper;
import com.tracegrid.dist.mapper.DistLogisticsNodeMapper;
import com.tracegrid.dist.mapper.DistLogisticsRecordMapper;
import com.tracegrid.dist.mapper.DistSaleRecordMapper;
import com.tracegrid.proc.entity.ProcInventoryRecord;
import com.tracegrid.proc.entity.ProcProductBatch;
import com.tracegrid.proc.entity.ProcQualityInspection;
import com.tracegrid.proc.entity.ProcRawInbound;
import com.tracegrid.proc.mapper.ProcInventoryRecordMapper;
import com.tracegrid.proc.mapper.ProcProductBatchMapper;
import com.tracegrid.proc.mapper.ProcQualityInspectionMapper;
import com.tracegrid.proc.mapper.ProcRawInboundMapper;
import com.tracegrid.prod.entity.ProdBatch;
import com.tracegrid.prod.entity.ProdFarmingRecord;
import com.tracegrid.prod.mapper.ProdBatchMapper;
import com.tracegrid.prod.mapper.ProdFarmingRecordMapper;
import com.tracegrid.prod.mapper.ProdPlotMapper;
import com.tracegrid.sys.entity.SysOrg;
import com.tracegrid.sys.mapper.SysOrgMapper;
import com.tracegrid.trace.entity.TraceCode;
import com.tracegrid.trace.mapper.TraceCodeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 公开溯源链路数据聚合（扫码查询与 RAG 共用，避免 AI 仅能依赖向量库）。
 */
@Service
@RequiredArgsConstructor
public class OpenTraceAggregateService {

    /** 溯源档案聚合缓存（不含实时扫码次数），减轻高并发重复查库 */
    private final Cache<String, Map<String, Object>> payloadCache = Caffeine.newBuilder()
            .maximumSize(8_000)
            .expireAfterWrite(90, TimeUnit.SECONDS)
            .build();

    private final TraceCodeMapper traceCodeMapper;
    private final ProcProductBatchMapper productBatchMapper;
    private final ProcInventoryRecordMapper inventoryMapper;
    private final ProcQualityInspectionMapper qualityMapper;
    private final ProcRawInboundMapper rawInboundMapper;
    private final ProdBatchMapper prodBatchMapper;
    private final ProdPlotMapper plotMapper;
    private final ProdFarmingRecordMapper farmingMapper;
    private final SysOrgMapper orgMapper;
    private final DistLogisticsRecordMapper logisticsMapper;
    private final DistLogisticsNodeMapper logisticsNodeMapper;
    private final DistSaleRecordMapper saleMapper;
    private final DistInoutRecordMapper distInoutMapper;

    /**
     * 扫码页：递增查验次数并返回全量数据。
     */
    public Map<String, Object> aggregateWithScanUpdate(String code) {
        String normalizedCode = normalizeCode(code);
        TraceCode traceCode = loadValidTrace(normalizedCode);
        traceCodeMapper.incrementScanCount(traceCode.getId());
        TraceCode refreshed = traceCodeMapper.selectById(traceCode.getId());

        Map<String, Object> result = getOrBuildPayload(refreshed, normalizedCode);
        int newCount = refreshed.getScanCount() == null ? 0 : refreshed.getScanCount();
        result.put("traceCode", refreshed);
        result.put("scanRisk", newCount > 1);
        return result;
    }

    /**
     * AI 问答：不修改查验次数，仅读取与页面一致的链路数据。
     */
    public Map<String, Object> aggregateReadOnly(String code) {
        String normalizedCode = normalizeCode(code);
        TraceCode traceCode = loadValidTrace(normalizedCode);
        Map<String, Object> result = getOrBuildPayload(traceCode, normalizedCode);
        int cnt = traceCode.getScanCount() == null ? 0 : traceCode.getScanCount();
        result.put("traceCode", traceCode);
        result.put("scanRisk", cnt > 1);
        return result;
    }

    private Map<String, Object> getOrBuildPayload(TraceCode traceCode, String normalizedCode) {
        Map<String, Object> cached = payloadCache.getIfPresent(normalizedCode);
        if (cached != null) {
            return deepCopyPayload(cached);
        }
        Map<String, Object> built = buildPayload(traceCode, normalizedCode);
        payloadCache.put(normalizedCode, deepCopyPayload(built));
        return built;
    }

    /** 浅拷贝顶层 Map，避免缓存被后续写入污染 */
    private static Map<String, Object> deepCopyPayload(Map<String, Object> source) {
        return new LinkedHashMap<>(source);
    }

    private static String normalizeCode(String code) {
        return code == null ? "" : code.trim();
    }
    private TraceCode loadValidTrace(String normalizedCode) {
        TraceCode traceCode = traceCodeMapper.selectOne(
                new LambdaQueryWrapper<TraceCode>().eq(TraceCode::getCode, normalizedCode));
        if (traceCode == null) {
            throw BusinessException.of(404, "追溯码不存在");
        }
        if (traceCode.getStatus() == 6) {
            throw BusinessException.of(400, "该追溯码已作废");
        }
        return traceCode;
    }

    private Map<String, Object> buildPayload(TraceCode traceCode, String normalizedCode) {
        Map<String, Object> result = new HashMap<>();
        result.put("traceCode", traceCode);

        ProcProductBatch productBatch = null;
        if (traceCode.getProcBatchId() != null) {
            productBatch = productBatchMapper.selectById(traceCode.getProcBatchId());
        }

        ProdBatch prodBatch = null;
        if (traceCode.getProdBatchId() != null) {
            prodBatch = prodBatchMapper.selectById(traceCode.getProdBatchId());
        } else if (productBatch != null && productBatch.getRawInboundId() != null) {
            ProcRawInbound rawIn = rawInboundMapper.selectById(productBatch.getRawInboundId());
            if (rawIn != null && rawIn.getProdBatchId() != null) {
                prodBatch = prodBatchMapper.selectById(rawIn.getProdBatchId());
            }
        }

        if (prodBatch != null) {
            result.put("prodBatch", prodBatch);
            result.put("plot", plotMapper.selectById(prodBatch.getPlotId()));
            result.put("farmingList", farmingMapper.selectList(new LambdaQueryWrapper<ProdFarmingRecord>()
                    .eq(ProdFarmingRecord::getPlotId, prodBatch.getPlotId())
                    .orderByDesc(ProdFarmingRecord::getActionDate)));

            if (productBatch == null) {
                ProcRawInbound rawIn = rawInboundMapper.selectOne(new LambdaQueryWrapper<ProcRawInbound>()
                        .eq(ProcRawInbound::getProdBatchId, prodBatch.getId())
                        .orderByDesc(ProcRawInbound::getCreatedAt)
                        .last("LIMIT 1"));
                if (rawIn != null) {
                    productBatch = productBatchMapper.selectOne(new LambdaQueryWrapper<ProcProductBatch>()
                            .eq(ProcProductBatch::getRawInboundId, rawIn.getId())
                            .orderByDesc(ProcProductBatch::getCreatedAt)
                            .last("LIMIT 1"));
                }
            }
        }

        if (productBatch != null) {
            result.put("productBatch", productBatch);

            SysOrg productionOrg = orgMapper.selectById(productBatch.getOrgId());
            if (productionOrg != null) {
                result.put("productionOrg", productionOrg);
            }

            List<ProcQualityInspection> qualityList = qualityMapper.selectList(
                    new LambdaQueryWrapper<ProcQualityInspection>()
                            .eq(ProcQualityInspection::getProcBatchId, productBatch.getId()));
            result.put("qualityList", qualityList);

            List<ProcInventoryRecord> inventoryList = inventoryMapper.selectList(
                    new LambdaQueryWrapper<ProcInventoryRecord>()
                            .eq(ProcInventoryRecord::getProductBatchId, productBatch.getId())
                            .orderByDesc(ProcInventoryRecord::getOperateDate)
                            .orderByDesc(ProcInventoryRecord::getCreatedAt));
            result.put("inventorySnapshot", buildInventorySnapshot(productBatch, inventoryList));
            result.put("inventoryList", inventoryList);
        }

        List<DistLogisticsRecord> logisticsList = logisticsMapper.selectList(
                new LambdaQueryWrapper<DistLogisticsRecord>()
                        .eq(DistLogisticsRecord::getTraceCode, normalizedCode));
        result.put("logisticsList", buildLogisticsListWithNodes(logisticsList));

        List<DistSaleRecord> saleList = saleMapper.selectList(
                new LambdaQueryWrapper<DistSaleRecord>()
                        .eq(DistSaleRecord::getTraceCode, normalizedCode));
        result.put("saleList", saleList);

        List<DistInoutRecord> distInoutList = distInoutMapper.selectList(
                new LambdaQueryWrapper<DistInoutRecord>()
                        .eq(DistInoutRecord::getTraceCode, normalizedCode)
                        .orderByDesc(DistInoutRecord::getOperateDate)
                        .orderByDesc(DistInoutRecord::getCreatedAt));
        result.put("distInoutList", distInoutList);

        Map<String, Object> consumerFacing = buildConsumerFacing(traceCode, productBatch, prodBatch);
        result.put("consumerFacing", consumerFacing);

        LinkedHashMap<String, Object> ordered = new LinkedHashMap<>();
        ordered.put("traceCode", result.get("traceCode"));
        ordered.put("consumerFacing", consumerFacing);
        for (Map.Entry<String, Object> e : result.entrySet()) {
            String k = e.getKey();
            if (!"traceCode".equals(k) && !"consumerFacing".equals(k)) {
                ordered.put(k, e.getValue());
            }
        }
        return ordered;
    }

    private static Map<String, Object> buildInventorySnapshot(ProcProductBatch productBatch, List<ProcInventoryRecord> inventoryRecords) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("batchNo", blankToEmpty(productBatch.getBatchNo()));
        m.put("productName", blankToEmpty(productBatch.getProductName()));
        m.put("productSpec", blankToEmpty(productBatch.getProductSpec()));
        m.put("ingredients", blankToEmpty(productBatch.getIngredients()));
        m.put("productOrigin", blankToEmpty(productBatch.getProductOrigin()));
        m.put("shelfLife", blankToEmpty(productBatch.getShelfLife()));
        m.put("storageCondition", blankToEmpty(productBatch.getStorageCondition()));
        m.put("quantity", ledgerBasedQuantity(productBatch, inventoryRecords));
        m.put("unit", blankToEmpty(productBatch.getUnit()));
        m.put("finishDate", productBatch.getFinishDate());
        m.put("qualityStatus", productBatch.getQualityStatus());
        m.put("status", productBatch.getStatus());
        return m;
    }

    /**
     * 有库存流水时按时间序重算结存（1入库 +、2出库 -、3盘点覆盖）；无流水则退回批次表 quantity。
     */
    private static BigDecimal ledgerBasedQuantity(ProcProductBatch batch, List<ProcInventoryRecord> records) {
        if (records == null || records.isEmpty()) {
            return batch.getQuantity() == null ? BigDecimal.ZERO : batch.getQuantity();
        }
        List<ProcInventoryRecord> asc = new ArrayList<>(records);
        asc.sort(Comparator
                .comparing(ProcInventoryRecord::getOperateDate, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(ProcInventoryRecord::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())));
        BigDecimal running = BigDecimal.ZERO;
        boolean applied = false;
        for (ProcInventoryRecord r : asc) {
            if (r.getOperateType() == null || r.getQuantity() == null) {
                continue;
            }
            applied = true;
            if (r.getOperateType() == 1) {
                running = running.add(r.getQuantity());
            } else if (r.getOperateType() == 2) {
                running = running.subtract(r.getQuantity());
            } else if (r.getOperateType() == 3) {
                running = r.getQuantity();
            }
        }
        if (!applied) {
            return batch.getQuantity() == null ? BigDecimal.ZERO : batch.getQuantity();
        }
        return running;
    }

    private static Map<String, Object> buildConsumerFacing(
            TraceCode traceCode, ProcProductBatch productBatch, ProdBatch prodBatch) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("traceCode", traceCode.getCode());
        String display = "";
        if (notBlank(productBatch != null ? productBatch.getProductName() : null)) {
            display = productBatch.getProductName().trim();
        } else if (notBlank(prodBatch != null ? prodBatch.getProductName() : null)) {
            display = prodBatch.getProductName().trim();
        }
        m.put("displayProductName", display);
        if (prodBatch != null) {
            m.put("harvestStageRecordName", blankToEmpty(prodBatch.getProductName()));
            m.put("harvestBatchNo", blankToEmpty(prodBatch.getBatchNo()));
        } else {
            m.put("harvestStageRecordName", "");
            m.put("harvestBatchNo", "");
        }
        if (productBatch != null) {
            m.put("finishedProductBatchNo", blankToEmpty(productBatch.getBatchNo()));
            m.put("productSpec", blankToEmpty(productBatch.getProductSpec()));
            m.put("ingredients", blankToEmpty(productBatch.getIngredients()));
            m.put("productOrigin", blankToEmpty(productBatch.getProductOrigin()));
            m.put("shelfLife", blankToEmpty(productBatch.getShelfLife()));
            m.put("storageCondition", blankToEmpty(productBatch.getStorageCondition()));
        } else {
            m.put("finishedProductBatchNo", "");
            m.put("productSpec", "");
            m.put("ingredients", "");
            m.put("productOrigin", "");
            m.put("shelfLife", "");
            m.put("storageCondition", "");
        }
        m.put(
                "namingHint",
                "用户问「产品名称」时只答复 displayProductName；harvestStageRecordName 仅为采收/原料环节在系统内的记录用名，"
                        + "不得与成品名称并列称为两个商品；除非用户明确问采收侧名称。");
        return m;
    }

    /**
     * 公开溯源页物流：在运单基础上附带节点轨迹（与流通端录入一致）。
     */
    private List<Map<String, Object>> buildLogisticsListWithNodes(List<DistLogisticsRecord> records) {
        if (records == null || records.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> logisticsIds = records.stream()
                .map(DistLogisticsRecord::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        Map<Long, List<DistLogisticsNode>> nodesByLogisticsId = new HashMap<>();
        if (!logisticsIds.isEmpty()) {
            List<DistLogisticsNode> allNodes = logisticsNodeMapper.selectList(
                    new LambdaQueryWrapper<DistLogisticsNode>()
                            .in(DistLogisticsNode::getLogisticsId, logisticsIds)
                            .orderByAsc(DistLogisticsNode::getArriveTime)
                            .orderByAsc(DistLogisticsNode::getId));
            for (DistLogisticsNode node : allNodes) {
                nodesByLogisticsId
                        .computeIfAbsent(node.getLogisticsId(), k -> new ArrayList<>())
                        .add(node);
            }
        }

        List<Map<String, Object>> out = new ArrayList<>();
        for (DistLogisticsRecord r : records) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", r.getId());
            m.put("orgId", r.getOrgId());
            m.put("traceCode", r.getTraceCode());
            m.put("carrier", r.getCarrier());
            m.put("waybillNo", r.getWaybillNo());
            m.put("origin", r.getOrigin());
            m.put("destination", r.getDestination());
            m.put("status", r.getStatus());
            m.put("createdAt", r.getCreatedAt());
            m.put("updatedAt", r.getUpdatedAt());
            m.put("nodes", nodesByLogisticsId.getOrDefault(r.getId(), Collections.emptyList()));
            out.add(m);
        }
        return out;
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    private static String blankToEmpty(String s) {
        return s == null || s.isBlank() ? "" : s.trim();
    }
}
