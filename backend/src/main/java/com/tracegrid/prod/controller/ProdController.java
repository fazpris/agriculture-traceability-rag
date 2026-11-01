package com.tracegrid.prod.controller;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tracegrid.common.exception.BusinessException;
import com.tracegrid.common.result.R;
import com.tracegrid.common.security.SecurityUtils;
import com.tracegrid.prod.entity.ProdFarmingRecord;
import com.tracegrid.prod.entity.ProdBatch;
import com.tracegrid.prod.entity.ProdPlot;
import com.tracegrid.prod.mapper.ProdBatchMapper;
import com.tracegrid.prod.mapper.ProdFarmingRecordMapper;
import com.tracegrid.prod.mapper.ProdPlotMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 生产模块接口
 */
@Tag(name = "生产管理")
@RestController
@RequestMapping("/prod")
@RequiredArgsConstructor
public class ProdController {

    private final ProdPlotMapper plotMapper;
    private final ProdFarmingRecordMapper farmingMapper;
    private final ProdBatchMapper batchMapper;
    private final com.tracegrid.sys.mapper.SysUserMapper userMapper;
    private final com.tracegrid.sys.service.SysMessageService messageService;

    // ============ 地块管理 ============

    @Operation(summary = "地块列表")
    @GetMapping("/plot/list")
    public R<?> plotList(@RequestParam(defaultValue = "1") Integer page,
                         @RequestParam(defaultValue = "10") Integer size) {
        Long orgId = getCurrentOrgId();
        Page<ProdPlot> p = new Page<>(page, size);
        LambdaQueryWrapper<ProdPlot> wrapper = new LambdaQueryWrapper<ProdPlot>()
                .eq(orgId != null, ProdPlot::getOrgId, orgId)
                .orderByDesc(ProdPlot::getCreatedAt);
        return R.ok(plotMapper.selectPage(p, wrapper));
    }

    @Operation(summary = "新增地块")
    @PostMapping("/plot")
    public R<Void> addPlot(@RequestBody ProdPlot plot) {
        plot.setOrgId(getCurrentOrgId());
        plot.setCreatedBy(getCurrentUserId());
        plot.setCreatedAt(LocalDateTime.now());
        plotMapper.insert(plot);
        return R.success("新增成功");
    }

    @Operation(summary = "更新地块")
    @PutMapping("/plot/{id}")
    public R<Void> updatePlot(@PathVariable Long id, @RequestBody ProdPlot plot) {
        plot.setId(id);
        plot.setUpdatedBy(getCurrentUserId());
        plot.setUpdatedAt(LocalDateTime.now());
        plotMapper.updateById(plot);
        return R.success("更新成功");
    }

    @Operation(summary = "删除地块")
    @DeleteMapping("/plot/{id}")
    public R<Void> deletePlot(@PathVariable Long id) {
        plotMapper.deleteById(id);
        return R.ok();
    }

    // ============ 农事记录 ============

    @Operation(summary = "农事记录列表")
    @GetMapping("/farming-record/list")
    public R<?> farmingList(@RequestParam(defaultValue = "1") Integer page,
                            @RequestParam(defaultValue = "10") Integer size,
                            @RequestParam(required = false) Long plotId,
                            @RequestParam(required = false) String actionType,
                            @RequestParam(required = false) Integer status) {
        Long orgId = getCurrentOrgId();
        Page<ProdFarmingRecord> p = new Page<>(page, size);
        LambdaQueryWrapper<ProdFarmingRecord> wrapper = new LambdaQueryWrapper<ProdFarmingRecord>()
                .eq(orgId != null, ProdFarmingRecord::getOrgId, orgId)
                .eq(plotId != null, ProdFarmingRecord::getPlotId, plotId)
                .eq(actionType != null, ProdFarmingRecord::getActionType, actionType)
                .eq(status != null, ProdFarmingRecord::getStatus, status)
                .orderByDesc(ProdFarmingRecord::getActionDate);
        return R.ok(farmingMapper.selectPage(p, wrapper));
    }

    @Operation(summary = "新增农事记录")
    @PostMapping("/farming-record")
    public R<Void> addFarming(@RequestBody ProdFarmingRecord record) {
        record.setOrgId(getCurrentOrgId());
        record.setCreatedBy(getCurrentUserId());
        record.setCreatedAt(LocalDateTime.now());
        record.setStatus(0); // 草稿
        farmingMapper.insert(record);
        return R.success("保存成功");
    }

    @Operation(summary = "提交农事记录")
    @PostMapping("/farming-record/{id}/submit")
    public R<Void> submitFarming(@PathVariable Long id) {
        ProdFarmingRecord record = farmingMapper.selectById(id);
        if (record == null) throw BusinessException.of("记录不存在");
        if (record.getStatus() != 0) throw BusinessException.of("只有草稿状态可提交");
        record.setStatus(1);
        record.setUpdatedAt(LocalDateTime.now());
        farmingMapper.updateById(record);
        return R.success("提交成功");
    }

    @Operation(summary = "更新农事记录")
    @PutMapping("/farming-record/{id}")
    public R<Void> updateFarming(@PathVariable Long id, @RequestBody ProdFarmingRecord record) {
        ProdFarmingRecord old = farmingMapper.selectById(id);
        if (old == null) throw BusinessException.of("记录不存在");
        if (old.getStatus() > 0) throw BusinessException.of("已提交记录不可直接修改");
        record.setId(id);
        record.setUpdatedBy(getCurrentUserId());
        record.setUpdatedAt(LocalDateTime.now());
        farmingMapper.updateById(record);
        return R.success("更新成功");
    }

