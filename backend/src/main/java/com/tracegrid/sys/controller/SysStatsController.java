package com.tracegrid.sys.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tracegrid.common.result.R;
import com.tracegrid.complaint.entity.ComplaintFeedback;
import com.tracegrid.complaint.mapper.ComplaintFeedbackMapper;
import com.tracegrid.dist.entity.DistInoutRecord;
import com.tracegrid.dist.entity.DistSaleRecord;
import com.tracegrid.dist.mapper.DistInoutRecordMapper;
import com.tracegrid.dist.mapper.DistLogisticsRecordMapper;
import com.tracegrid.dist.mapper.DistSaleRecordMapper;
import com.tracegrid.proc.mapper.ProcProductBatchMapper;
import com.tracegrid.proc.mapper.ProcRawInboundMapper;
import com.tracegrid.prod.mapper.ProdBatchMapper;
import com.tracegrid.prod.mapper.ProdFarmingRecordMapper;
import com.tracegrid.prod.mapper.ProdPlotMapper;
import com.tracegrid.sys.entity.SysUser;
import com.tracegrid.sys.mapper.SysRegisterApplyMapper;
import com.tracegrid.sys.mapper.SysUserMapper;
import com.tracegrid.trace.entity.TraceCode;
import com.tracegrid.trace.mapper.TraceCodeMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 系统统计接口
 */
@Tag(name = "系统统计")
@RestController
@RequestMapping("/sys/stats")
@RequiredArgsConstructor
public class SysStatsController {

    private final SysUserMapper userMapper;
    private final SysRegisterApplyMapper registerMapper;
    private final ProdBatchMapper batchMapper;
    private final TraceCodeMapper traceCodeMapper;
    private final ComplaintFeedbackMapper complaintMapper;
    
    private final ProdPlotMapper plotMapper;
    private final ProdFarmingRecordMapper farmingMapper;
    private final ProcRawInboundMapper rawInboundMapper;
    private final ProcProductBatchMapper productBatchMapper;
    private final DistInoutRecordMapper inoutRecordMapper;
    private final DistLogisticsRecordMapper logisticsMapper;
    private final DistSaleRecordMapper distSaleRecordMapper;
    
    private final com.tracegrid.sys.mapper.SysOrgMapper orgMapper;
    private final com.tracegrid.proc.mapper.ProcQualityInspectionMapper qualityMapper;

