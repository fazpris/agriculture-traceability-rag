package com.tracegrid.sys.controller;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tracegrid.common.exception.BusinessException;
import com.tracegrid.common.result.R;
import com.tracegrid.common.security.SecurityUtils;
import com.tracegrid.sys.entity.SysOrg;
import com.tracegrid.sys.entity.SysRegisterApply;
import com.tracegrid.sys.entity.SysUser;
import com.tracegrid.sys.mapper.SysOrgMapper;
import com.tracegrid.sys.mapper.SysRegisterApplyMapper;
import com.tracegrid.sys.mapper.SysUserMapper;
import com.tracegrid.sys.service.SysMessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 注册申请管理
 */
@Tag(name = "注册审核")
@RestController
@RequestMapping("/sys/register")
@RequiredArgsConstructor
public class SysRegisterController {

    private final SysRegisterApplyMapper applyMapper;
    private final SysUserMapper userMapper;
    private final SysOrgMapper orgMapper;
    private final SysMessageService messageService;

    @Operation(summary = "提交注册申请（无需登录）")
    @PostMapping("/apply")
    public R<Void> apply(@RequestBody SysRegisterApply req) {
        // 检查用户名是否已存在
        Long count = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, req.getUsername()));
        if (count > 0) throw BusinessException.of("用户名已存在");
        if (req.getRole() != null && req.getRole() == 2) {
            req.setLicenseNo(null);
        }
        req.setPassword(BCrypt.hashpw(req.getPassword()));
        if (req.getApplyKind() == null) {
            req.setApplyKind(0);
        }
        req.setStatus(0);
        applyMapper.insert(req);
        // 通知管理员
        messageService.send(1L, "新的注册审核申请", 
            "主体【" + req.getOrgName() + "】提交了入驻申请，请及时审核。", 1);
        return R.success("申请提交成功，请等待审核");
    }

    @Operation(summary = "查询注册申请进度（无需登录）")
    @GetMapping("/query-status")
    public R<SysRegisterApply> queryStatus(@RequestParam String username, @RequestParam String phone) {
        SysRegisterApply apply = applyMapper.selectOne(new LambdaQueryWrapper<SysRegisterApply>()
                .eq(SysRegisterApply::getUsername, username)
                .eq(SysRegisterApply::getPhone, phone)
                .orderByDesc(SysRegisterApply::getCreatedAt)
                .last("limit 1"));
        if (apply == null) throw BusinessException.of("未找到相关的注册申请记录");
        
        // 仅返回安全相关字段
        SysRegisterApply result = new SysRegisterApply();
        result.setStatus(apply.getStatus());
        result.setRejectReason(apply.getRejectReason());
        result.setOrgName(apply.getOrgName());
        result.setCreatedAt(apply.getCreatedAt());
        return R.ok(result);
    }

    @Operation(summary = "已入驻主体提交认证资料变更（需登录）")
    @PostMapping("/cert-reapply")
    public R<Void> certReapply(@RequestBody CertReapplyReq body) {
        StpUtil.checkLogin();
        Long userId = Long.parseLong(StpUtil.getLoginIdAsString());
        SysUser user = userMapper.selectById(userId);
        if (user == null || user.getOrgId() == null) {
            throw BusinessException.of("当前账号未关联主体");
        }
        if (user.getRole() != null && user.getRole() == 1) {
            throw BusinessException.of("管理员请通过后台用户/主体管理维护资料");
        }
        SysOrg org = orgMapper.selectById(user.getOrgId());
        if (org == null) {
            throw BusinessException.of("主体不存在");
        }
        SysRegisterApply apply = new SysRegisterApply();
        apply.setApplyKind(1);
        apply.setTargetOrgId(user.getOrgId());
        apply.setUsername(user.getUsername());
        apply.setPassword(user.getPassword());
        apply.setRealName(body.getRealName() != null ? body.getRealName() : user.getRealName());
        apply.setPhone(body.getPhone() != null ? body.getPhone() : user.getPhone());
        apply.setRole(user.getRole());
        apply.setOrgName(body.getOrgName() != null ? body.getOrgName() : org.getName());
        apply.setOrgType(org.getType());
        apply.setOrgAddress(body.getOrgAddress() != null ? body.getOrgAddress() : org.getAddress());
        if (user.getRole() != null && user.getRole() == 2) {
            apply.setLicenseNo(null);
        } else {
            apply.setLicenseNo(body.getLicenseNo() != null ? body.getLicenseNo() : org.getLicenseNo());
        }
        apply.setStatus(0);
        applyMapper.insert(apply);
        messageService.send(1L, "主体认证资料变更待审",
                "用户【" + user.getUsername() + "】所属主体【" + apply.getOrgName() + "】提交了认证资料变更，请在注册审核中处理。", 1);
        return R.success("已提交审核，请等待管理员处理");
    }

    @Operation(summary = "查询注册申请列表（管理员）")
    @GetMapping("/list")
    public R<?> list(@RequestParam(defaultValue = "1") Integer page,
                     @RequestParam(defaultValue = "10") Integer size,
                     @RequestParam(required = false) Integer status) {
        SecurityUtils.requireAdmin(userMapper);
        Page<SysRegisterApply> p = new Page<>(page, size);
        LambdaQueryWrapper<SysRegisterApply> wrapper = new LambdaQueryWrapper<SysRegisterApply>()
                .orderByDesc(SysRegisterApply::getCreatedAt);
        if (status != null) wrapper.eq(SysRegisterApply::getStatus, status);
        return R.ok(applyMapper.selectPage(p, wrapper));
    }

    @Operation(summary = "审核通过")
    @PostMapping("/{id}/approve")
    public R<Void> approve(@PathVariable Long id) {
        SecurityUtils.requireAdmin(userMapper);
        return handleApprove(id);
    }

    private R<Void> handleApprove(Long id) {
        SysRegisterApply apply = applyMapper.selectById(id);
        if (apply == null || apply.getStatus() != 0)
            throw BusinessException.of("申请不存在或已审核");

        if (Integer.valueOf(1).equals(apply.getApplyKind())) {
            return approveCertReapply(apply);
        }

        Long usernameTaken = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, apply.getUsername()));
        if (usernameTaken != null && usernameTaken > 0) {
            throw BusinessException.of("用户名已被占用，无法通过审核");
        }

        // 创建主体
        SysOrg org = new SysOrg();
        org.setName(apply.getOrgName());
        org.setType(apply.getOrgType());
        org.setAddress(apply.getOrgAddress());
        // 生产主体（角色 2 / 主体类型 1）不强制营业执照号
        if (apply.getRole() != null && apply.getRole() == 2) {
            org.setLicenseNo(null);
        } else {
            org.setLicenseNo(apply.getLicenseNo());
        }
        org.setContact(apply.getRealName());
        org.setPhone(apply.getPhone());
        org.setStatus(1);
        orgMapper.insert(org);

        // 创建用户
        SysUser user = new SysUser();
        user.setUsername(apply.getUsername());
        user.setPassword(apply.getPassword());
        user.setRealName(apply.getRealName());
        user.setPhone(apply.getPhone());
        user.setRole(apply.getRole());
        user.setOrgId(org.getId());
        user.setStatus(1);
        userMapper.insert(user);

        // 更新申请状态
        apply.setStatus(1);
        apply.setAuditBy(Long.parseLong(StpUtil.getLoginIdAsString()));
        apply.setAuditAt(LocalDateTime.now());
        applyMapper.updateById(apply);

        // 发送通知给管理员 (TODO: 这里其实应该发给刚创建好的用户)
        messageService.send(user.getId(), "系统入驻成功", 
            "恭喜您，您的注册申请已通过审核。欢迎加入溯源平台！", 1);
            
        return R.success("审核通过");
    }

    private R<Void> approveCertReapply(SysRegisterApply apply) {
        SysUser existingUser = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, apply.getUsername()));
        if (existingUser == null) {
            throw BusinessException.of("关联用户不存在，无法通过认证变更");
        }
        if (apply.getTargetOrgId() == null || !apply.getTargetOrgId().equals(existingUser.getOrgId())) {
            throw BusinessException.of("认证变更申请与当前用户主体不一致");
        }
        SysOrg org = orgMapper.selectById(existingUser.getOrgId());
        if (org == null) {
            throw BusinessException.of("主体不存在");
        }
        org.setName(apply.getOrgName());
        org.setAddress(apply.getOrgAddress());
        if (apply.getRole() != null && apply.getRole() == 2) {
            org.setLicenseNo(null);
        } else {
            org.setLicenseNo(apply.getLicenseNo());
        }
        org.setContact(apply.getRealName());
        org.setPhone(apply.getPhone());
        orgMapper.updateById(org);

        existingUser.setRealName(apply.getRealName());
        existingUser.setPhone(apply.getPhone());
        userMapper.updateById(existingUser);

        apply.setStatus(1);
        apply.setAuditBy(Long.parseLong(StpUtil.getLoginIdAsString()));
        apply.setAuditAt(LocalDateTime.now());
        applyMapper.updateById(apply);

        messageService.send(existingUser.getId(), "认证资料更新已通过",
                "您提交的主体认证资料变更已通过审核，现已生效。", 1);
        return R.success("认证变更已通过");
    }

    @Operation(summary = "审核驳回")
    @PostMapping("/{id}/reject")
    public R<Void> reject(@PathVariable Long id, @RequestParam String reason) {
        SecurityUtils.requireAdmin(userMapper);
        if (reason == null || reason.trim().isEmpty()) throw BusinessException.of("评审驳回必须填写原因");
        return handleReject(id, reason);
    }

    private R<Void> handleReject(Long id, String reason) {
        SysRegisterApply apply = applyMapper.selectById(id);
        if (apply == null || apply.getStatus() != 0)
            throw BusinessException.of("申请不存在或已审核");
        apply.setStatus(2);
        apply.setRejectReason(reason);
        apply.setAuditBy(Long.parseLong(StpUtil.getLoginIdAsString()));
        apply.setAuditAt(LocalDateTime.now());
        applyMapper.updateById(apply);
        if (Integer.valueOf(1).equals(apply.getApplyKind())) {
            SysUser u = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                    .eq(SysUser::getUsername, apply.getUsername()));
            if (u != null) {
                messageService.send(u.getId(), "认证资料变更被驳回",
                        "原因：" + reason, 1);
            }
        }
        return R.success("已驳回");
    }

    @Operation(summary = "批量审核通过")
    @PostMapping("/batch-approve")
    public R<Map<String, Object>> batchApprove(@RequestBody List<Long> ids) {
        SecurityUtils.requireAdmin(userMapper);
        if (ids == null || ids.isEmpty()) {
            throw BusinessException.of("请选择至少一条申请");
        }
        int ok = 0;
        List<String> errors = new ArrayList<>();
        for (Long id : ids) {
            try {
                handleApprove(id);
                ok++;
            } catch (Exception e) {
                String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                errors.add("ID#" + id + "：" + msg);
            }
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("successCount", ok);
        body.put("failCount", errors.size());
        body.put("errors", errors);
        return R.ok("批量审核完成：成功 " + ok + "，失败 " + errors.size(), body);
    }

    @Operation(summary = "批量审核驳回")
    @PostMapping("/batch-reject")
    public R<Map<String, Object>> batchReject(@RequestBody BatchRejectRequest req) {
        SecurityUtils.requireAdmin(userMapper);
        if (req.getReason() == null || req.getReason().trim().isEmpty())
            throw BusinessException.of("评审驳回必须填写原因");
        if (req.getIds() == null || req.getIds().isEmpty()) {
            throw BusinessException.of("请选择至少一条申请");
        }
        int ok = 0;
        List<String> errors = new ArrayList<>();
        for (Long id : req.getIds()) {
            try {
                handleReject(id, req.getReason());
                ok++;
            } catch (Exception e) {
                String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                errors.add("ID#" + id + "：" + msg);
            }
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("successCount", ok);
        body.put("failCount", errors.size());
        body.put("errors", errors);
        return R.ok("批量驳回完成：成功 " + ok + "，失败 " + errors.size(), body);
    }

    @Data
    public static class CertReapplyReq {
        private String orgName;
        private String orgAddress;
        private String licenseNo;
        private String realName;
        private String phone;
    }

    @Data
    public static class BatchRejectRequest {
        private List<Long> ids;
        private String reason;
    }
}