    @Operation(summary = "删除农事记录")
    @DeleteMapping("/farming-record/{id}")
    public R<Void> deleteFarming(@PathVariable Long id) {
        farmingMapper.deleteById(id);
        return R.ok();
    }

    // ============ 生产批次 ============

    @Operation(summary = "生产批次列表")
    @GetMapping("/batch/list")
    public R<Page<ProdBatch>> batchList(@RequestParam(defaultValue = "1") Integer page,
                                        @RequestParam(defaultValue = "10") Integer size,
                                        @RequestParam(required = false) Integer status) {
        Long orgId = getCurrentOrgId();
        Long userId = getCurrentUserId();
        com.tracegrid.sys.entity.SysUser user = userMapper.selectById(userId);
        
        Page<ProdBatch> p = new Page<>(page, size);
        LambdaQueryWrapper<ProdBatch> wrapper = new LambdaQueryWrapper<ProdBatch>();
        
        // 如果是生产主体（role=2），只能看自己的
        if (user.getRole() != null && user.getRole() == 2) {
            wrapper.eq(ProdBatch::getOrgId, orgId);
            wrapper.eq(status != null, ProdBatch::getStatus, status);
        } else {
            // 其他角色（如管理员 role=1、加工商 role=3）视角
            wrapper.eq(status != null, ProdBatch::getStatus, status);
            // 如果是管理员，还可以根据具体的 orgId 过滤（如果前端传了的话，此处暂不实现，默认为全局）
        }
        
        wrapper.orderByDesc(ProdBatch::getCreatedAt);
        return R.ok(batchMapper.selectPage(p, wrapper));
    }

    @Operation(summary = "新增生产批次")
    @PostMapping("/batch")
    public R<Void> addBatch(@RequestBody ProdBatch batch) {
        batch.setOrgId(getCurrentOrgId());
        batch.setCreatedBy(getCurrentUserId());
        batch.setCreatedAt(LocalDateTime.now());
        // 自动生成批次号
        if (batch.getBatchNo() == null || batch.getBatchNo().isBlank()) {
            batch.setBatchNo("PB-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                    + "-" + IdUtil.getSnowflakeNextIdStr().substring(10));
        }
        batch.setStatus(0);
        batchMapper.insert(batch);
        // 通知管理员
        messageService.send(1L, "新的生产批次待审核", 
            "产品【" + batch.getProductName() + "】的新批次已提交，请及时处理。", 1);
        return R.success("批次创建成功");
    }

    @Operation(summary = "生产批次详情")
    @GetMapping("/batch/{id}")
    public R<ProdBatch> batchDetail(@PathVariable Long id) {
        return R.ok(batchMapper.selectById(id));
    }

    @Operation(summary = "生产批次审核列表 (管理员用)")
    @GetMapping("/batch/audit-list")
    public R<?> batchAuditList(@RequestParam(defaultValue = "1") Integer page,
                               @RequestParam(defaultValue = "10") Integer size,
                               @RequestParam(required = false) Integer status) {
        SecurityUtils.requireAdmin(userMapper);
        Page<ProdBatch> p = new Page<>(page, size);
        LambdaQueryWrapper<ProdBatch> wrapper = new LambdaQueryWrapper<ProdBatch>()
                .eq(status != null, ProdBatch::getStatus, status)
                .orderByDesc(ProdBatch::getCreatedAt);
        batchMapper.selectPage(p, wrapper);
        enrichProdBatchAuditors(p.getRecords());
        return R.ok(p);
    }

    @Operation(summary = "审核生产批次")
    @PostMapping("/batch/{id}/audit")
    public R<Void> auditBatch(@PathVariable Long id, 
                             @RequestParam Integer status,
                             @RequestParam(required = false) String reason) {
        SecurityUtils.requireAdmin(userMapper);
        ProdBatch batch = batchMapper.selectById(id);
        if (batch == null) throw BusinessException.of("批次不存在");
        if (batch.getStatus() == null || batch.getStatus() != 0) {
            throw BusinessException.of("仅「待审核」状态的生产批次可审核");
        }
        if (status != 1 && status != 2) {
            throw BusinessException.of("审核结果参数无效");
        }
        if (status == 2 && (reason == null || reason.isBlank())) {
            throw BusinessException.of("驳回操作必须填写原因");
        }

        Long auditorId = getCurrentUserId();
        LocalDateTime now = LocalDateTime.now();
        batch.setStatus(status);
        if (status == 1) {
            batch.setRejectReason(null);
        } else {
            batch.setRejectReason(reason);
        }
        batch.setAuditBy(auditorId);
        batch.setAuditAt(now);
        batch.setUpdatedBy(auditorId);
        batch.setUpdatedAt(now);
        batchMapper.updateById(batch);
        String content = status == 1 ? "您的生产批次审核已通过。" : "您的生产批次审核被驳回，原因：" + reason;
        messageService.send(batch.getCreatedBy(), "生产批次审核结果", content, 1);

        return R.success("审核结果已下达");
    }

    @Operation(summary = "删除生产批次")
    @DeleteMapping("/batch/{id}")
    public R<Void> deleteBatch(@PathVariable Long id) {
        batchMapper.deleteById(id);
        return R.ok();
    }

    private void enrichProdBatchAuditors(List<ProdBatch> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        Set<Long> ids = new HashSet<>();
        for (ProdBatch row : records) {
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
        for (ProdBatch row : records) {
            if (row.getAuditBy() != null) {
                row.setAuditorName(names.get(row.getAuditBy()));
            }
        }
    }

    // ============ 工具方法 ============

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
}