    @Operation(summary = "业务主体概览指标")
    @GetMapping("/business-overview")
    public R<List<Map<String, Object>>> businessOverview() {
        Long userId = Long.parseLong(StpUtil.getLoginIdAsString());
        SysUser user = userMapper.selectById(userId);
        if (user == null || user.getOrgId() == null) return R.ok(new ArrayList<>());
        
        Long orgId = user.getOrgId();
        List<Map<String, Object>> metrics = new ArrayList<>();
        
        LocalDateTime w7s = LocalDate.now().minusDays(7).atStartOfDay();
        LocalDateTime w7e = LocalDate.now().plusDays(1).atStartOfDay();
        LocalDateTime p7s = LocalDate.now().minusDays(14).atStartOfDay();
        LocalDateTime p7e = LocalDate.now().minusDays(7).atStartOfDay();

        switch (user.getRole()) {
            case 2: // 生产主体（指标为累计；趋势按 created_at 近7日 vs 前7日新增）
                metrics.add(metricWithRoll(
                        "地块总数",
                        plotMapper.selectCount(new LambdaQueryWrapper<com.tracegrid.prod.entity.ProdPlot>().eq(com.tracegrid.prod.entity.ProdPlot::getOrgId, orgId)),
                        "Location", "#3B82F6",
                        orgId, this::plotCreatedBetween, w7s, w7e, p7s, p7e));
                metrics.add(metricWithRoll(
                        "农事记录",
                        farmingMapper.selectCount(new LambdaQueryWrapper<com.tracegrid.prod.entity.ProdFarmingRecord>().eq(com.tracegrid.prod.entity.ProdFarmingRecord::getOrgId, orgId)),
                        "Edit", "#10B981",
                        orgId, this::farmingCreatedBetween, w7s, w7e, p7s, p7e));
                metrics.add(metricWithRoll(
                        "生产批次",
                        batchMapper.selectCount(new LambdaQueryWrapper<com.tracegrid.prod.entity.ProdBatch>().eq(com.tracegrid.prod.entity.ProdBatch::getOrgId, orgId)),
                        "Box", "#F59E0B",
                        orgId, this::prodBatchCreatedBetween, w7s, w7e, p7s, p7e));
                break;
            case 3: // 加工企业
                metrics.add(metricWithRoll(
                        "原料入库",
                        rawInboundMapper.selectCount(new LambdaQueryWrapper<com.tracegrid.proc.entity.ProcRawInbound>().eq(com.tracegrid.proc.entity.ProcRawInbound::getOrgId, orgId)),
                        "SoldOut", "#3B82F6",
                        orgId, this::rawInboundCreatedBetween, w7s, w7e, p7s, p7e));
                metrics.add(metricWithRoll(
                        "成品批次",
                        productBatchMapper.selectCount(new LambdaQueryWrapper<com.tracegrid.proc.entity.ProcProductBatch>().eq(com.tracegrid.proc.entity.ProcProductBatch::getOrgId, orgId)),
                        "Chicken", "#8B5CF6",
                        orgId, this::procBatchCreatedBetween, w7s, w7e, p7s, p7e));
                metrics.add(metricWithRoll(
                        "质检报告",
                        qualityMapper.selectCount(new LambdaQueryWrapper<com.tracegrid.proc.entity.ProcQualityInspection>().eq(com.tracegrid.proc.entity.ProcQualityInspection::getOrgId, orgId)),
                        "CircleCheck", "#10B981",
                        orgId, this::qualityCreatedBetween, w7s, w7e, p7s, p7e));
                break;
            case 4: // 流通经销
                metrics.add(metricWithRoll(
                        "入库单据",
                        inoutRecordMapper.selectCount(new LambdaQueryWrapper<DistInoutRecord>().eq(DistInoutRecord::getOrgId, orgId).eq(DistInoutRecord::getInoutType, 1)),
                        "Document", "#3B82F6",
                        orgId, this::distInboundCreatedBetween, w7s, w7e, p7s, p7e));
                metrics.add(metricWithRoll(
                        "出库单据",
                        inoutRecordMapper.selectCount(new LambdaQueryWrapper<DistInoutRecord>().eq(DistInoutRecord::getOrgId, orgId).eq(DistInoutRecord::getInoutType, 2)),
                        "Sort", "#F59E0B",
                        orgId, this::distOutboundCreatedBetween, w7s, w7e, p7s, p7e));
                metrics.add(metricWithRoll(
                        "销售登记",
                        distSaleRecordMapper.selectCount(new LambdaQueryWrapper<DistSaleRecord>().eq(DistSaleRecord::getOrgId, orgId)),
                        "Shop", "#8B5CF6",
                        orgId, this::distSaleCreatedBetween, w7s, w7e, p7s, p7e));
                metrics.add(metricWithRoll(
                        "物流运单",
                        logisticsMapper.selectCount(new LambdaQueryWrapper<com.tracegrid.dist.entity.DistLogisticsRecord>().eq(com.tracegrid.dist.entity.DistLogisticsRecord::getOrgId, orgId)),
                        "Van", "#10B981",
                        orgId, this::logisticsCreatedBetween, w7s, w7e, p7s, p7e));
                break;
        }
        
        return R.ok(metrics);
    }
    @Operation(summary = "管理员概览指标")
    @GetMapping("/admin-overview")
    public R<List<Map<String, Object>>> adminOverview() {
        List<Map<String, Object>> metrics = new ArrayList<>();
        LocalDateTime t0 = LocalDate.now().atStartOfDay();
        LocalDateTime y0 = LocalDate.now().minusDays(1).atStartOfDay();
        LocalDateTime y1 = LocalDate.now().atStartOfDay();
        LocalDateTime w7s = LocalDate.now().minusDays(7).atStartOfDay();
        LocalDateTime w7e = LocalDate.now().plusDays(1).atStartOfDay();
        LocalDateTime p7s = LocalDate.now().minusDays(14).atStartOfDay();
        LocalDateTime p7e = LocalDate.now().minusDays(7).atStartOfDay();

        long pendingAudit = registerMapper.selectCount(new LambdaQueryWrapper<com.tracegrid.sys.entity.SysRegisterApply>().eq(com.tracegrid.sys.entity.SysRegisterApply::getStatus, 0));
        long pendToday = registerMapper.selectCount(new LambdaQueryWrapper<com.tracegrid.sys.entity.SysRegisterApply>()
                .eq(com.tracegrid.sys.entity.SysRegisterApply::getStatus, 0)
                .ge(com.tracegrid.sys.entity.SysRegisterApply::getCreatedAt, t0));
        long pendYest = registerMapper.selectCount(new LambdaQueryWrapper<com.tracegrid.sys.entity.SysRegisterApply>()
                .eq(com.tracegrid.sys.entity.SysRegisterApply::getStatus, 0)
                .ge(com.tracegrid.sys.entity.SysRegisterApply::getCreatedAt, y0)
                .lt(com.tracegrid.sys.entity.SysRegisterApply::getCreatedAt, y1));
        metrics.add(createMetric("待审核申请", pendingAudit, "DocumentChecked", "#3B82F6",
                String.format("待审新单：今日提交 %d · 昨日提交 %d（按创建时间）", pendToday, pendYest), pendToday >= pendYest));

        long batchCount = batchMapper.selectCount(null) + productBatchMapper.selectCount(null);
        long bLast = prodBatchCreatedBetween(null, w7s, w7e) + procBatchCreatedBetween(null, w7s, w7e);
        long bPrev = prodBatchCreatedBetween(null, p7s, p7e) + procBatchCreatedBetween(null, p7s, p7e);
        metrics.add(createMetric("业务批次总数", batchCount, "Box", "#1A7A4A", rollingCompareText(bLast, bPrev), bLast >= bPrev));

        long complaints = complaintMapper.selectCount(new LambdaQueryWrapper<ComplaintFeedback>().eq(ComplaintFeedback::getStatus, 0));
        long cToday = complaintMapper.selectCount(new LambdaQueryWrapper<ComplaintFeedback>()
                .eq(ComplaintFeedback::getStatus, 0)
                .ge(ComplaintFeedback::getCreatedAt, t0));
        long cYest = complaintMapper.selectCount(new LambdaQueryWrapper<ComplaintFeedback>()
                .eq(ComplaintFeedback::getStatus, 0)
                .ge(ComplaintFeedback::getCreatedAt, y0)
                .lt(ComplaintFeedback::getCreatedAt, y1));
        metrics.add(createMetric("待处理投诉", complaints, "ChatLineSquare", "#E0881A",
                String.format("待处理新投诉：今日 %d · 昨日 %d（按创建时间）", cToday, cYest), cToday >= cYest));

        long orgCount = orgMapper.selectCount(null);
        long oLast = orgCreatedBetween(w7s, w7e);
        long oPrev = orgCreatedBetween(p7s, p7e);
        metrics.add(createMetric("已入驻主体", orgCount, "OfficeBuilding", "#8B5CF6", rollingCompareText(oLast, oPrev), oLast >= oPrev));

        return R.ok(metrics);
    }

