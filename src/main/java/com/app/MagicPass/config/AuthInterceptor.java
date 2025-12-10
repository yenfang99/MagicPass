package com.app.MagicPass.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String path = request.getRequestURI();

        // Public endpoints/resources that do not require authentication
        if (isPublicPath(path)) {
            return true;
        }

        HttpSession session = request.getSession(false);
        boolean isUser = session != null && session.getAttribute("currentUser") != null;
        boolean isStaff = session != null && session.getAttribute("currentStaff") != null;
        boolean loggedIn = isUser || isStaff;

        if (!loggedIn) {
            response.sendRedirect("/login");
            return false;
        }

        // Require staff permissions for admin/staff routes
        if (path.startsWith("/admin") || path.startsWith("/staff")) {
            if (!isStaff) {
                response.sendRedirect("/login");
                return false;
            }
        }

        return true;
    }

    private boolean isPublicPath(String path) {
        return path.equals("/")
                || path.equals("/login")
                || path.equals("/register")
                || path.equals("/logout")
                || path.startsWith("/css/")
                || path.startsWith("/js/")
                || path.startsWith("/images/")
                || path.startsWith("/webjars/")
                || path.equals("/favicon.ico")
                || path.equals("/error");
    }
}
