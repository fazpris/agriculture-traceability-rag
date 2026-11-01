package com.tracegrid.open.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tracegrid.common.exception.BusinessException;
import com.tracegrid.common.result.R;
import com.tracegrid.open.dto.OpenRagChatRequest;
import com.tracegrid.open.service.OpenRagProxyService;
import com.tracegrid.open.service.OpenTraceAggregateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 溯源详情页智能问答（代理至 RAG 服务，并附带只读溯源链路 JSON）
 */
@Tag(name = "公开查询")
@RestController
@RequestMapping("/open/rag")
@RequiredArgsConstructor
public class OpenRagController {

    private final OpenTraceAggregateService traceAggregateService;
    private final OpenRagProxyService ragProxyService;
    private final ObjectMapper objectMapper;

    @Operation(summary = "按当前追溯码向知识库提问（RAG）")
    @PostMapping("/chat")
    public R<Map<String, String>> chat(@Valid @RequestBody OpenRagChatRequest req) {
        String code = req.getProductCode().trim();
        Map<String, Object> tracePayload = traceAggregateService.aggregateReadOnly(code);
        final String traceJson;
        try {
            traceJson = objectMapper.writeValueAsString(tracePayload);
        } catch (JsonProcessingException e) {
            throw BusinessException.of(500, "溯源数据序列化失败");
        }

        String answer = ragProxyService.chat(
                code,
                req.getQuestion().trim(),
                req.getSessionId().trim(),
                traceJson);
        return R.ok(Map.of("answer", answer));
    }
}
