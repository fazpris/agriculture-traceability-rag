package com.tracegrid.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web 配置：CORS + 静态资源
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${file.upload-path:./uploads}")
    private String uploadPath;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 静态文件访问
        registry.addResourceHandler("/file/preview/**")
                .addResourceLocations(normalizeFileLocation(uploadPath));
    }

    private String normalizeFileLocation(String path) {
        String normalized = path.replace("\\", "/");
        if (!normalized.endsWith("/")) {
            normalized += "/";
        }
        return "file:" + normalized;
    }
}
