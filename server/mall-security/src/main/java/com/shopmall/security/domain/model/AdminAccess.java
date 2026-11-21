package com.shopmall.security.domain.model;

import com.shopmall.security.api.dto.AdminMenu;

import java.util.List;

public record AdminAccess(List<String> permissions, List<AdminMenu> menus) {
}
