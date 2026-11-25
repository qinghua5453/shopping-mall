package com.shopmall.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopmall.common.ApiResponse;
import com.shopmall.common.BizException;
import com.shopmall.common.ErrorCode;
import com.shopmall.service.AdminSessionService;
import com.shopmall.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class LoginInterceptor implements HandlerInterceptor {
    public static final String ADMIN_ID_ATTRIBUTE = "adminId";
    public static final String SESSION_ID_ATTRIBUTE = "sessionId";

    private final JwtUtil jwtUtil;
    private final AdminSessionService sessionService;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            writeError(response, ErrorCode.UNAUTHORIZED);
            return false;
        }

        try {
            JwtUtil.AccessTokenClaims claims = jwtUtil.parse(authorization.substring(7));
            sessionService.validate(claims.sessionId(), claims.adminId());
            request.setAttribute(ADMIN_ID_ATTRIBUTE, claims.adminId());
            request.setAttribute(SESSION_ID_ATTRIBUTE, claims.sessionId());
            return true;
        } catch (BizException exception) {
            writeError(response, exception.getErrorCode());
            return false;
        }
    }

    private void writeError(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        response.setStatus(errorCode.getHttpStatus().value());
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), ApiResponse.error(errorCode));
    }
}
