package com.tracegrid.trace.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tracegrid.common.exception.BusinessException;
import com.tracegrid.common.result.R;
import com.tracegrid.common.security.SecurityUtils;
import com.tracegrid.proc.entity.ProcProductBatch;
import com.tracegrid.proc.entity.ProcRawInbound;
import com.tracegrid.proc.mapper.ProcProductBatchMapper;
import com.tracegrid.proc.mapper.ProcRawInboundMapper;
import com.tracegrid.prod.entity.ProdBatch;
import com.tracegrid.prod.mapper.ProdBatchMapper;
import com.tracegrid.sys.entity.SysUser;
import com.tracegrid.sys.mapper.SysUserMapper;
import com.tracegrid.trace.entity.TraceCode;
import com.tracegrid.trace.mapper.TraceCodeMapper;
import com.tracegrid.trace.service.TraceGenerationRuleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 溯源码/追溯码：业务端赋码与作废；管理端全平台查询、规则配置、批量作废。
 */
@Tag(name = "溯源码管理")
@RestController
@RequestMapping("/trace")
@RequiredArgsConstructor
public class TraceCodeController {

    private final TraceCodeMapper traceCodeMapper;
    private final ProcProductBatchMapper productBatchMapper;
    private final ProdBatchMapper prodBatchMapper;
    private final ProcRawInboundMapper rawInboundMapper;
    private final SysUserMapper userMapper;
    private final TraceGenerationRuleService traceGenerationRuleService;

    @Operation(summary = "全局生成规则（管理员）")
    @GetMapping("/generation-rules")
    public R<TraceGenerationRuleService.TraceGenerationRules> getGenerationRules() {
        SecurityUtils.requireAdmin(userMapper);
        return R.ok(traceGenerationRuleService.load());
    }

    @Operation(summary = "保存全局生成规则（管理员）")
    @PutMapping("/generation-rules")
    public R<Void> saveGenerationRules(@RequestBody TraceGenerationRuleService.TraceGenerationRules body) throws IOException {
        SecurityUtils.requireAdmin(userMapper);
        if (body.getPrefix() == null || body.getPrefix().isBlank()) {
            throw BusinessException.of("前缀不能为空");
        }
        if (body.getMiddleSegment() == null || body.getMiddleSegment().isBlank()) {
            throw BusinessException.of("码段配置不能为空");
        }
        if (body.getMaxGeneratePerRequest() < 1) body.setMaxGeneratePerRequest(1);
        if (body.getMaxGenerateAdmin() < 1) body.setMaxGenerateAdmin(1);
        traceGenerationRuleService.save(body);
        return R.success("已保存");
    }

    @Operation(summary = "追溯码列表（管理员全平台；业务端仅本主体相关）")
    @GetMapping("/list")
    public R<?> list(@RequestParam(defaultValue = "1") Integer page,
                     @RequestParam(defaultValue = "10") Integer size,
                     @RequestParam(required = false) String code,
                     @RequestParam(required = false) Integer status) {
        SysUser user = currentUser();
        Page<TraceCode> p = new Page<>(page, size);
        LambdaQueryWrapper<TraceCode> wrapper = new LambdaQueryWrapper<TraceCode>()
                .like(code != null && !code.isBlank(), TraceCode::getCode, code)
                .eq(status != null, TraceCode::getStatus, status)
                .orderByDesc(TraceCode::getCreatedAt);

        if (user.getRole() == null || user.getRole() != 1) {
            applyBusinessScope(wrapper, user);
        }
        return R.ok(traceCodeMapper.selectPage(p, wrapper));
    }

    private void applyBusinessScope(LambdaQueryWrapper<TraceCode> wrapper, SysUser user) {
        Long orgId = user.getOrgId();
        if (orgId == null) {
            wrapper.apply("1=0");
            return;
        }
        if (user.getRole() != null && user.getRole() == 4) {
            wrapper.apply("1=0");
            return;
        }
        Set<Long> prodIds = prodBatchMapper.selectList(new LambdaQueryWrapper<ProdBatch>()
                        .eq(ProdBatch::getOrgId, orgId)).stream()
                .map(ProdBatch::getId).collect(Collectors.toSet());
        Set<Long> procIds = productBatchMapper.selectList(new LambdaQueryWrapper<ProcProductBatch>()
                        .eq(ProcProductBatch::getOrgId, orgId)).stream()
                .map(ProcProductBatch::getId).collect(Collectors.toSet());
        if (prodIds.isEmpty() && procIds.isEmpty()) {
            wrapper.apply("1=0");
            return;
        }
        wrapper.and(w -> {
            boolean first = true;
            if (!prodIds.isEmpty()) {
                w.in(TraceCode::getProdBatchId, prodIds);
                first = false;
            }
            if (!procIds.isEmpty()) {
                if (!first) {
                    w.or();
                }
                w.in(TraceCode::getProcBatchId, procIds);
            }
        });
    }

