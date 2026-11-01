package com.tracegrid.dist.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tracegrid.common.exception.BusinessException;
import com.tracegrid.common.result.R;
import com.tracegrid.dist.entity.DistInoutRecord;
import com.tracegrid.dist.entity.DistLogisticsNode;
import com.tracegrid.dist.entity.DistLogisticsRecord;
import com.tracegrid.dist.entity.DistSaleRecord;
import com.tracegrid.dist.mapper.DistInoutRecordMapper;
import com.tracegrid.dist.mapper.DistLogisticsNodeMapper;
import com.tracegrid.dist.mapper.DistLogisticsRecordMapper;
import com.tracegrid.dist.mapper.DistSaleRecordMapper;
import com.tracegrid.proc.entity.ProcProductBatch;
import com.tracegrid.proc.mapper.ProcProductBatchMapper;
import com.tracegrid.integration.service.ProcDistStockLinkService;
import com.tracegrid.sys.mapper.SysUserMapper;
import com.tracegrid.trace.entity.TraceCode;
import com.tracegrid.trace.mapper.TraceCodeMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 流通管理接口
 */
@Tag(name = "流通管理")
@RestController
@RequestMapping("/dist")
@RequiredArgsConstructor
public class DistController {

    private final DistInoutRecordMapper inoutMapper;
    private final DistLogisticsRecordMapper logisticsMapper;
    private final DistLogisticsNodeMapper nodeMapper;
    private final DistSaleRecordMapper saleMapper;
    private final SysUserMapper userMapper;
    private final TraceCodeMapper traceCodeMapper;
    private final ProcProductBatchMapper procProductBatchMapper;
    private final ProcDistStockLinkService procDistStockLinkService;

    // ============ 出入库 ============

    @Operation(summary = "出入库记录列表")
    @GetMapping("/inout-record/list")
    public R<?> inoutList(@RequestParam(defaultValue = "1") Integer page,
                          @RequestParam(defaultValue = "10") Integer size,
                          @RequestParam(required = false) Integer inoutType,
                          @RequestParam(required = false) String traceCode) {
        Long orgId = getCurrentOrgId();
        Page<DistInoutRecord> p = new Page<>(page, size);
        LambdaQueryWrapper<DistInoutRecord> wrapper = new LambdaQueryWrapper<DistInoutRecord>()
                .eq(orgId != null, DistInoutRecord::getOrgId, orgId)
                .eq(inoutType != null, DistInoutRecord::getInoutType, inoutType)
                .like(traceCode != null, DistInoutRecord::getTraceCode, traceCode)
                .orderByDesc(DistInoutRecord::getCreatedAt);
        return R.ok(inoutMapper.selectPage(p, wrapper));
    }

    @Operation(summary = "按追溯码查询流通入库/出库/销售累计与可动销余量（入库−出库−销售）")
    @GetMapping("/inout-record/balance")
    public R<Map<String, Object>> inoutBalance(@RequestParam String traceCode) {
        String code = normalizeTraceCode(traceCode);
        if (code == null || code.isEmpty()) {
            throw BusinessException.of("请填写追溯码");
        }
        Long orgId = getCurrentOrgId();
        BigDecimal inTotal = sumInoutQuantity(orgId, code, 1);
        BigDecimal outTotal = sumInoutQuantity(orgId, code, 2);
        BigDecimal saleTotal = sumSaleQuantity(orgId, code);
        BigDecimal distRemaining = distributionRemaining(inTotal, outTotal, saleTotal);
        BigDecimal procCap = procStockCapSameOrg(orgId, code);
        BigDecimal available = applyProcStockCap(distRemaining, procCap);
        Map<String, Object> m = new HashMap<>();
        m.put("inTotal", inTotal);
        m.put("outTotal", outTotal);
        m.put("saleTotal", saleTotal);
        m.put("distChannelRemaining", distRemaining);
        m.put("procBatchStockCap", procCap);
        m.put("procBatchLinked", procCap != null);
        m.put("available", available);
        return R.ok(m);
    }

