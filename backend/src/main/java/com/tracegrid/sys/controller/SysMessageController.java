package com.tracegrid.sys.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tracegrid.common.result.R;
import com.tracegrid.sys.entity.SysMessage;
import com.tracegrid.sys.service.SysMessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 系统消息控制器
 */
@Tag(name = "消息中心")
@RestController
@RequestMapping("/sys/message")
@RequiredArgsConstructor
public class SysMessageController {

    private final SysMessageService messageService;

    @Operation(summary = "获取当前用户消息列表")
    @GetMapping("/list")
    public R<Page<SysMessage>> list(@RequestParam(defaultValue = "1") Integer page,
                                   @RequestParam(defaultValue = "10") Integer size,
                                   @RequestParam(required = false) Integer status) {
        Long userId = StpUtil.getLoginIdAsLong();
        return R.ok(messageService.listMessages(userId, page, size, status));
    }

    @Operation(summary = "标记消息为已读")
    @PostMapping("/{id}/read")
    public R<Void> markRead(@PathVariable Long id) {
        messageService.markAsRead(id);
        return R.success("消息已读");
    }

    @Operation(summary = "获取未读消息总数")
    @GetMapping("/unread-count")
    public R<Long> getUnreadCount() {
        return R.ok(messageService.getUnreadCount(StpUtil.getLoginIdAsLong()));
    }
}