    @Operation(summary = "详细业务统计指标")
    @GetMapping("/full-metrics")
    public R<List<Map<String, Object>>> fullMetrics() {
        List<Map<String, Object>> metrics = new ArrayList<>();
        
        // 1. 总追溯码生成量
        long totalCodes = traceCodeMapper.selectCount(null);
        LocalDateTime w7s = LocalDate.now().minusDays(7).atStartOfDay();
        LocalDateTime w7e = LocalDate.now().plusDays(1).atStartOfDay();
        LocalDateTime p7s = LocalDate.now().minusDays(14).atStartOfDay();
        LocalDateTime p7e = LocalDate.now().minusDays(7).atStartOfDay();
        long codesLast7 = traceCodeCreatedBetween(w7s, w7e);
        long codesPrev7 = traceCodeCreatedBetween(p7s, p7e);
        metrics.add(createMetric("追溯码赋码总量", totalCodes, "Ticket", "#8B5CF6", rollingCompareText(codesLast7, codesPrev7), codesLast7 >= codesPrev7));

        long totalQuality = qualityMapper.selectCount(null);
        String passRate = "100%";
        if (totalQuality > 0) {
            long passCount = qualityMapper.selectCount(new LambdaQueryWrapper<com.tracegrid.proc.entity.ProcQualityInspection>().eq(com.tracegrid.proc.entity.ProcQualityInspection::getResult, 1));
            passRate = String.format("%.1f%%", (double) passCount * 100 / totalQuality);
        }
        metrics.add(createMetric("质检合格率", passRate, "CircleCheck", "#1A7A4A",
                "基于全量检测记录统计，样本 " + totalQuality + " 笔", true));

        long totalBatches = batchMapper.selectCount(null) + productBatchMapper.selectCount(null);
        long batLast = prodBatchCreatedBetween(null, w7s, w7e) + procBatchCreatedBetween(null, w7s, w7e);
        long batPrev = prodBatchCreatedBetween(null, p7s, p7e) + procBatchCreatedBetween(null, p7s, p7e);
        metrics.add(createMetric("溯源批次总数", totalBatches, "Box", "#3B82F6", rollingCompareText(batLast, batPrev), batLast >= batPrev));

        long totalOrgs = orgMapper.selectCount(null);
        long orgLast = orgCreatedBetween(w7s, w7e);
        long orgPrev = orgCreatedBetween(p7s, p7e);
        metrics.add(createMetric("入驻主体总数", totalOrgs, "OfficeBuilding", "#E0881A", rollingCompareText(orgLast, orgPrev), orgLast >= orgPrev));
        
        return R.ok(metrics);
    }

