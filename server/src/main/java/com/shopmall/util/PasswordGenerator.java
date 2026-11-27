package com.shopmall.util;

import java.security.SecureRandom;

/**
 * 生成初始/重置密码：12 位，包含大小写字母、数字与一个符号。
 */
public final class PasswordGenerator {
    private static final String LOWER = "abcdefghijkmnpqrstuvwxyz";
    private static final String UPPER = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String DIGITS = "23456789";
    private static final String SYMBOLS = "!@#$%^&*";
    private static final String ALL = LOWER + UPPER + DIGITS + SYMBOLS;
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordGenerator() {
    }

    public static String generate() {
        StringBuilder sb = new StringBuilder(12);
        sb.append(pick(LOWER));
        sb.append(pick(UPPER));
        sb.append(pick(DIGITS));
        sb.append(pick(SYMBOLS));
        for (int i = 0; i < 8; i++) {
            sb.append(pick(ALL));
        }
        return shuffle(sb.toString());
    }

    private static char pick(String src) {
        return src.charAt(RANDOM.nextInt(src.length()));
    }

    private static String shuffle(String s) {
        char[] arr = s.toCharArray();
        for (int i = arr.length - 1; i > 0; i--) {
            int j = RANDOM.nextInt(i + 1);
            char t = arr[i];
            arr[i] = arr[j];
            arr[j] = t;
        }
        return new String(arr);
    }
}
