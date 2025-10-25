// src/main/java/com/example/config/RestConfig.java
package com.example.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestConfig {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder, ObjectMapper objectMapper) {
        RestTemplate rest = builder.build();
        // 用你的 ObjectMapper 定制 JSON 解析（可选）
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        converter.setObjectMapper(objectMapper);
        rest.getMessageConverters().removeIf(c -> c instanceof MappingJackson2HttpMessageConverter);
        rest.getMessageConverters().add(converter);
        return rest;
    }
}
