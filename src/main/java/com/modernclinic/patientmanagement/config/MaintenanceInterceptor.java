package com.modernclinic.patientmanagement.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class MaintenanceInterceptor implements HandlerInterceptor {

    @Value("${app.maintenance.enabled:false}")
    private boolean maintenanceEnabled;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!maintenanceEnabled) {
            return true;
        }

        String uri = request.getRequestURI();

        // Extra safeguard: Never redirect if already on maintenance, error, or static paths
        if (uri.startsWith("/maintenance") ||
                uri.startsWith("/error") ||
                uri.startsWith("/images") ||
                uri.startsWith("/css") ||
                uri.startsWith("/js") ||
                uri.equals("/favicon.ico")) {
            return true;
        }

        response.sendRedirect(request.getContextPath() + "/maintenance");
        return false;
    }
}