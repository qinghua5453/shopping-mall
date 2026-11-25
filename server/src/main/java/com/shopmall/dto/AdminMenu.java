package com.shopmall.dto;

import java.util.List;

public record AdminMenu(
        String id,
        String name,
        String code,
        String routePath,
        String icon,
        List<AdminMenu> children
) {
}