    @Operation(summary = "生成追溯码（业务端：生产/加工；管理员禁止调用）")
    @PostMapping("/generate")
    public R<Void> generate(@RequestBody TraceCodeRequest req) {
        SysUser user = currentUser();
        if (user.getRole() != null && user.getRole() == 1) {
            throw BusinessException.of("管理员请在「全局生成规则」中维护规则；赋码由生产/加工主体在各自工作台操作。");
        }
        if (req.getCount() == null || req.getCount() <= 0) req.setCount(1);
        TraceGenerationRuleService.TraceGenerationRules rules = traceGenerationRuleService.load();
        int cap = rules.getMaxGeneratePerRequest() > 0 ? rules.getMaxGeneratePerRequest() : 500;
        if (req.getCount() > cap) {
            throw BusinessException.of("单次生成数量不可超过 " + cap + "（由全局规则配置）");
        }

        validateGenerateRequest(user, req);

        Long userId = user.getId();
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = sanitizeSegment(rules.getPrefix());
        String mid = sanitizeSegment(rules.getMiddleSegment());

        for (int i = 0; i < req.getCount(); i++) {
            TraceCode tc = new TraceCode();
            tc.setProdBatchId(req.getProdBatchId());
            tc.setProcBatchId(req.getProcBatchId());
            if (tc.getProcBatchId() != null) {
                ProcProductBatch pb = productBatchMapper.selectById(tc.getProcBatchId());
                if (pb != null && pb.getRawInboundId() != null) {
                    ProcRawInbound ri = rawInboundMapper.selectById(pb.getRawInboundId());
                    if (ri != null && ri.getProdBatchId() != null) {
                        tc.setProdBatchId(ri.getProdBatchId());
                    }
                }
            }

            String code = buildUniqueCode(prefix, date, mid);
            tc.setCode(code);
            tc.setStatus(1);
            tc.setCreatedBy(userId);
            tc.setCreatedAt(LocalDateTime.now());
            traceCodeMapper.insert(tc);
        }
        return R.success("批量生成成功");
    }

    private static String sanitizeSegment(String s) {
        String t = s == null ? "" : s.replaceAll("[^A-Za-z0-9\\-]", "").trim();
        return t.isEmpty() ? "AG" : t;
    }

    private String buildUniqueCode(String prefix, String date, String mid) {
        Random rnd = new Random();
        for (int attempt = 0; attempt < 50; attempt++) {
            String serial = String.format("%06d", rnd.nextInt(999999));
            String checkBit = String.valueOf((char) ('A' + rnd.nextInt(26)));
            String code = prefix + "-" + date + "-" + mid + "-" + serial + "-" + rnd.nextInt(10) + checkBit;
            Long exists = traceCodeMapper.selectCount(new LambdaQueryWrapper<TraceCode>().eq(TraceCode::getCode, code));
            if (exists == null || exists == 0) {
                return code;
            }
        }
        throw BusinessException.of("生成唯一码失败，请稍后重试");
    }

    private void validateGenerateRequest(SysUser user, TraceCodeRequest req) {
        Long orgId = user.getOrgId();
        if (orgId == null) {
            throw BusinessException.of("当前账号未关联主体，无法赋码");
        }
        Integer role = user.getRole();
        if (role == null || (role != 2 && role != 3)) {
            throw BusinessException.of("当前角色不可生成溯源码（流通端不提供赋码）");
        }
        if (role == 2) {
            if (req.getProdBatchId() == null || req.getProcBatchId() != null) {
                throw BusinessException.of("生产主体仅可选择已审核通过的生产批次生成溯源码");
            }
            ProdBatch pb = prodBatchMapper.selectById(req.getProdBatchId());
            if (pb == null || !orgId.equals(pb.getOrgId()) || pb.getStatus() == null || pb.getStatus() != 1) {
                throw BusinessException.of("仅允许对「本主体且管理员已审核通过」的生产批次赋码");
            }
            enforceQuantityCap(req.getProdBatchId(), null, req.getCount(), pb.getQuantity());
            return;
        }
        if (role == 3) {
            if (req.getProcBatchId() == null || req.getProdBatchId() != null) {
                throw BusinessException.of("加工企业请选择成品批次生成溯源码");
            }
            ProcProductBatch pb = productBatchMapper.selectById(req.getProcBatchId());
            if (pb == null || !orgId.equals(pb.getOrgId())) {
                throw BusinessException.of("成品批次不存在或无权操作");
            }
            if (pb.getQualityStatus() == null || pb.getQualityStatus() != 1) {
                throw BusinessException.of("仅质检合格的成品批次可赋码");
            }
            if (pb.getRawInboundId() == null) {
                throw BusinessException.of("成品批次未关联原料入库单，无法赋码");
            }
            ProcRawInbound ri = rawInboundMapper.selectById(pb.getRawInboundId());
            if (ri == null || ri.getStatus() == null || ri.getStatus() != 2) {
                throw BusinessException.of("仅当关联原料入库单已通过管理员审核时，方可生成溯源码");
            }
            enforceQuantityCap(null, req.getProcBatchId(), req.getCount(), pb.getQuantity());
        }
    }

