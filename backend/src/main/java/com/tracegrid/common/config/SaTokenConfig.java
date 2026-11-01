package com.tracegrid.common.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sa-Token 路由拦截配置
 */
@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册 Sa-Token 路由拦截器
        registry.addInterceptor(new SaInterceptor(handle -> {
            // 设置路由权限校验规则
            SaRouter.match("/**")
                    .notMatch("/auth/login", "/sys/register/apply", "/sys/register/check-username", "/proc/raw-inbound")
                    .notMatch("/sys/stats/full-metrics")
                    .notMatch("/open/**")
                    .notMatch("/file/preview/**")
                    .notMatch("/doc.html", "/swagger-ui/**", "/v3/api-docs/**", "/favicon.ico")
                    .check(r -> StpUtil.checkLogin());
        })).addPathPatterns("/**");
    }
}
