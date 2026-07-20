package com.roze.trackeyecentral.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Guards the dashboard endpoints (/api/admin/** and /api/screenshots/**).
 *
 * Before this existed, those controllers read @RequestAttribute Long organizationId
 * but NOTHING ever set that attribute for a logged-in web user - so the dashboard
 * could never load. This filter reads the "Authorization: Bearer <token>" header,
 * validates the signature (TokenService), and populates:
 *      organizationId, userId, role
 * which every /api/admin/** and /api/screenshots/** controller relies on.
 *
 * It also enforces that admin-only actions require an admin/supervisor role.
 */
@Slf4j
@Component
@Order(2)
@RequiredArgsConstructor
public class UserAuthenticationFilter extends OncePerRequestFilter {

    private final TokenService tokenService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String auth = request.getHeader("Authorization");
        TokenService.Claims claims = tokenService.verify(auth);

        if (claims == null) {
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "Missing or invalid session token");
            return;
        }

        // Role gate: write actions under /api/admin require admin or supervisor.
        String path = request.getRequestURI();
        String method = request.getMethod();
        boolean writeAdminAction = path.contains("/api/admin/")
                && (method.equals("POST") || method.equals("PUT") || method.equals("DELETE"));
        if (writeAdminAction && !isManager(claims.getRole())) {
            sendError(response, HttpServletResponse.SC_FORBIDDEN, "Insufficient permissions");
            return;
        }

        request.setAttribute("organizationId", claims.getOid());
        request.setAttribute("userId", claims.getUid());
        request.setAttribute("role", claims.getRole());

        filterChain.doFilter(request, response);
    }

    private boolean isManager(String role) {
        return "admin".equalsIgnoreCase(role) || "supervisor".equalsIgnoreCase(role);
    }

    /** Only run this filter on dashboard endpoints. */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (request.getMethod().equals("OPTIONS")) return true;
        boolean isDashboard = path.startsWith("/api/admin/") || path.startsWith("/api/screenshots/");
        return !isDashboard;
    }

    private void sendError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        Map<String, String> error = new HashMap<>();
        error.put("error", message);
        error.put("timestamp", String.valueOf(System.currentTimeMillis()));
        response.getWriter().write(objectMapper.writeValueAsString(error));
    }
}
