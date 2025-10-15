//package com.example.config;
//
//import org.springframework.context.annotation.Configuration;
//import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
//import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
//
//@Configuration
//public class InterceptorConfig implements WebMvcConfigurer {
//
//    @Override
//    public void addInterceptors(InterceptorRegistry registry) {
//        registry.addInterceptor(new JwtInterceptor())
//                .addPathPatterns("/**") // 拦截所有接口
//                .excludePathPatterns("/**"); // 放行登录、注册和/files接口
////                .excludePathPatterns("/login", "/register", "/files/**","/generate"); // 放行登录、注册和/files接口
//    }
//}
