// src/main/java/com/trackeye/security/ApiKeyAuthenticationFilter.java
package com.roze.trackeyecentral.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private final ApiKeyService apiKeyService;
    private final RateLimiterService rateLimiterService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();

        // Public endpoints - no authentication required
        if (isPublicEndpoint(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = getClientIp(request);

        // Rate limiting
        if (rateLimiterService.isRateLimited(clientIp)) {
            sendErrorResponse(response, 429,
                "Rate limit exceeded. Please try again later.");
            return;
        }

        // Extract API Key and Device ID
        String apiKey = request.getHeader("X-API-Key");
        String deviceId = request.getHeader("X-Device-ID");

        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("Missing API Key from IP: {}", clientIp);
            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "Missing API Key");
            return;
        }

        if (deviceId == null || deviceId.isEmpty()) {
            log.warn("Missing Device ID from IP: {}", clientIp);
            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "Missing Device ID");
            return;
        }

        // Validate API Key
        ApiKeyService.ApiKeyValidationResult validation = apiKeyService.validateApiKey(apiKey, deviceId);

        if (!validation.isValid()) {
            log.warn("Invalid API Key/Device combination: {} from IP: {}", validation.getErrorMessage(), clientIp);
            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, validation.getErrorMessage());
            return;
        }

        // Set attributes for downstream use
        request.setAttribute("organizationId", validation.getOrganizationId());
        request.setAttribute("userId", validation.getUserId());
        request.setAttribute("deviceId", validation.getDeviceId());

        // Log successful authentication
        log.debug("Authenticated request: deviceId={}, userId={}, path={}",
            deviceId, validation.getUserId(), path);

        filterChain.doFilter(request, response);
    }

    private boolean isPublicEndpoint(String path) {
        return path.startsWith("/api/public/") ||
               path.startsWith("/actuator/health") ||
               path.equals("/health") ||
               path.equals("/") ||
               path.startsWith("/h2-console");
    }

    private void sendErrorResponse(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        Map<String, String> error = new HashMap<>();
        error.put("error", message);
        error.put("timestamp", String.valueOf(System.currentTimeMillis()));
        response.getWriter().write(objectMapper.writeValueAsString(error));
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }
}