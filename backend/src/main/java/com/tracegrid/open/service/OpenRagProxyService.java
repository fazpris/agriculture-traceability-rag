package com.tracegrid.open.service;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.tracegrid.common.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 将公开溯源页的问答请求转发到独立 RAG 服务（FastAPI app_api）。
 */
@Service
public class OpenRagProxyService {

    @Value("${tracegrid.rag.base-url:http://127.0.0.1:8000}")
    private String ragBaseUrl;

    public String chat(String productCode, String question, String sessionId, String traceContextJson) {
        String url = ragBaseUrl.replaceAll("/$", "") + "/chat";
        JSONObject body = JSONUtil.createObj()
                .set("product_code", productCode)
                .set("question", question)
                .set("session_id", sessionId);
        if (traceContextJson != null && !traceContextJson.isBlank()) {
            body.set("trace_context", traceContextJson);
        }

        try (HttpResponse resp = HttpRequest.post(url)
                .header("Content-Type", "application/json")
                .body(body.toString())
                .timeout(120_000)
                .execute()) {
            if (!resp.isOk()) {
                String err = tryParseDetail(resp.body());
                throw BusinessException.of(resp.getStatus(), err != null ? err : "智能问答服务暂时不可用");
            }
            JSONObject json = JSONUtil.parseObj(resp.body());
            String answer = json.getStr("answer");
            if (answer == null || answer.isBlank()) {
                throw BusinessException.of(502, "智能问答返回格式异常");
            }
            return answer;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            String tail = e.getMessage() != null ? "（" + e.getMessage() + "）" : "";
            throw BusinessException.of(503, "无法连接 RAG 服务或网络异常，请确认进程已监听 tracegrid.rag.base-url" + tail);
        }
    }

    private static String tryParseDetail(String body) {
        if (body == null || body.isBlank()) {
            return null;
        }
        try {
            JSONObject o = JSONUtil.parseObj(body);
            Object detail = o.get("detail");
            if (detail == null) {
                return null;
            }
            if (detail instanceof String s) {
                return s;
            }
            return detail.toString();
        } catch (Exception ignored) {
            return null;
        }
    }
}