    @Operation(summary = "主体活跃度排行")
    @GetMapping("/entity-rank")
    public R<List<Map<String, Object>>> entityRank() {
        List<com.tracegrid.sys.entity.SysOrg> orgs = orgMapper.selectList(new LambdaQueryWrapper<com.tracegrid.sys.entity.SysOrg>().orderByDesc(com.tracegrid.sys.entity.SysOrg::getCreatedAt));
        List<Map<String, Object>> list = new ArrayList<>();
        
        for (com.tracegrid.sys.entity.SysOrg org : orgs) {
            long bizCount = 0;
            if (org.getType() == 1) { // 生产：批次 + 农事活跃度
                Long oid = org.getId();
                bizCount = batchMapper.selectCount(new LambdaQueryWrapper<com.tracegrid.prod.entity.ProdBatch>().eq(com.tracegrid.prod.entity.ProdBatch::getOrgId, oid))
                        + farmingMapper.selectCount(new LambdaQueryWrapper<com.tracegrid.prod.entity.ProdFarmingRecord>().eq(com.tracegrid.prod.entity.ProdFarmingRecord::getOrgId, oid));
            } else if (org.getType() == 2) { // 加工：成品批次 + 原料入库
                Long oid = org.getId();
                bizCount = productBatchMapper.selectCount(new LambdaQueryWrapper<com.tracegrid.proc.entity.ProcProductBatch>().eq(com.tracegrid.proc.entity.ProcProductBatch::getOrgId, oid))
                        + rawInboundMapper.selectCount(new LambdaQueryWrapper<com.tracegrid.proc.entity.ProcRawInbound>().eq(com.tracegrid.proc.entity.ProcRawInbound::getOrgId, oid));
            } else if (org.getType() == 3) { // 流通：出入库 + 物流 + 销售一并计入活跃度
                Long oid = org.getId();
                bizCount = logisticsMapper.selectCount(new LambdaQueryWrapper<com.tracegrid.dist.entity.DistLogisticsRecord>().eq(com.tracegrid.dist.entity.DistLogisticsRecord::getOrgId, oid))
                        + inoutRecordMapper.selectCount(new LambdaQueryWrapper<DistInoutRecord>().eq(DistInoutRecord::getOrgId, oid))
                        + distSaleRecordMapper.selectCount(new LambdaQueryWrapper<DistSaleRecord>().eq(DistSaleRecord::getOrgId, oid));
            }
            
            Map<String, Object> item = new HashMap<>();
            item.put("name", org.getName());
            item.put("type", getRoleName(org.getType() + 1));
            item.put("batchCount", bizCount);
            // 活跃度简单算法：业务量 * 20 (封顶100)
            item.put("activeRate", Math.min(100, bizCount * 20));
            list.add(item);
        }
        
        // 排序取前 5
        list.sort((a, b) -> Long.compare(((Number) b.get("batchCount")).longValue(), ((Number) a.get("batchCount")).longValue()));
        return R.ok(list.size() > 5 ? list.subList(0, 5) : list);
    }

