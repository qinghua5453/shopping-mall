package com.shopmall.util;

import java.util.UUID;

/**
 * 应用层生成的全局唯一 ID（与现有 sys_admin/sys_role 无 AUTO_INCREMENT 的表保持一致）。
 * 取 UUID 的最高 63 位，保证正数。
 */
public final class IdGenerator {

    private IdGenerator() {
    }

    public static long nextId() {
        return UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE;
    }
}
