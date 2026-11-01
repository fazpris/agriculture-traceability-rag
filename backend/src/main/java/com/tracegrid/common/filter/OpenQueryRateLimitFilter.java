package com.tracegrid.common.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tracegrid.common.result.R;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 公开溯源查询限流，防止爬虫/误操作拖垮数据库。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
@RequiredArgsConstructor
public class OpenQueryRateLimitFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;

    @Value("${tracegrid.open-query.rate-limit-per-minute:120}")
    private int maxPerMinute;

    private final Cache<String, AtomicInteger> counters = Caffeine.newBuilder()
            .maximumSize(20_000)
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .build();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return !uri.endsWith("/open/query");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String ip = resolveClientIp(request);
        AtomicInteger counter = counters.get(ip, k -> new AtomicInteger(0));
        if (counter.incrementAndGet() > maxPerMinute) {
            response.setStatus(429);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(objectMapper.writeValueAsString(
                    R.fail(429, "查询过于频繁，请稍后再试")));
            return;
        }
        chain.doFilter(request, response);
    }

    private static String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }
}
