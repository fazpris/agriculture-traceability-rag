package com.tracegrid.trace.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 全平台溯源码生成规则（持久化到本地文件，管理端可配置）
 */
@Service
@RequiredArgsConstructor
public class TraceGenerationRuleService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${file.upload-path:./uploads}")
    private String uploadPath;

    private Path ruleFile() {
        return Paths.get(uploadPath).resolve("trace-generation-rules.json");
    }

    public TraceGenerationRules load() {
        Path p = ruleFile();
        try {
            if (Files.isRegularFile(p)) {
                return objectMapper.readValue(Files.readString(p, StandardCharsets.UTF_8), TraceGenerationRules.class);
            }
        } catch (IOException ignored) {
        }
        return new TraceGenerationRules();
    }

    public void save(TraceGenerationRules rules) throws IOException {
        Path p = ruleFile();
        Files.createDirectories(p.getParent());
        Files.writeString(p, objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(rules), StandardCharsets.UTF_8);
    }

    @Data
    public static class TraceGenerationRules {
        /** 码前缀，如 AG */
        private String prefix = "AG";
        /** 码中段，如 P001（与日期、流水等共同构成对外展示格式） */
        private String middleSegment = "P001";
        /** 业务端单次生成上限 */
        private int maxGeneratePerRequest = 500;
        /** 全平台单次生成上限（管理端若开放生成时使用，当前业务生成走业务端） */
        private int maxGenerateAdmin = 1000;
    }
}