    @Operation(summary = "可选加工出库流水（按追溯码，用于入库核销下拉）")
    @GetMapping("/inout-record/proc-source-options")
    public R<?> procSourceOptions(@RequestParam String traceCode) {
        return R.ok(procDistStockLinkService.listUnsettledProcOutboundsForTrace(traceCode));
    }

    @Operation(summary = "新增出入库记录")
    @PostMapping("/inout-record")
    @Transactional(rollbackFor = Exception.class)
    public R<Void> addInout(@RequestBody DistInoutRecord req) {
        req.setTraceCode(normalizeTraceCode(req.getTraceCode()));
        Long orgId = getCurrentOrgId();
        req.setOrgId(orgId);
        procDistStockLinkService.validateInboundBeforeInsert(req, orgId);
        if (req.getInoutType() != null && req.getInoutType() == 2) {
            assertOutboundWithinRemaining(orgId, req.getTraceCode(), req.getQuantity());
        }
        fillProcBatchIdFromTrace(req);
        req.setCreatedBy(getCurrentUserId());
        req.setCreatedAt(LocalDateTime.now());
        inoutMapper.insert(req);
        if (req.getInoutType() != null && req.getInoutType() == 1) {
            procDistStockLinkService.settleInboundAfterInsert(req, orgId);
        }
        return R.success("记录已创建");
    }

    // ============ 物流 ============

    @Operation(summary = "物流记录列表")
    @GetMapping("/logistics/list")
    public R<?> logisticsList(@RequestParam(defaultValue = "1") Integer page,
                              @RequestParam(defaultValue = "10") Integer size) {
        Long orgId = getCurrentOrgId();
        Page<DistLogisticsRecord> p = new Page<>(page, size);
        return R.ok(logisticsMapper.selectPage(p,
                new LambdaQueryWrapper<DistLogisticsRecord>()
                        .eq(orgId != null, DistLogisticsRecord::getOrgId, orgId)
                        .orderByDesc(DistLogisticsRecord::getCreatedAt)));
    }

    @Operation(summary = "新增物流记录")
    @PostMapping("/logistics")
    public R<DistLogisticsRecord> addLogistics(@RequestBody DistLogisticsRecord req) {
        req.setTraceCode(normalizeTraceCode(req.getTraceCode()));
        req.setOrgId(getCurrentOrgId());
        req.setCreatedBy(getCurrentUserId());
        req.setCreatedAt(LocalDateTime.now());
        logisticsMapper.insert(req);
        return R.ok(req);
    }

    @Operation(summary = "追加物流节点")
    @PostMapping("/logistics/{id}/node")
    public R<Void> addNode(@PathVariable Long id, @RequestBody DistLogisticsNode node) {
        node.setLogisticsId(id);
        node.setCreatedBy(getCurrentUserId());
        node.setCreatedAt(LocalDateTime.now());
        nodeMapper.insert(node);
        return R.success("节点已追加");
    }

    @Operation(summary = "查询物流节点")
    @GetMapping("/logistics/{id}/nodes")
    public R<?> getNodes(@PathVariable Long id) {
        return R.ok(nodeMapper.selectList(new LambdaQueryWrapper<DistLogisticsNode>()
                .eq(DistLogisticsNode::getLogisticsId, id)
                .orderByAsc(DistLogisticsNode::getArriveTime)));
    }

    // ============ 终端销售 ============

    @Operation(summary = "销售记录列表")
    @GetMapping("/sale-record/list")
    public R<?> saleList(@RequestParam(defaultValue = "1") Integer page,
                         @RequestParam(defaultValue = "10") Integer size) {
        Long orgId = getCurrentOrgId();
        Page<DistSaleRecord> p = new Page<>(page, size);
        return R.ok(saleMapper.selectPage(p,
                new LambdaQueryWrapper<DistSaleRecord>()
                        .eq(orgId != null, DistSaleRecord::getOrgId, orgId)
                        .orderByDesc(DistSaleRecord::getCreatedAt)));
    }

