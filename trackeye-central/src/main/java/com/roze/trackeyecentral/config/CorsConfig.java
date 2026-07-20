package com.roze.trackeyecentral.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * The central server had NO CORS configuration, so a browser-based frontend
 * (the Next.js dashboard) served from a different origin would be blocked from
 * calling the API. This enables it.
 *
 * Origins are read from app.frontend-origins so you can add your LAN address
 * without recompiling, e.g.:
 *   app.frontend-origins=http://localhost:3000,http://192.168.1.50:3000
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Value("${app.frontend-origins:http://localhost:3000}")
    private String[] allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Authorization")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