    private String getRoleName(int role) {
        Map<Integer, String> map = Map.of(2, "生产主体", 3, "加工企业", 4, "流通经销");
        return map.getOrDefault(role, "其他");
    }

    @Operation(summary = "近30天全链路日新增趋势（按 created_at：生产批次、成品批次、赋码、流通入库）")
    @GetMapping("/batch-trend")
    public R<Map<String, Object>> batchTrend() {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(29);

        List<String> xData = new ArrayList<>();
        List<Long> yProd = new ArrayList<>();
        List<Long> yProc = new ArrayList<>();
        List<Long> yTrace = new ArrayList<>();
        List<Long> yDistIn = new ArrayList<>();
        List<Long> ySum = new ArrayList<>();

        for (int i = 0; i < 30; i++) {
            LocalDate date = start.plusDays(i);
            xData.add(date.getMonthValue() + "/" + date.getDayOfMonth());
            LocalDateTime d0 = date.atStartOfDay();
            LocalDateTime d1 = date.plusDays(1).atStartOfDay();
            long prod = batchMapper.selectCount(new LambdaQueryWrapper<com.tracegrid.prod.entity.ProdBatch>()
                    .ge(com.tracegrid.prod.entity.ProdBatch::getCreatedAt, d0)
                    .lt(com.tracegrid.prod.entity.ProdBatch::getCreatedAt, d1));
            long proc = productBatchMapper.selectCount(new LambdaQueryWrapper<com.tracegrid.proc.entity.ProcProductBatch>()
                    .ge(com.tracegrid.proc.entity.ProcProductBatch::getCreatedAt, d0)
                    .lt(com.tracegrid.proc.entity.ProcProductBatch::getCreatedAt, d1));
            long trace = traceCodeMapper.selectCount(new LambdaQueryWrapper<TraceCode>()
                    .ge(TraceCode::getCreatedAt, d0)
                    .lt(TraceCode::getCreatedAt, d1));
            long distIn = inoutRecordMapper.selectCount(new LambdaQueryWrapper<DistInoutRecord>()
                    .eq(DistInoutRecord::getInoutType, 1)
                    .ge(DistInoutRecord::getCreatedAt, d0)
                    .lt(DistInoutRecord::getCreatedAt, d1));
            yProd.add(prod);
            yProc.add(proc);
            yTrace.add(trace);
            yDistIn.add(distIn);
            ySum.add(prod + proc + trace + distIn);
        }

        Map<String, Object> res = new HashMap<>();
        res.put("xData", xData);
        res.put("yData", ySum);
        List<Map<String, Object>> series = new ArrayList<>();
        series.add(Map.of("name", "生产批次", "yData", new ArrayList<>(yProd)));
        series.add(Map.of("name", "成品批次", "yData", new ArrayList<>(yProc)));
        series.add(Map.of("name", "追溯赋码", "yData", new ArrayList<>(yTrace)));
        series.add(Map.of("name", "流通入库单", "yData", new ArrayList<>(yDistIn)));
        res.put("series", series);
        return R.ok(res);
    }

