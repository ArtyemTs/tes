package com.tes.api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                // для дев-режима можно так:
                .allowedOriginPatterns("http://localhost:5173", "http://127.0.0.1:5173", "http://192.168.31.207:5173", "http://10.128.229.65:5173")
                // или вообще .allowedOriginPatterns("*") ТОЛЬКО для девелопмента
                .allowedMethods("GET","POST","PUT","DELETE","OPTIONS")
                .allowedHeaders("*");
    }
}