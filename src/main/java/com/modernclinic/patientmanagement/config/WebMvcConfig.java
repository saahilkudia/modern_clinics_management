package com.modernclinic.patientmanagement.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private MaintenanceInterceptor maintenanceInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(maintenanceInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/maintenance",
                        "/maintenance/**",
                        "/error",
                        "/images/**",
                        "/css/**",
                        "/js/**",
                        "/favicon.ico"
                );
    }
}