package com.tracegrid.proc.controller;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tracegrid.common.exception.BusinessException;
import com.tracegrid.common.result.R;
import com.tracegrid.common.security.SecurityUtils;
import com.tracegrid.proc.entity.ProcInventoryRecord;
import com.tracegrid.proc.entity.ProcProcessRecord;
import com.tracegrid.proc.entity.ProcProductBatch;
import com.tracegrid.proc.entity.ProcQualityInspection;
import com.tracegrid.proc.entity.ProcRawInbound;
import com.tracegrid.proc.mapper.ProcInventoryRecordMapper;
import com.tracegrid.proc.mapper.ProcProcessRecordMapper;
import com.tracegrid.proc.mapper.ProcProductBatchMapper;
import com.tracegrid.proc.mapper.ProcQualityInspectionMapper;
import com.tracegrid.proc.mapper.ProcRawInboundMapper;
import com.tracegrid.integration.service.ProcDistStockLinkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 加工模块接口
 */
@Tag(name = "加工管理")
@RestController
@RequestMapping("/proc")
@RequiredArgsConstructor
public class ProcController {

    private final ProcRawInboundMapper rawInboundMapper;
    private final ProcProductBatchMapper productBatchMapper;
    private final ProcInventoryRecordMapper inventoryRecordMapper;
    private final ProcQualityInspectionMapper qualityMapper;
    private final ProcProcessRecordMapper processRecordMapper;
    private final com.tracegrid.sys.mapper.SysUserMapper userMapper;
    private final com.tracegrid.sys.service.SysMessageService messageService;
    private final ProcDistStockLinkService procDistStockLinkService;

    // ============ 原料入库 ============

    @Operation(summary = "原料入库列表")
    @GetMapping("/raw-inbound/list")
    public R<?> rawInboundList(@RequestParam(defaultValue = "1") Integer page,
                               @RequestParam(defaultValue = "10") Integer size,
                               @RequestParam(required = false) Integer status) {
        Long orgId = getCurrentOrgId();
        Page<ProcRawInbound> p = new Page<>(page, size);
        return R.ok(rawInboundMapper.selectPage(p,
                new LambdaQueryWrapper<ProcRawInbound>()
                        .eq(orgId != null, ProcRawInbound::getOrgId, orgId)
                        .eq(status != null, ProcRawInbound::getStatus, status)
                        .orderByDesc(ProcRawInbound::getCreatedAt)));
    }

    @Operation(summary = "新增原料入库")
    @PostMapping("/raw-inbound")
    public R<Void> addRawInbound(@RequestBody ProcRawInbound req) {
        req.setOrgId(getCurrentOrgId());
        req.setCreatedBy(getCurrentUserId());
        req.setCreatedAt(LocalDateTime.now());
        req.setStatus(0);
        rawInboundMapper.insert(req);
        return R.success("入库记录已创建");
    }

    @Operation(summary = "提交原料入库")
    @PostMapping("/raw-inbound/{id}/submit")
    public R<Void> submitRawInbound(@PathVariable Long id) {
        ProcRawInbound r = rawInboundMapper.selectById(id);
        if (r == null) throw BusinessException.of("记录不存在");
        if (r.getStatus() == null || r.getStatus() != 0) {
            throw BusinessException.of("仅草稿状态可提交审核");
        }
        r.setStatus(1);
        rawInboundMapper.updateById(r);
        // 通知管理员
        messageService.send(1L, "新的原料入库待审核", 
            "有新的原料入库申请已提交，请及时处理。", 1);
        return R.success("提交成功");
    }

    @Operation(summary = "原料入库审核列表")
    @GetMapping("/raw-inbound/audit-list")
    public R<?> rawInboundAuditList(@RequestParam(defaultValue = "1") Integer page,
                                   @RequestParam(defaultValue = "10") Integer size,
                                   @RequestParam(required = false) Integer status) {
        SecurityUtils.requireAdmin(userMapper);
        Page<ProcRawInbound> p = new Page<>(page, size);
        LambdaQueryWrapper<ProcRawInbound> wrapper = new LambdaQueryWrapper<ProcRawInbound>()
                .eq(status != null, ProcRawInbound::getStatus, status)
                .orderByDesc(ProcRawInbound::getCreatedAt);
        rawInboundMapper.selectPage(p, wrapper);
        enrichRawInboundAuditors(p.getRecords());
        return R.ok(p);
    }

