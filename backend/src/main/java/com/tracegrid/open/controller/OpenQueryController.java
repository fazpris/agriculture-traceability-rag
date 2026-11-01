package com.tracegrid.open.controller;

import com.tracegrid.common.result.R;
import com.tracegrid.complaint.entity.ComplaintFeedback;
import com.tracegrid.complaint.mapper.ComplaintFeedbackMapper;
import com.tracegrid.open.service.OpenTraceAggregateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 公开查询接口（无需登录）
 */
@Tag(name = "公开查询")
@RestController
@RequestMapping("/open")
@RequiredArgsConstructor
public class OpenQueryController {

    private final OpenTraceAggregateService traceAggregateService;
    private final ComplaintFeedbackMapper complaintMapper;
    private final com.tracegrid.sys.service.SysMessageService messageService;

    @Operation(summary = "按追溯码查询全链路溯源信息")
    @GetMapping("/query")
    public R<Map<String, Object>> query(@RequestParam String code) {
        return R.ok(traceAggregateService.aggregateWithScanUpdate(code));
    }

    @Operation(summary = "提交消费者投诉")
    @PostMapping("/complaint")
    public R<Void> complaint(@RequestBody ComplaintFeedback req) {
        req.setStatus(0);
        req.setCreatedAt(LocalDateTime.now());
        complaintMapper.insert(req);
        // 通知管理员
        messageService.send(1L, "接收到新的消费者投诉",
            "接到关于追溯码【" + req.getTraceCode() + "】的新投诉，请及时处理。", 2);
        return R.success("投诉已提交，感谢您的反馈");
    }
}
