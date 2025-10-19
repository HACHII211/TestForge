// WebConfig.java
package com.example.config;

import com.example.security.AuthInterceptor;
import com.example.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new AuthInterceptor(jwtUtil))
                .addPathPatterns("/**")          // 根据你的接口路径调整
                .excludePathPatterns("/auth/**");    // 登陆/公开接口不拦截
    }
}