    @Operation(summary = "审核原料入库记录")
    @PostMapping("/raw-inbound/{id}/audit")
    public R<Void> auditRawInbound(@PathVariable Long id, 
                                  @RequestParam Integer status,
                                  @RequestParam(required = false) String reason) {
        SecurityUtils.requireAdmin(userMapper);
        ProcRawInbound r = rawInboundMapper.selectById(id);
        if (r == null) throw BusinessException.of("记录不存在");
        if (r.getStatus() == null || r.getStatus() != 1) {
            throw BusinessException.of("仅「待审核」状态的入库单可审核");
        }
        if (status != 2 && status != 3) {
            throw BusinessException.of("审核结果参数无效");
        }
        if (status == 3 && (reason == null || reason.isBlank())) {
            throw BusinessException.of("驳回操作必须填写具体原因");
        }

        Long auditorId = getCurrentUserId();
        LocalDateTime now = LocalDateTime.now();
        r.setStatus(status);
        if (status == 2) {
            r.setRejectReason(null);
        } else {
            r.setRejectReason(reason);
        }
        r.setAuditBy(auditorId);
        r.setAuditAt(now);
        r.setUpdatedAt(now);
        r.setUpdatedBy(auditorId);
        rawInboundMapper.updateById(r);
        String content = status == 2 ? "您的原料入库申请已通过。" : "您的原料入库申请被驳回，原因：" + reason;
        messageService.send(r.getCreatedBy(), "原料入库审核结果", content, 1);

        return R.success("审核操作已完成");
    }

    // ============ 成品批次 ============

    @Operation(summary = "成品批次列表")
    @GetMapping("/product-batch/list")
    public R<?> productBatchList(@RequestParam(defaultValue = "1") Integer page,
                                 @RequestParam(defaultValue = "10") Integer size,
                                 @RequestParam(required = false) Integer qualityStatus) {
        Long orgId = getCurrentOrgId();
        Page<ProcProductBatch> p = new Page<>(page, size);
        LambdaQueryWrapper<ProcProductBatch> wrapper = new LambdaQueryWrapper<ProcProductBatch>()
                .eq(orgId != null, ProcProductBatch::getOrgId, orgId)
                .eq(qualityStatus != null, ProcProductBatch::getQualityStatus, qualityStatus)
                .orderByDesc(ProcProductBatch::getCreatedAt);
        return R.ok(productBatchMapper.selectPage(p, wrapper));
    }

