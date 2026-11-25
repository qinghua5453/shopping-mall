package com.shopmall.dto;

import java.util.List;

public record MeResponse(
        AdminProfile admin,
        List<String> permissions,
        List<AdminMenu> menus,
        boolean pwdResetRequired
) {
}
