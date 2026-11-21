package com.shopmall.security.api.dto;

import java.util.List;

public record AdminMeResponse(
        AdminProfile admin,
        List<String> permissions,
        List<AdminMenu> menus,
        boolean pwdResetRequired
) {
}