    @Operation(summary = "新增成品批次")
    @PostMapping("/product-batch")
    @Transactional(rollbackFor = Exception.class)
    public R<Void> addProductBatch(@RequestBody ProcProductBatch batch) {
        batch.setOrgId(getCurrentOrgId());
        batch.setCreatedBy(getCurrentUserId());
        batch.setCreatedAt(LocalDateTime.now());
        if (batch.getBatchNo() == null || batch.getBatchNo().isBlank()) {
            batch.setBatchNo("CB-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                    + "-" + IdUtil.getSnowflakeNextIdStr().substring(10));
        }
        batch.setQualityStatus(0);
        batch.setStatus(0);
        validateProductBatchAgainstRawInbound(batch);
        productBatchMapper.insert(batch);
        // 与库存流水联动：有数量时自动记一笔「成品入库」流水，溯源页库存快照与流水一致
        if (batch.getQuantity() != null && batch.getQuantity().compareTo(BigDecimal.ZERO) > 0) {
            ProcInventoryRecord inv = new ProcInventoryRecord();
            inv.setOrgId(batch.getOrgId());
            inv.setProductBatchId(batch.getId());
            inv.setOperateType(1);
            inv.setQuantity(batch.getQuantity());
            inv.setOperateDate(batch.getFinishDate() != null ? batch.getFinishDate() : LocalDate.now());
            inv.setRemark("成品批次创建—初始入库");
            inv.setDistLinkPending(0);
            inv.setLinkedDistInoutId(null);
            inv.setCreatedBy(batch.getCreatedBy());
            inv.setCreatedAt(LocalDateTime.now());
            inventoryRecordMapper.insert(inv);
        }
        return R.success("成品批次创建成功");
    }

    @Operation(summary = "成品批次详情")
    @GetMapping("/product-batch/{id}")
    public R<ProcProductBatch> productBatchDetail(@PathVariable Long id) {
        return R.ok(productBatchMapper.selectById(id));
    }

    // ============ 成品库存流水（与 proc_product_batch.quantity 联动） ============

    @Operation(summary = "成品库存流水列表")
    @GetMapping("/inventory-record/list")
    public R<?> inventoryRecordList(@RequestParam Long productBatchId,
                                    @RequestParam(defaultValue = "1") Integer page,
                                    @RequestParam(defaultValue = "20") Integer size) {
        Long orgId = getCurrentOrgId();
        ProcProductBatch batch = productBatchMapper.selectById(productBatchId);
        if (batch == null) {
            throw BusinessException.of("成品批次不存在");
        }
        if (orgId != null && !orgId.equals(batch.getOrgId())) {
            throw BusinessException.of("无权查看该批次库存");
        }
        Page<ProcInventoryRecord> p = new Page<>(page, size);
        return R.ok(inventoryRecordMapper.selectPage(p,
                new LambdaQueryWrapper<ProcInventoryRecord>()
                        .eq(ProcInventoryRecord::getProductBatchId, productBatchId)
                        .orderByDesc(ProcInventoryRecord::getOperateDate)
                        .orderByDesc(ProcInventoryRecord::getCreatedAt)));
    }

    @Operation(summary = "新增成品库存流水（入库累加、出库扣减、盘点覆盖结存，并同步批次 quantity）")
    @PostMapping("/inventory-record")
    @Transactional(rollbackFor = Exception.class)
    public R<Void> addInventoryRecord(@RequestBody ProcInventoryRecord req) {
        if (req.getProductBatchId() == null) {
            throw BusinessException.of("请指定成品批次");
        }
        Long orgId = getCurrentOrgId();
        ProcProductBatch batch = productBatchMapper.selectById(req.getProductBatchId());
        if (batch == null) {
            throw BusinessException.of("成品批次不存在");
        }
        if (orgId != null && !orgId.equals(batch.getOrgId())) {
            throw BusinessException.of("无权操作该批次库存");
        }
        if (req.getOperateType() == null) {
            throw BusinessException.of("请指定操作类型：1入库 2出库 3盘点");
        }
        BigDecimal q = req.getQuantity() == null ? BigDecimal.ZERO : req.getQuantity();
        if (req.getOperateType() != 3 && q.compareTo(BigDecimal.ZERO) <= 0) {
            throw BusinessException.of("入库/出库数量须大于 0");
        }
        if (req.getOperateType() == 3 && q.compareTo(BigDecimal.ZERO) < 0) {
            throw BusinessException.of("盘点结存不能为负数");
        }

        BigDecimal onHand = nz(batch.getQuantity());
        if (req.getOperateType() == 2) {
            if (onHand.compareTo(q) < 0) {
                throw BusinessException.of("出库数量不能超过当前成品批次库存（当前 " + stripNum(onHand) + "）");
            }
            batch.setQuantity(onHand.subtract(q));
        } else if (req.getOperateType() == 1) {
            batch.setQuantity(onHand.add(q));
        } else if (req.getOperateType() == 3) {
            batch.setQuantity(q);
        } else {
            throw BusinessException.of("不支持的操作类型");
        }
        productBatchMapper.updateById(batch);

        req.setOrgId(batch.getOrgId());
        if (req.getOperateDate() == null) {
            req.setOperateDate(LocalDate.now());
        }
        procDistStockLinkService.applyOutboundPendingFlag(req, batch.getOrgId());
        req.setCreatedBy(getCurrentUserId());
        req.setCreatedAt(LocalDateTime.now());
        inventoryRecordMapper.insert(req);
        return R.success("库存流水已登记");
    }

    @Operation(summary = "待流通核销的加工出库流水（未绑定流通入库）")
    @GetMapping("/inventory-record/pending-dist-links")
    public R<?> pendingDistInventoryLinks(@RequestParam(defaultValue = "1") Integer page,
                                          @RequestParam(defaultValue = "20") Integer size) {
        Long orgId = getCurrentOrgId();
        Page<ProcInventoryRecord> p = new Page<>(page, size);
        LambdaQueryWrapper<ProcInventoryRecord> w = new LambdaQueryWrapper<ProcInventoryRecord>()
                .eq(ProcInventoryRecord::getOperateType, 2)
                .isNull(ProcInventoryRecord::getLinkedDistInoutId)
                .orderByDesc(ProcInventoryRecord::getDistLinkPending)
                .orderByDesc(ProcInventoryRecord::getCreatedAt);
        if (orgId != null) {
            w.eq(ProcInventoryRecord::getOrgId, orgId);
        }
        return R.ok(inventoryRecordMapper.selectPage(p, w));
    }

    // ============ 质检记录 ============

    @Operation(summary = "质检记录列表")
    @GetMapping("/quality-inspection/list")
    public R<?> qualityList(@RequestParam(defaultValue = "1") Integer page,
                            @RequestParam(defaultValue = "10") Integer size) {
        Long orgId = getCurrentOrgId();
        Page<ProcQualityInspection> p = new Page<>(page, size);
        return R.ok(qualityMapper.selectPage(p,
                new LambdaQueryWrapper<ProcQualityInspection>()
                        .eq(orgId != null, ProcQualityInspection::getOrgId, orgId)
                        .orderByDesc(ProcQualityInspection::getCreatedAt)));
    }

    @Operation(summary = "新增质检记录")
    @PostMapping("/quality-inspection")
    public R<Void> addQuality(@RequestBody ProcQualityInspection req) {
        normalizeQualityMedia(req);
        req.setOrgId(getCurrentOrgId());
        req.setCreatedBy(getCurrentUserId());
        req.setCreatedAt(LocalDateTime.now());
        req.setStatus(0);
        qualityMapper.insert(req);

        // 如果质检结果确定，同步更新成品批次质检状态
        if (req.getResult() != null && req.getProcBatchId() != null) {
            ProcProductBatch batch = productBatchMapper.selectById(req.getProcBatchId());
            if (batch != null) {
                batch.setQualityStatus(req.getResult()); // 1合格 2不合格
                productBatchMapper.updateById(batch);
            }
        }
        return R.success("质检记录已提交");
    }

    @Operation(summary = "更新质检记录")
    @PutMapping("/quality-inspection/{id}")
    public R<Void> updateQuality(@PathVariable Long id, @RequestBody ProcQualityInspection req) {
        normalizeQualityMedia(req);
        req.setId(id);
        req.setUpdatedBy(getCurrentUserId());
        req.setUpdatedAt(LocalDateTime.now());
        qualityMapper.updateById(req);
        return R.success("更新成功");
    }

    // ============ 加工记录 ============

    @Operation(summary = "加工记录列表")
    @GetMapping("/record/list")
    public R<?> recordList(@RequestParam(defaultValue = "1") Integer page,
                           @RequestParam(defaultValue = "10") Integer size) {
        Long orgId = getCurrentOrgId();
        Page<ProcProcessRecord> p = new Page<>(page, size);
        return R.ok(processRecordMapper.selectPage(p,
                new LambdaQueryWrapper<ProcProcessRecord>()
                        .eq(orgId != null, ProcProcessRecord::getOrgId, orgId)
                        .orderByDesc(ProcProcessRecord::getCreatedAt)));
    }

    @Operation(summary = "新增加工记录")
    @PostMapping("/record")
    public R<Void> addRecord(@RequestBody ProcProcessRecord req) {
        req.setOrgId(getCurrentOrgId());
        req.setCreatedBy(getCurrentUserId());
        req.setCreatedAt(LocalDateTime.now());
        processRecordMapper.insert(req);
        return R.success("加工记录已保存");
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private void enrichRawInboundAuditors(List<ProcRawInbound> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        Set<Long> ids = new HashSet<>();
        for (ProcRawInbound row : records) {
            if (row.getAuditBy() != null) {
                ids.add(row.getAuditBy());
            }
        }
        if (ids.isEmpty()) {
            return;
        }
        Map<Long, String> names = new HashMap<>();
        for (Long uid : ids) {
            com.tracegrid.sys.entity.SysUser u = userMapper.selectById(uid);
            if (u == null) {
                names.put(uid, "");
            } else if (u.getRealName() != null && !u.getRealName().isBlank()) {
                names.put(uid, u.getRealName());
            } else {
                names.put(uid, u.getUsername() != null ? u.getUsername() : "");
            }
        }
        for (ProcRawInbound row : records) {
            if (row.getAuditBy() != null) {
                row.setAuditorName(names.get(row.getAuditBy()));
            }
        }
    }

    /**
     * 成品与原料联动：同一原料入库单下，各成品批次 quantity 合计不得超过原料入库数量；且原料须已审核通过。
     */
    private void validateProductBatchAgainstRawInbound(ProcProductBatch batch) {
        if (batch.getRawInboundId() == null) {
            return;
        }
        ProcRawInbound raw = rawInboundMapper.selectById(batch.getRawInboundId());
        if (raw == null) {
            throw BusinessException.of("原料入库单不存在");
        }
        if (raw.getOrgId() != null && batch.getOrgId() != null && !raw.getOrgId().equals(batch.getOrgId())) {
            throw BusinessException.of("原料入库单与成品批次须属于同一加工主体");
        }
        if (raw.getStatus() == null || raw.getStatus() != 2) {
            throw BusinessException.of("原料入库须审核通过后，才可创建成品批次");
        }
        if (raw.getQuantity() == null) {
            return;
        }
        BigDecimal newQty = nz(batch.getQuantity());
        BigDecimal sumOthers = productBatchMapper.selectList(
                        new LambdaQueryWrapper<ProcProductBatch>().eq(ProcProductBatch::getRawInboundId, raw.getId()))
                .stream()
                .map(ProcProductBatch::getQuantity)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal total = sumOthers.add(newQty);
        if (total.compareTo(raw.getQuantity()) > 0) {
            throw BusinessException.of(String.format(
                    "同一原料入库单下，成品「当前数量」合计不能超过原料入库数量（原料 %s，已有成品合计 %s，本次新增 %s）。",
                    stripNum(raw.getQuantity()), stripNum(sumOthers), stripNum(newQty)));
        }
    }

    private static String stripNum(BigDecimal v) {
        return v.stripTrailingZeros().toPlainString();
    }

    private Long getCurrentUserId() {
        return Long.parseLong(StpUtil.getLoginIdAsString());
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
     * 统一清洗质检附件，避免 blob: 临时地址入库导致公开页无法加载。
     */
    private void normalizeQualityMedia(ProcQualityInspection req) {
        List<String> urls = extractUrls(req.getImages());
        String report = sanitizeMediaUrl(req.getReportUrl());
        if (!report.isEmpty()) {
            urls.add(report);
        }
        urls = deduplicate(urls);
        req.setImages(urls.isEmpty() ? null : toJsonArray(urls));

        String pdf = urls.stream().filter(this::isPdf).findFirst().orElse("");
        req.setReportUrl(pdf.isEmpty() ? null : pdf);
    }

    private List<String> extractUrls(String raw) {
        if (raw == null || raw.isBlank()) return new ArrayList<>();
        String text = raw.trim();
        if ("null".equalsIgnoreCase(text) || "undefined".equalsIgnoreCase(text) || "-".equals(text)) {
            return new ArrayList<>();
        }
        String normalized = text;
        if (normalized.startsWith("[") && normalized.endsWith("]")) {
            normalized = normalized.substring(1, normalized.length() - 1);
        }
        String[] parts = normalized.split("[,\\n;|]");
        List<String> result = new ArrayList<>();
        for (String part : parts) {
            String cleaned = part == null ? "" : part.trim();
            if ((cleaned.startsWith("\"") && cleaned.endsWith("\"")) || (cleaned.startsWith("'") && cleaned.endsWith("'"))) {
                cleaned = cleaned.substring(1, cleaned.length() - 1).trim();
            }
            String url = sanitizeMediaUrl(cleaned);
            if (!url.isEmpty()) {
                result.add(url);
            }
        }
        return result;
    }

    private String sanitizeMediaUrl(String value) {
        if (value == null) return "";
        String url = value.trim();
        if (url.isEmpty()) return "";
        if (url.startsWith("blob:")) return "";
        return url;
    }

    private List<String> deduplicate(List<String> urls) {
        Set<String> unique = new LinkedHashSet<>(urls);
        return new ArrayList<>(unique);
    }

    private String toJsonArray(List<String> urls) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < urls.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append("\"").append(urls.get(i).replace("\\", "\\\\").replace("\"", "\\\"")).append("\"");
        }
        sb.append("]");
        return sb.toString();
    }

    private boolean isPdf(String url) {
        if (url == null) return false;
        String lower = url.toLowerCase();
        int q = lower.indexOf('?');
        if (q >= 0) lower = lower.substring(0, q);
        return lower.endsWith(".pdf");
    }
}
