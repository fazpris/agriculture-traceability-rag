package com.tracegrid.complaint.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tracegrid.common.result.R;
import com.tracegrid.complaint.entity.ComplaintFeedback;
import com.tracegrid.complaint.mapper.ComplaintFeedbackMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 投诉反馈管理（后台）
 */
@Tag(name = "投诉反馈管理")
@RestController
@RequestMapping("/sys/complaint")
@RequiredArgsConstructor
public class ComplaintController {

    private final ComplaintFeedbackMapper complaintMapper;

    @Operation(summary = "投诉列表")
    @GetMapping("/list")
    public R<Page<ComplaintFeedback>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String traceCode,
            @RequestParam(required = false) Integer status) {
        
        Page<ComplaintFeedback> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<ComplaintFeedback> wrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.hasText(traceCode)) {
            wrapper.eq(ComplaintFeedback::getTraceCode, traceCode);
        }
        if (status != null) {
            wrapper.eq(ComplaintFeedback::getStatus, status);
        }
        wrapper.orderByDesc(ComplaintFeedback::getCreatedAt);
        
        return R.ok(complaintMapper.selectPage(pageParam, wrapper));
    }

    @Operation(summary = "处理投诉")
    @PostMapping("/{id}/handle")
    public R<Void> handle(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        ComplaintFeedback complaint = complaintMapper.selectById(id);
        if (complaint == null) return R.fail("记录不存在");
        
        String result = (String) params.get("result");
        Integer status = (Integer) params.get("status");
        
        complaint.setHandleResult(result);
        complaint.setStatus(status != null ? status : 2); // 默认设为已处理
        complaint.setHandledAt(LocalDateTime.now());
        // 此处获取登录用户 ID，由于已禁用鉴权，默认用 1
        complaint.setHandleBy(1L);
        
        complaintMapper.updateById(complaint);
        return R.success("处理完成");
    }
}
