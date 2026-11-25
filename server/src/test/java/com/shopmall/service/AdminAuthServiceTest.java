package com.shopmall.service;

import com.shopmall.common.BizException;
import com.shopmall.common.ErrorCode;
import com.shopmall.config.AuthProperties;
import com.shopmall.dto.LoginRequest;
import com.shopmall.dto.LoginResponse;
import com.shopmall.entity.SysAdmin;
import com.shopmall.mapper.SysAdminMapper;
import com.shopmall.mapper.SysPermissionMapper;
import com.shopmall.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminAuthServiceTest {
    @Mock
    private SysAdminMapper adminMapper;
    @Mock
    private SysPermissionMapper permissionMapper;
    @Mock
    private AdminSessionService sessionService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtUtil jwtUtil;

    private AdminAuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AdminAuthService(
                adminMapper,
                permissionMapper,
                sessionService,
                passwordEncoder,
                jwtUtil,
                new AuthProperties("test-secret-key-with-at-least-32-bytes", Duration.ofMinutes(30))
        );
    }

    @Test
    void shouldRejectWrongPassword() {
        SysAdmin admin = activeAdmin();
        when(adminMapper.selectByUsername("admin")).thenReturn(admin);
        when(passwordEncoder.matches("wrong-password", "encoded-password")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(loginRequest("wrong-password"), "127.0.0.1"))
                .isInstanceOf(BizException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.BAD_CREDENTIALS);
        verifyNoInteractions(sessionService, jwtUtil);
    }

    @Test
    void shouldRejectDisabledAdmin() {
        SysAdmin admin = activeAdmin();
        admin.setStatus(0);
        when(adminMapper.selectByUsername("admin")).thenReturn(admin);
        when(passwordEncoder.matches("password", "encoded-password")).thenReturn(true);

        assertThatThrownBy(() -> authService.login(loginRequest("password"), "127.0.0.1"))
                .isInstanceOf(BizException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ACCOUNT_DISABLED);
        verifyNoInteractions(sessionService, jwtUtil);
    }

    @Test
    void shouldLoginAndCreateRedisSession() {
        SysAdmin admin = activeAdmin();
        when(adminMapper.selectByUsername("admin")).thenReturn(admin);
        when(passwordEncoder.matches("password", "encoded-password")).thenReturn(true);
        when(sessionService.create(1L)).thenReturn("session-001");
        when(jwtUtil.createToken(1L, "session-001")).thenReturn("access-token");

        LoginResponse response = authService.login(loginRequest("password"), "127.0.0.1");

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEmpty();
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(1800);
        verify(adminMapper).updateLoginInfo(eq(1L), any(LocalDateTime.class), eq("127.0.0.1"));
    }

    private SysAdmin activeAdmin() {
        SysAdmin admin = new SysAdmin();
        admin.setId(1L);
        admin.setUsername("admin");
        admin.setPassword("encoded-password");
        admin.setNickname("超级管理员");
        admin.setStatus(1);
        admin.setPwdResetRequired(0);
        return admin;
    }

    private LoginRequest loginRequest(String password) {
        return new LoginRequest("admin", password, "test-device-001");
    }
}
