package com.shopmall.util;

import com.shopmall.common.BizException;
import com.shopmall.common.ErrorCode;
import com.shopmall.config.AuthProperties;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtUtilTest {
    private static final String SECRET = "test-secret-key-with-at-least-32-bytes";

    @Test
    void shouldCreateAndParseToken() {
        JwtUtil jwtUtil = new JwtUtil(new AuthProperties(SECRET, Duration.ofMinutes(30)));

        String token = jwtUtil.createToken(1L, "session-001");
        JwtUtil.AccessTokenClaims claims = jwtUtil.parse(token);

        assertThat(claims.adminId()).isEqualTo(1L);
        assertThat(claims.sessionId()).isEqualTo("session-001");
    }

    @Test
    void shouldRejectTamperedToken() {
        JwtUtil jwtUtil = new JwtUtil(new AuthProperties(SECRET, Duration.ofMinutes(30)));
        String token = jwtUtil.createToken(1L, "session-001");
        char lastChar = token.charAt(token.length() - 1);
        String tampered = token.substring(0, token.length() - 1) + (lastChar == 'a' ? 'b' : 'a');

        assertThatThrownBy(() -> jwtUtil.parse(tampered))
                .isInstanceOf(BizException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.TOKEN_INVALID);
    }

    @Test
    void shouldRejectExpiredToken() {
        JwtUtil jwtUtil = new JwtUtil(new AuthProperties(SECRET, Duration.ofSeconds(-1)));
        String token = jwtUtil.createToken(1L, "session-001");

        assertThatThrownBy(() -> jwtUtil.parse(token))
                .isInstanceOf(BizException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.TOKEN_EXPIRED);
    }
}
