package com.tracegrid.common.security;

import cn.dev33.satoken.stp.StpUtil;
import com.tracegrid.common.exception.BusinessException;
import com.tracegrid.sys.entity.SysUser;
import com.tracegrid.sys.mapper.SysUserMapper;

/**
 * 登录态与角色校验（审核等敏感操作）
 */
public final class SecurityUtils {

    private SecurityUtils() {}

    /** 平台管理员 role = 1 */
    public static void requireAdmin(SysUserMapper userMapper) {
        Long uid = Long.parseLong(StpUtil.getLoginIdAsString());
        SysUser u = userMapper.selectById(uid);
        if (u == null || u.getRole() == null || u.getRole() != 1) {
            throw BusinessException.of("仅管理员可进行审核相关操作");
        }
    }
}
