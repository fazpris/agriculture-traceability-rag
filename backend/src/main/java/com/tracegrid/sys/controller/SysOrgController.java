package com.tracegrid.sys.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tracegrid.common.exception.BusinessException;
import com.tracegrid.common.result.R;
import com.tracegrid.sys.entity.SysOrg;
import com.tracegrid.sys.entity.SysUser;
import com.tracegrid.sys.mapper.SysOrgMapper;
import com.tracegrid.sys.mapper.SysUserMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 主体（组织）管理：加工-流通库存核销开关等
 */
@Tag(name = "主体管理")
@RestController
@RequestMapping("/sys/org")
@RequiredArgsConstructor
public class SysOrgController {

    private final SysOrgMapper orgMapper;
    private final SysUserMapper userMapper;

    private void requireAdmin() {
        Long uid = Long.parseLong(StpUtil.getLoginIdAsString());
        SysUser u = userMapper.selectById(uid);
        if (u == null || u.getRole() == null || u.getRole() != 1) {
            throw BusinessException.of("仅管理员可操作");
        }
    }

    @Operation(summary = "主体列表（管理员）")
    @GetMapping("/list")
    public R<?> list(@RequestParam(defaultValue = "1") Integer page,
                     @RequestParam(defaultValue = "50") Integer size) {
        requireAdmin();
        Page<SysOrg> p = new Page<>(page, size);
        return R.ok(orgMapper.selectPage(p,
                new LambdaQueryWrapper<SysOrg>().orderByDesc(SysOrg::getCreatedAt)));
    }

    @Operation(summary = "更新加工/流通库存核销开关（管理员）")
    @PutMapping("/{id}/inventory-link-flags")
    public R<Void> updateInventoryLinkFlags(@PathVariable Long id, @RequestBody InventoryLinkFlags body) {
        requireAdmin();
        SysOrg org = orgMapper.selectById(id);
        if (org == null) {
            throw BusinessException.of("主体不存在");
        }
        if (body.getProcDistLinkOutbound() != null) {
            if (org.getType() == null || org.getType() != 2) {
                throw BusinessException.of("「出库产生待核销」仅适用于加工类主体");
            }
            org.setProcDistLinkOutbound(body.getProcDistLinkOutbound() == 1 ? 1 : 0);
        }
        if (body.getProcDistLinkInbound() != null) {
            if (org.getType() == null || org.getType() != 3) {
                throw BusinessException.of("「入库须关联加工出库」仅适用于流通类主体");
            }
            org.setProcDistLinkInbound(body.getProcDistLinkInbound() == 1 ? 1 : 0);
        }
        orgMapper.updateById(org);
        return R.success("已保存");
    }

    @Operation(summary = "当前主体核销开关（加工/流通业务端自查）")
    @GetMapping("/my-inventory-link-flags")
    public R<Map<String, Object>> myInventoryLinkFlags() {
        Long uid = Long.parseLong(StpUtil.getLoginIdAsString());
        SysUser u = userMapper.selectById(uid);
        if (u == null || u.getOrgId() == null) {
            return R.ok(Map.of("procDistLinkOutbound", false, "procDistLinkInbound", false));
        }
        SysOrg org = orgMapper.selectById(u.getOrgId());
        Map<String, Object> m = new HashMap<>();
        m.put("orgType", org != null ? org.getType() : null);
        m.put("procDistLinkOutbound", org != null && org.getProcDistLinkOutbound() != null && org.getProcDistLinkOutbound() == 1);
        m.put("procDistLinkInbound", org != null && org.getProcDistLinkInbound() != null && org.getProcDistLinkInbound() == 1);
        return R.ok(m);
    }

    @Data
    public static class InventoryLinkFlags {
        /** 0 关闭 1 开启 */
        private Integer procDistLinkOutbound;
        private Integer procDistLinkInbound;
    }
}
