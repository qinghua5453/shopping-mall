package com.shopmall.bootstrap.security;

import com.shopmall.common.error.BizException;
import com.shopmall.common.error.ErrorCode;
import com.shopmall.security.application.service.TokenService;
import com.shopmall.security.config.AuthProperties;
import com.shopmall.security.domain.model.AccessTokenClaims;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TokenServiceTest {
    private final TokenService tokenService = new TokenService(new AuthProperties(
            "unit-test-jwt-secret-with-at-least-32-bytes",
            Duration.ofMinutes(30),
            Duration.ofDays(7)));

    @Test
    void issuesAndParsesAdminAccessToken() {
        String token = tokenService.createAccessToken(10001L, "session-1");

        AccessTokenClaims claims = tokenService.parseAccessToken(token);

        assertThat(claims.adminId()).isEqualTo(10001L);
        assertThat(claims.sessionId()).isEqualTo("session-1");
    }

    @Test
    void rejectsTamperedToken() {
        String token = tokenService.createAccessToken(10001L, "session-1") + "tampered";

        assertThatThrownBy(() -> tokenService.parseAccessToken(token))
                .isInstanceOf(BizException.class)
                .extracting(exception -> ((BizException) exception).getErrorCode())
                .isEqualTo(ErrorCode.TOKEN_INVALID);
    }
}
