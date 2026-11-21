package com.shopmall.security.domain.model;

import java.security.Principal;

public record AdminPrincipal(Long adminId, String username, String sessionId) implements Principal {
    @Override
    public String getName() {
        return username;
    }
}
