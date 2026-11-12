package com.tyut.studyroombookingcheckin.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import com.tyut.studyroombookingcheckin.common.Result;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.jsonwebtoken.Claims;

/**
 * 统一鉴权拦截器。
 * <p>
 * 规则：
 * 1. 未携带 Token 时，仅放行 GET 方式的楼栋/房间/座位查询（游客浏览），其余一律 401；
 * 2. 携带 Token 时解析并写入 SecurityContext；
 * 3. 处理器上标注 {@link RequireAdmin}（类或方法）时，要求 role=1，否则 403。
 */
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
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String token = request.getHeader(header);
        if (token != null && token.startsWith(prefix)) {
            token = token.substring(prefix.length());
        }

        if (token == null || token.isEmpty()) {
            if (isGuestReadable(request)) {
                return true;
            }
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
        } catch (Exception e) {
            responseError(response, 401, "无效的token");
            return false;
        }

        if (requiresAdmin(handler) && !Integer.valueOf(1).equals(SecurityContextHolder.getRole())) {
            responseError(response, 403, "无权限访问，仅管理员可操作");
            return false;
        }
        return true;
    }

    /** 游客可匿名访问：楼栋/房间/座位的 GET 查询。 */
    private boolean isGuestReadable(HttpServletRequest request) {
        if (!"GET".equalsIgnoreCase(request.getMethod())) {
            return false;
        }
        String path = request.getServletPath();
        return path.startsWith("/buildings") || path.startsWith("/rooms") || path.startsWith("/seats");
    }

    private boolean requiresAdmin(Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return false;
        }
        return handlerMethod.hasMethodAnnotation(RequireAdmin.class)
                || handlerMethod.getBeanType().isAnnotationPresent(RequireAdmin.class);
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        SecurityContextHolder.clear();
    }

    private void responseError(HttpServletResponse response, Integer code, String message) throws Exception {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(code == 403 ? HttpServletResponse.SC_FORBIDDEN : HttpServletResponse.SC_UNAUTHORIZED);
        Result<?> result = Result.error(code, message);
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }
}
