package com.eldercare.security;

import com.eldercare.common.Result;
import com.eldercare.common.ResultCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 首次登录强制改密：以数据库加载到 LoginUser 的 mustChangePassword 为准（每次 JWT 鉴权都会查库）。
 * 非白名单业务 API 一律 403。
 */
@Component
public class MustChangePasswordFilter extends OncePerRequestFilter {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    /** method + pattern；登录本身 permitAll，此处主要限制已认证用户 */
    private static final List<String> ALLOWED_GET = List.of(
            "/api/system/users/me",
            "/api/auth/me"
    );
    private static final List<String> ALLOWED_POST = List.of(
            "/api/system/users/change-password",
            "/api/auth/login"
    );

    private final ObjectMapper objectMapper;

    public MustChangePasswordFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
                && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof LoginUser loginUser
                && loginUser.isMustChangePassword()
                && !isAllowed(request)) {
            writeForbidden(response);
            return;
        }
        filterChain.doFilter(request, response);
    }

    private boolean isAllowed(HttpServletRequest request) {
        String method = request.getMethod();
        String path = request.getRequestURI();
        // 去掉 context-path（本项目为空）后的 servlet path 更稳妥
        String servletPath = request.getServletPath();
        if (servletPath != null && !servletPath.isBlank()) {
            path = servletPath;
        }
        if ("GET".equalsIgnoreCase(method)) {
            return matchesAny(path, ALLOWED_GET);
        }
        if ("POST".equalsIgnoreCase(method)) {
            return matchesAny(path, ALLOWED_POST);
        }
        return false;
    }

    private boolean matchesAny(String path, List<String> patterns) {
        for (String pattern : patterns) {
            if (PATH_MATCHER.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }

    private void writeForbidden(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(),
                Result.fail(ResultCode.FORBIDDEN, "首次登录请先修改密码"));
    }
}