    private void enforceQuantityCap(Long prodBatchId, Long procBatchId, int addCount, BigDecimal batchQty) {
        if (batchQty == null) {
            return;
        }
        long existing;
        if (procBatchId != null) {
            existing = traceCodeMapper.selectCount(new LambdaQueryWrapper<TraceCode>()
                    .eq(TraceCode::getProcBatchId, procBatchId));
        } else {
            existing = traceCodeMapper.selectCount(new LambdaQueryWrapper<TraceCode>()
                    .eq(TraceCode::getProdBatchId, prodBatchId));
        }
        BigDecimal planned = BigDecimal.valueOf(existing + addCount);
        if (planned.compareTo(batchQty) > 0) {
            throw BusinessException.of(String.format(
                    "赋码数量不能超过批次数量口径（批次数量 %s，已赋码 %d，本次申请 %d）。",
                    batchQty.stripTrailingZeros().toPlainString(), existing, addCount));
        }
    }

    @Operation(summary = "作废追溯码（业务端仅可操作本主体相关码）")
    @PostMapping("/{id}/void")
    public R<Void> voidCode(@PathVariable Long id) {
        SysUser user = currentUser();
        TraceCode tc = traceCodeMapper.selectById(id);
        if (tc == null) throw BusinessException.of("追溯码不存在");
        if (user.getRole() == null || user.getRole() != 1) {
            assertBusinessOwnsTrace(user, tc);
        }
        tc.setStatus(6);
        tc.setUpdatedAt(LocalDateTime.now());
        traceCodeMapper.updateById(tc);
        return R.success("已作废");
    }

    @Operation(summary = "批量作废（仅管理员）")
    @PostMapping("/batch-void")
    public R<Void> batchVoid(@RequestBody List<Long> ids) {
        SecurityUtils.requireAdmin(userMapper);
        if (ids == null || ids.isEmpty()) {
            throw BusinessException.of("请选择至少一条记录");
        }
        for (Long id : new HashSet<>(ids)) {
            TraceCode tc = traceCodeMapper.selectById(id);
            if (tc == null) {
                continue;
            }
            tc.setStatus(6);
            tc.setUpdatedAt(LocalDateTime.now());
            traceCodeMapper.updateById(tc);
        }
        return R.success("批量作废完成");
    }

    @Operation(summary = "追溯码详情")
    @GetMapping("/{id}")
    public R<TraceCode> detail(@PathVariable Long id) {
        SysUser user = currentUser();
        TraceCode tc = traceCodeMapper.selectById(id);
        if (tc == null) {
            return R.ok(null);
        }
        if (user.getRole() == null || user.getRole() != 1) {
            assertBusinessOwnsTrace(user, tc);
        }
        return R.ok(tc);
    }

    private void assertBusinessOwnsTrace(SysUser user, TraceCode tc) {
        Long orgId = user.getOrgId();
        if (orgId == null) {
            throw BusinessException.of("无权操作");
        }
        if (tc.getProcBatchId() != null) {
            ProcProductBatch pb = productBatchMapper.selectById(tc.getProcBatchId());
            if (pb != null && orgId.equals(pb.getOrgId())) {
                return;
            }
        }
        if (tc.getProdBatchId() != null) {
            ProdBatch b = prodBatchMapper.selectById(tc.getProdBatchId());
            if (b != null && orgId.equals(b.getOrgId())) {
                return;
            }
        }
        throw BusinessException.of("无权操作该追溯码");
    }

    private SysUser currentUser() {
        Long userId = Long.parseLong(StpUtil.getLoginIdAsString());
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw BusinessException.of(401, "用户不存在");
        }
        return user;
    }

    @Data
    public static class TraceCodeRequest {
        private Long prodBatchId;
        private Long procBatchId;
        private Integer count;
    }
}
