package com.tracegrid.sys.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tracegrid.sys.entity.SysMessage;
import com.tracegrid.sys.mapper.SysMessageMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SysMessageService {

    private final SysMessageMapper messageMapper;

    /**
     * 发送系统消息
     */
    public void send(Long userId, String title, String content, Integer type) {
        SysMessage msg = new SysMessage();
        msg.setUserId(userId);
        msg.setTitle(title);
        msg.setContent(content);
        msg.setType(type);
        msg.setStatus(0);
        msg.setCreatedAt(LocalDateTime.now());
        messageMapper.insert(msg);
    }

    /**
     * 分页查询用户消息
     */
    public Page<SysMessage> listMessages(Long userId, Integer page, Integer size, Integer status) {
        Page<SysMessage> p = new Page<>(page, size);
        LambdaQueryWrapper<SysMessage> wrapper = new LambdaQueryWrapper<SysMessage>()
                .eq(SysMessage::getUserId, userId)
                .orderByDesc(SysMessage::getCreatedAt);
        if (status != null) {
            wrapper.eq(SysMessage::getStatus, status);
        }
        return messageMapper.selectPage(p, wrapper);
    }

    /**
     * 标记为已读
     */
    public void markAsRead(Long id) {
        SysMessage msg = new SysMessage();
        msg.setId(id);
        msg.setStatus(1);
        messageMapper.updateById(msg);
    }

    /**
     * 获取未读消息数
     */
    public Long getUnreadCount(Long userId) {
        return messageMapper.selectCount(new LambdaQueryWrapper<SysMessage>()
                .eq(SysMessage::getUserId, userId)
                .eq(SysMessage::getStatus, 0));
    }
}