    @Operation(summary = "新增销售记录")
    @PostMapping("/sale-record")
    public R<Void> addSale(@RequestBody DistSaleRecord req) {
        req.setTraceCode(normalizeTraceCode(req.getTraceCode()));
        Long orgId = getCurrentOrgId();
        req.setOrgId(orgId);
        assertSaleWithinRemaining(orgId, req.getTraceCode(), req.getQuantity());
        req.setCreatedBy(getCurrentUserId());
        req.setCreatedAt(LocalDateTime.now());
        saleMapper.insert(req);
        return R.success("销售记录已创建");
    }

    private Long getCurrentUserId() {
        return Long.parseLong(StpUtil.getLoginIdAsString());
    }

    private String normalizeTraceCode(String traceCode) {
        return traceCode == null ? null : traceCode.trim();
    }

    private Long getCurrentOrgId() {
        Long userId = getCurrentUserId();
        com.tracegrid.sys.entity.SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw BusinessException.of("用户不存在");
        }
        // 管理员角色（role=1）不需要关联主体
        if (user.getRole() != null && user.getRole() == 1) {
            return null;
        }
        if (user.getOrgId() == null) {
            throw BusinessException.of("该账号未关联所属主体，请联系管理员");
        }
        return user.getOrgId();
    }

    /**
     * 同一追溯码下：入库 − 出库 − 终端销售累计 ≥ 0；出库、销售均不得突破该余量。
     */
    private void assertOutboundWithinRemaining(Long orgId, String normalizedTraceCode, BigDecimal outboundQty) {
        if (normalizedTraceCode == null || normalizedTraceCode.isEmpty()) {
            throw BusinessException.of("出库须填写追溯码");
        }
        if (outboundQty == null || outboundQty.compareTo(BigDecimal.ZERO) <= 0) {
            throw BusinessException.of("出库数量须大于 0");
        }
        BigDecimal inTotal = sumInoutQuantity(orgId, normalizedTraceCode, 1);
        BigDecimal outTotal = sumInoutQuantity(orgId, normalizedTraceCode, 2);
        BigDecimal saleTotal = sumSaleQuantity(orgId, normalizedTraceCode);
        BigDecimal distRem = distributionRemaining(inTotal, outTotal, saleTotal);
        BigDecimal available = applyProcStockCap(distRem, procStockCapSameOrg(orgId, normalizedTraceCode));
        if (available.compareTo(outboundQty) < 0) {
            BigDecimal procCap = procStockCapSameOrg(orgId, normalizedTraceCode);
            String link = procCap == null ? "" : "（已与同主体加工成品批次结存取较小值）";
            throw BusinessException.of(String.format(
                    "出库数量不能超过上限%s。流通口径余量 %s；加工结存上限 %s；综合最多可出库 %s。当前入库合计 %s，已出库 %s，已登记销售 %s。",
                    link,
                    stripNumber(distRem),
                    procCap == null ? "—" : stripNumber(procCap),
                    stripNumber(available),
                    stripNumber(inTotal), stripNumber(outTotal), stripNumber(saleTotal)));
        }
    }

    private void assertSaleWithinRemaining(Long orgId, String normalizedTraceCode, BigDecimal saleQty) {
        if (normalizedTraceCode == null || normalizedTraceCode.isEmpty()) {
            throw BusinessException.of("销售须填写追溯码");
        }
        if (saleQty == null || saleQty.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        BigDecimal inTotal = sumInoutQuantity(orgId, normalizedTraceCode, 1);
        BigDecimal outTotal = sumInoutQuantity(orgId, normalizedTraceCode, 2);
        BigDecimal saleTotal = sumSaleQuantity(orgId, normalizedTraceCode);
        BigDecimal distRem = distributionRemaining(inTotal, outTotal, saleTotal);
        BigDecimal available = applyProcStockCap(distRem, procStockCapSameOrg(orgId, normalizedTraceCode));
        if (available.compareTo(saleQty) < 0) {
            BigDecimal procCap = procStockCapSameOrg(orgId, normalizedTraceCode);
            String link = procCap == null ? "" : "（已与同主体加工成品批次结存取较小值）";
            throw BusinessException.of(String.format(
                    "销售数量不能超过上限%s。流通口径余量 %s；加工结存上限 %s；综合最多可售 %s。当前入库合计 %s，已出库 %s，已登记销售 %s。",
                    link,
                    stripNumber(distRem),
                    procCap == null ? "—" : stripNumber(procCap),
                    stripNumber(available),
                    stripNumber(inTotal), stripNumber(outTotal), stripNumber(saleTotal)));
        }
    }

    private static BigDecimal distributionRemaining(BigDecimal inTotal, BigDecimal outTotal, BigDecimal saleTotal) {
        return inTotal.subtract(outTotal).subtract(saleTotal);
    }

    /**
     * 追溯码绑定加工成品批次、且流通主体与加工主体为同一 org 时，出库/销售不得超过成品批次当前结存（quantity，与加工库存流水同步）。
     */
    private BigDecimal procStockCapSameOrg(Long orgId, String normalizedTraceCode) {
        if (normalizedTraceCode == null || normalizedTraceCode.isEmpty() || orgId == null) {
            return null;
        }
        TraceCode tc = traceCodeMapper.selectOne(
                new LambdaQueryWrapper<TraceCode>().apply("TRIM(code) = {0}", normalizedTraceCode));
        if (tc == null || tc.getProcBatchId() == null) {
            return null;
        }
        ProcProductBatch pb = procProductBatchMapper.selectById(tc.getProcBatchId());
        if (pb == null || pb.getOrgId() == null || !pb.getOrgId().equals(orgId)) {
            return null;
        }
        return pb.getQuantity() == null ? BigDecimal.ZERO : pb.getQuantity();
    }

    private static BigDecimal applyProcStockCap(BigDecimal distRemaining, BigDecimal procCap) {
        if (procCap == null) {
            return distRemaining;
        }
        return distRemaining.min(procCap);
    }

    private void fillProcBatchIdFromTrace(DistInoutRecord req) {
        if (req.getProcBatchId() != null || req.getTraceCode() == null || req.getTraceCode().isEmpty()) {
            return;
        }
        String c = req.getTraceCode().trim();
        TraceCode tc = traceCodeMapper.selectOne(
                new LambdaQueryWrapper<TraceCode>().apply("TRIM(code) = {0}", c));
        if (tc != null && tc.getProcBatchId() != null) {
            req.setProcBatchId(tc.getProcBatchId());
        }
    }

    private BigDecimal sumSaleQuantity(Long orgId, String normalizedTraceCode) {
        LambdaQueryWrapper<DistSaleRecord> w = new LambdaQueryWrapper<DistSaleRecord>()
                .apply("TRIM(trace_code) = {0}", normalizedTraceCode);
        if (orgId != null) {
            w.eq(DistSaleRecord::getOrgId, orgId);
        }
        List<DistSaleRecord> rows = saleMapper.selectList(w);
        BigDecimal sum = BigDecimal.ZERO;
        for (DistSaleRecord r : rows) {
            if (r.getQuantity() != null) {
                sum = sum.add(r.getQuantity());
            }
        }
        return sum;
    }

    private BigDecimal sumInoutQuantity(Long orgId, String normalizedTraceCode, int inoutType) {
        LambdaQueryWrapper<DistInoutRecord> w = new LambdaQueryWrapper<DistInoutRecord>()
                .apply("TRIM(trace_code) = {0}", normalizedTraceCode)
                .eq(DistInoutRecord::getInoutType, inoutType);
        if (orgId != null) {
            w.eq(DistInoutRecord::getOrgId, orgId);
        }
        List<DistInoutRecord> rows = inoutMapper.selectList(w);
        BigDecimal sum = BigDecimal.ZERO;
        for (DistInoutRecord r : rows) {
            if (r.getQuantity() != null) {
                sum = sum.add(r.getQuantity());
            }
        }
        return sum;
    }

    private static String stripNumber(BigDecimal n) {
        return n == null ? "0" : n.stripTrailingZeros().toPlainString();
    }
}