    @Operation(summary = "用户角色分布图数据")
    @GetMapping("/user-role-dist")
    public R<List<Map<String, Object>>> userRoleDist() {
        List<Map<String, Object>> data = new ArrayList<>();
        String[] names = {"管理员", "生产主体", "加工企业", "流通经销"};
        Integer[] roles = {1, 2, 3, 4};
        String[] colors = {"#6B7280", "#1A7A4A", "#3B82F6", "#C9A227"};
        
        for (int i = 0; i < roles.length; i++) {
            long count = userMapper.selectCount(new LambdaQueryWrapper<com.tracegrid.sys.entity.SysUser>().eq(com.tracegrid.sys.entity.SysUser::getRole, roles[i]));
            if (count > 0) {
                Map<String, Object> item = new HashMap<>();
                item.put("name", names[i]);
                item.put("value", count);
                item.put("itemStyle", Map.of("color", colors[i]));
                data.add(item);
            }
        }
        // 如果全为空，给个保底显示
        if (data.isEmpty()) {
            data.add(Map.of("name", "暂无数据", "value", 0));
        }
        return R.ok(data);
    }

    @Operation(summary = "流通经销看板图表（近7天业务动态与全量销售渠道占比）")
    @GetMapping("/dist-charts")
    public R<Map<String, Object>> distCharts() {
        Long userId = Long.parseLong(StpUtil.getLoginIdAsString());
        SysUser user = userMapper.selectById(userId);
        Map<String, Object> empty = new HashMap<>();
        empty.put("xLabels", new ArrayList<String>());
        empty.put("seriesInbound", new ArrayList<Long>());
        empty.put("seriesOutbound", new ArrayList<Long>());
        empty.put("seriesSale", new ArrayList<Long>());
        empty.put("saleChannelPie", new ArrayList<Map<String, Object>>());
        if (user == null || user.getOrgId() == null || user.getRole() == null || user.getRole() != 4) {
            return R.ok(empty);
        }
        Long orgId = user.getOrgId();
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(6);
        LocalDateTime rangeStart = start.atStartOfDay();

        List<DistInoutRecord> inouts = inoutRecordMapper.selectList(
                new LambdaQueryWrapper<DistInoutRecord>()
                        .eq(DistInoutRecord::getOrgId, orgId)
                        .ge(DistInoutRecord::getCreatedAt, rangeStart));
        List<DistSaleRecord> salesInRange = distSaleRecordMapper.selectList(
                new LambdaQueryWrapper<DistSaleRecord>()
                        .eq(DistSaleRecord::getOrgId, orgId)
                        .ge(DistSaleRecord::getCreatedAt, rangeStart));

        List<String> xLabels = new ArrayList<>();
        List<Long> inbound = new ArrayList<>();
        List<Long> outbound = new ArrayList<>();
        List<Long> saleDaily = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate day = start.plusDays(i);
            xLabels.add(day.getMonthValue() + "/" + day.getDayOfMonth());
            long inC = inouts.stream()
                    .filter(r -> r.getInoutType() != null && r.getInoutType() == 1
                            && r.getCreatedAt() != null && day.equals(r.getCreatedAt().toLocalDate()))
                    .count();
            long outC = inouts.stream()
                    .filter(r -> r.getInoutType() != null && r.getInoutType() == 2
                            && r.getCreatedAt() != null && day.equals(r.getCreatedAt().toLocalDate()))
                    .count();
            long sC = salesInRange.stream()
                    .filter(r -> r.getCreatedAt() != null && day.equals(r.getCreatedAt().toLocalDate()))
                    .count();
            inbound.add(inC);
            outbound.add(outC);
            saleDaily.add(sC);
        }

