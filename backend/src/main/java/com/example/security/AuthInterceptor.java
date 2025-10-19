package com.example.security;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.List;

public class AuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    public static final String ATTR_USER_ID = "CURRENT_USER_ID";
    public static final String ATTR_ROLE_IDS = "CURRENT_ROLE_IDS";

    public AuthInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String path = request.getRequestURI();

        if (path.startsWith("/auth/login") || path.startsWith("/auth/register")) {
            return true;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"message\":\"Missing Authorization header\"}");
            return false;
        }

        String token = authHeader.substring(7);
        try {
            DecodedJWT jwt = jwtUtil.verifyToken(token);
            Long userId = jwtUtil.getUserId(jwt);
            List<Long> roleIds = jwtUtil.getRoleIds(jwt);

            if (userId == null) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("{\"message\":\"Invalid token: missing userId\"}");
                return false;
            }

            request.setAttribute(ATTR_USER_ID, userId);
            request.setAttribute(ATTR_ROLE_IDS, roleIds);
            return true;
        } catch (JWTVerificationException e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"message\":\"Token invalid or expired\"}");
            return false;
        }
    }

}
