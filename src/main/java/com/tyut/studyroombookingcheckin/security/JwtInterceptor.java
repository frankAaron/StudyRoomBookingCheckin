package com.tyut.studyroombookingcheckin.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.tyut.studyroombookingcheckin.common.Result;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.jsonwebtoken.Claims;

@Component
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    @Value("${jwt.header}")
    private String header;

    @Value("${jwt.prefix}")
    private String prefix;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String token = request.getHeader(header);
        if (token != null && token.startsWith(prefix)) {
            token = token.substring(prefix.length());
        }
        if (token == null || token.isEmpty()) {
            responseError(response, 401, "未授权，请先登录");
            return false;
        }
        try {
            if (jwtUtil.isExpired(token)) {
                responseError(response, 401, "登录已过期");
                return false;
            }
            Claims claims = jwtUtil.parseToken(token);
            SecurityContextHolder.setUserId(claims.get("userId", Long.class));
            SecurityContextHolder.setUsername(claims.get("username", String.class));
            SecurityContextHolder.setRole(claims.get("role", Integer.class));
            return true;
        } catch (Exception e) {
            responseError(response, 401, "无效的token");
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        SecurityContextHolder.clear();
    }

    private void responseError(HttpServletResponse response, Integer code, String message) throws Exception {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        Result<?> result = Result.error(code, message);
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }
}