        List<DistSaleRecord> allOrgSales = distSaleRecordMapper.selectList(
                new LambdaQueryWrapper<DistSaleRecord>().eq(DistSaleRecord::getOrgId, orgId));
        Map<String, Long> methodCount = allOrgSales.stream()
                .map(DistSaleRecord::getPurchaseMethod)
                .collect(Collectors.groupingBy(m -> (m == null || m.isBlank()) ? "未填写" : m.trim(), Collectors.counting()));
        List<Map<String, Object>> pie = new ArrayList<>();
        methodCount.forEach((name, value) -> {
            Map<String, Object> slice = new HashMap<>();
            slice.put("name", name);
            slice.put("value", value);
            pie.add(slice);
        });
        if (pie.isEmpty()) {
            Map<String, Object> placeholder = new HashMap<>();
            placeholder.put("name", "暂无销售数据");
            placeholder.put("value", 0L);
            pie.add(placeholder);
        }

        Map<String, Object> res = new HashMap<>();
        res.put("xLabels", xLabels);
        res.put("seriesInbound", inbound);
        res.put("seriesOutbound", outbound);
        res.put("seriesSale", saleDaily);
        res.put("saleChannelPie", pie);
        return R.ok(res);
    }

    private Map<String, Object> createMetric(String label, Object value, String icon, String color, String trendText, boolean up) {
        Map<String, Object> m = new HashMap<>();
        m.put("label", label);
        m.put("value", value);
        m.put("icon", icon);
        m.put("color", color);
        m.put("trend", trendText);
        m.put("up", up);
        return m;
    }

    @FunctionalInterface
    private interface OrgRangeCounter {
        long count(Long orgId, LocalDateTime fromInclusive, LocalDateTime toExclusive);
    }

    private Map<String, Object> metricWithRoll(String label, long total, String icon, String color, Long orgId,
                                               OrgRangeCounter counter,
                                               LocalDateTime w7s, LocalDateTime w7e, LocalDateTime p7s, LocalDateTime p7e) {
        long last7 = counter.count(orgId, w7s, w7e);
        long prev7 = counter.count(orgId, p7s, p7e);
        return createMetric(label, total, icon, color, rollingCompareText(last7, prev7), last7 >= prev7);
    }

    private static String rollingCompareText(long last7, long prev7) {
        long d = last7 - prev7;
        return String.format("近7日新增 %d · 较前7日 %+d（按创建时间）", last7, d);
    }

    private long plotCreatedBetween(Long orgId, LocalDateTime from, LocalDateTime toEx) {
        return plotMapper.selectCount(new LambdaQueryWrapper<com.tracegrid.prod.entity.ProdPlot>()
                .eq(com.tracegrid.prod.entity.ProdPlot::getOrgId, orgId)
                .ge(com.tracegrid.prod.entity.ProdPlot::getCreatedAt, from)
                .lt(com.tracegrid.prod.entity.ProdPlot::getCreatedAt, toEx));
    }

    private long farmingCreatedBetween(Long orgId, LocalDateTime from, LocalDateTime toEx) {
        return farmingMapper.selectCount(new LambdaQueryWrapper<com.tracegrid.prod.entity.ProdFarmingRecord>()
                .eq(com.tracegrid.prod.entity.ProdFarmingRecord::getOrgId, orgId)
                .ge(com.tracegrid.prod.entity.ProdFarmingRecord::getCreatedAt, from)
                .lt(com.tracegrid.prod.entity.ProdFarmingRecord::getCreatedAt, toEx));
    }

    private long prodBatchCreatedBetween(Long orgId, LocalDateTime from, LocalDateTime toEx) {
        LambdaQueryWrapper<com.tracegrid.prod.entity.ProdBatch> w = new LambdaQueryWrapper<com.tracegrid.prod.entity.ProdBatch>()
                .ge(com.tracegrid.prod.entity.ProdBatch::getCreatedAt, from)
                .lt(com.tracegrid.prod.entity.ProdBatch::getCreatedAt, toEx);
        if (orgId != null) {
            w.eq(com.tracegrid.prod.entity.ProdBatch::getOrgId, orgId);
        }
        return batchMapper.selectCount(w);
    }

    private long rawInboundCreatedBetween(Long orgId, LocalDateTime from, LocalDateTime toEx) {
        return rawInboundMapper.selectCount(new LambdaQueryWrapper<com.tracegrid.proc.entity.ProcRawInbound>()
                .eq(com.tracegrid.proc.entity.ProcRawInbound::getOrgId, orgId)
                .ge(com.tracegrid.proc.entity.ProcRawInbound::getCreatedAt, from)
                .lt(com.tracegrid.proc.entity.ProcRawInbound::getCreatedAt, toEx));
    }

    private long procBatchCreatedBetween(Long orgId, LocalDateTime from, LocalDateTime toEx) {
        LambdaQueryWrapper<com.tracegrid.proc.entity.ProcProductBatch> w = new LambdaQueryWrapper<com.tracegrid.proc.entity.ProcProductBatch>()
                .ge(com.tracegrid.proc.entity.ProcProductBatch::getCreatedAt, from)
                .lt(com.tracegrid.proc.entity.ProcProductBatch::getCreatedAt, toEx);
        if (orgId != null) {
            w.eq(com.tracegrid.proc.entity.ProcProductBatch::getOrgId, orgId);
        }
        return productBatchMapper.selectCount(w);
    }

    private long qualityCreatedBetween(Long orgId, LocalDateTime from, LocalDateTime toEx) {
        return qualityMapper.selectCount(new LambdaQueryWrapper<com.tracegrid.proc.entity.ProcQualityInspection>()
                .eq(com.tracegrid.proc.entity.ProcQualityInspection::getOrgId, orgId)
                .ge(com.tracegrid.proc.entity.ProcQualityInspection::getCreatedAt, from)
                .lt(com.tracegrid.proc.entity.ProcQualityInspection::getCreatedAt, toEx));
    }

    private long distInoutCreatedBetween(Long orgId, int inoutType, LocalDateTime from, LocalDateTime toEx) {
        return inoutRecordMapper.selectCount(new LambdaQueryWrapper<DistInoutRecord>()
                .eq(DistInoutRecord::getOrgId, orgId)
                .eq(DistInoutRecord::getInoutType, inoutType)
                .ge(DistInoutRecord::getCreatedAt, from)
                .lt(DistInoutRecord::getCreatedAt, toEx));
    }

    private long distInboundCreatedBetween(Long orgId, LocalDateTime from, LocalDateTime toEx) {
        return distInoutCreatedBetween(orgId, 1, from, toEx);
    }

    private long distOutboundCreatedBetween(Long orgId, LocalDateTime from, LocalDateTime toEx) {
        return distInoutCreatedBetween(orgId, 2, from, toEx);
    }

    private long distSaleCreatedBetween(Long orgId, LocalDateTime from, LocalDateTime toEx) {
        return distSaleRecordMapper.selectCount(new LambdaQueryWrapper<DistSaleRecord>()
                .eq(DistSaleRecord::getOrgId, orgId)
                .ge(DistSaleRecord::getCreatedAt, from)
                .lt(DistSaleRecord::getCreatedAt, toEx));
    }

    private long logisticsCreatedBetween(Long orgId, LocalDateTime from, LocalDateTime toEx) {
        return logisticsMapper.selectCount(new LambdaQueryWrapper<com.tracegrid.dist.entity.DistLogisticsRecord>()
                .eq(com.tracegrid.dist.entity.DistLogisticsRecord::getOrgId, orgId)
                .ge(com.tracegrid.dist.entity.DistLogisticsRecord::getCreatedAt, from)
                .lt(com.tracegrid.dist.entity.DistLogisticsRecord::getCreatedAt, toEx));
    }

    private long traceCodeCreatedBetween(LocalDateTime from, LocalDateTime toEx) {
        return traceCodeMapper.selectCount(new LambdaQueryWrapper<TraceCode>()
                .ge(TraceCode::getCreatedAt, from)
                .lt(TraceCode::getCreatedAt, toEx));
    }

    private long orgCreatedBetween(LocalDateTime from, LocalDateTime toEx) {
        return orgMapper.selectCount(new LambdaQueryWrapper<com.tracegrid.sys.entity.SysOrg>()
                .ge(com.tracegrid.sys.entity.SysOrg::getCreatedAt, from)
                .lt(com.tracegrid.sys.entity.SysOrg::getCreatedAt, toEx));
    }
}
