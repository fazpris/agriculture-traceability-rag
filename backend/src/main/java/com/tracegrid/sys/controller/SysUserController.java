package com.tracegrid.sys.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tracegrid.common.result.R;
import com.tracegrid.sys.entity.SysUser;
import com.tracegrid.sys.mapper.SysUserMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

/**
 * 用户管理接口
 */
@Tag(name = "用户管理")
@RestController
@RequestMapping("/sys/user")
@RequiredArgsConstructor
public class SysUserController {

    private final SysUserMapper userMapper;

    @Operation(summary = "用户列表")
    @GetMapping("/list")
    public R<?> list(@RequestParam(defaultValue = "1") Integer page,
                     @RequestParam(defaultValue = "10") Integer size,
                     @RequestParam(required = false) String username,
                     @RequestParam(required = false) Integer role,
                     @RequestParam(required = false) Integer status) {
        Page<SysUser> p = new Page<>(page, size);
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
                .like(username != null, SysUser::getUsername, username)
                .eq(role != null, SysUser::getRole, role)
                .eq(status != null, SysUser::getStatus, status)
                .orderByDesc(SysUser::getCreatedAt);
        return R.ok(userMapper.selectPage(p, wrapper));
    }

    @Operation(summary = "禁用/启用用户")
    @PostMapping("/{id}/toggle-status")
    public R<Void> toggleStatus(@PathVariable Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) return R.fail("用户不存在");
        user.setStatus(user.getStatus() == 1 ? 0 : 1);
        userMapper.updateById(user);
        return R.ok();
    }

    @Operation(summary = "重置密码")
    @PostMapping("/{id}/reset-password")
    public R<Void> resetPassword(@PathVariable Long id, @RequestBody java.util.Map<String, String> params) {
        String newPassword = params.get("password");
        if (newPassword == null || newPassword.isEmpty()) return R.fail("新密码不能为空");
        
        SysUser user = userMapper.selectById(id);
        if (user == null) return R.fail("用户不存在");
        
        user.setPassword(cn.hutool.crypto.digest.BCrypt.hashpw(newPassword));
        userMapper.updateById(user);
        return R.success("密码重置成功");
    }

    @Operation(summary = "新增/更新用户")
    @PostMapping("/save")
    public R<Void> save(@RequestBody SysUser user) {
        if (user.getId() == null) {
            // 新增
            if (user.getPassword() == null) user.setPassword("123456"); // 默认密码
            user.setPassword(cn.hutool.crypto.digest.BCrypt.hashpw(user.getPassword()));
            user.setStatus(1);
            userMapper.insert(user);
        } else {
            // 更新（不更新密码）
            SysUser oldUser = userMapper.selectById(user.getId());
            if (oldUser == null) return R.fail("用户不存在");
            user.setPassword(null); // 禁止在此接口改密码
            userMapper.updateById(user);
        }
        return R.ok();
    }

    @Operation(summary = "删除用户")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        userMapper.deleteById(id);
        return R.ok();
    }

    @Operation(summary = "获取服务器局域网IP")
    @GetMapping("/server-ip")
    public R<String> getServerIp() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                // 排除回调接口、虚拟接口和未启动的接口
                if (iface.isLoopback() || iface.isVirtual() || !iface.isUp()) continue;

                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (addr instanceof Inet4Address) {
                        return R.ok(addr.getHostAddress());
                    }
                }
            }
            return R.ok(InetAddress.getLocalHost().getHostAddress());
        } catch (Exception e) {
            return R.ok("127.0.0.1");
        }
    }
}
