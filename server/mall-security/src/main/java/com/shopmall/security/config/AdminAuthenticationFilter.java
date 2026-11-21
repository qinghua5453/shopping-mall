package com.shopmall.security.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopmall.common.api.ApiResponse;
import com.shopmall.common.error.BizException;
import com.shopmall.common.error.ErrorCode;
import com.shopmall.security.application.service.AdminAccountService;
import com.shopmall.security.application.service.AdminPermissionService;
import com.shopmall.security.application.service.AdminSessionService;
import com.shopmall.security.application.service.TokenService;
import com.shopmall.security.domain.model.AccessTokenClaims;
import com.shopmall.security.domain.model.AdminAccess;
import com.shopmall.security.domain.model.AdminPrincipal;
import com.shopmall.security.infrastructure.persistence.entity.SysAdminEntity;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminAuthenticationFilter extends OncePerRequestFilter {
    private final TokenService tokenService;
    private final AdminSessionService sessionService;
    private final AdminAccountService accountService;
    private final AdminPermissionService permissionService;
    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !path.startsWith("/api/v1/admin/")
                || path.equals("/api/v1/admin/auth/login")
                || path.equals("/api/v1/admin/auth/refresh")
                || "OPTIONS".equalsIgnoreCase(request.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            AccessTokenClaims claims = tokenService.parseAccessToken(authorization.substring(7));
            sessionService.validateSession(claims.adminId(), claims.sessionId());
            SysAdminEntity admin = accountService.getActiveById(claims.adminId());
            AdminAccess access = permissionService.getAccess(claims.adminId());
            AdminPrincipal principal = new AdminPrincipal(
                    claims.adminId(), admin.getUsername(), claims.sessionId());
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    principal,
                    null,
                    access.permissions().stream().map(SimpleGrantedAuthority::new).toList());
            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);
        } catch (BizException exception) {
            SecurityContextHolder.clearContext();
            writeError(response, exception.getErrorCode());
        } catch (Exception exception) {
            SecurityContextHolder.clearContext();
            log.error("管理端认证过滤器异常", exception);
            writeError(response, ErrorCode.SYSTEM_ERROR);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private void writeError(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        response.setStatus(errorCode.getHttpStatus().value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), ApiResponse.error(errorCode));
    }
}
