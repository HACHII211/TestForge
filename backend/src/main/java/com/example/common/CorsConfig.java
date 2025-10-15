package com.example.common;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;

/**
 * 跨域配置
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration corsConfiguration = new CorsConfiguration();
        corsConfiguration.setAllowCredentials(true);  // 允许携带 Cookie
        corsConfiguration.setAllowedOrigins(Arrays.asList("https://3a86e67c.r22.cpolar.top/","http://localhost:5173/"));
        corsConfiguration.addAllowedHeader("*"); // 2设置访问源请求头
        corsConfiguration.addExposedHeader("Authorization");
        corsConfiguration.addAllowedMethod("*"); // 3设置访问源请求方法
        source.registerCorsConfiguration("/**", corsConfiguration); // 4对接口配置跨域设置
        return new CorsFilter(source);
    }
}