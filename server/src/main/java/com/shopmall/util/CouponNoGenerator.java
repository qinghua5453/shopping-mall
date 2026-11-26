package com.shopmall.util;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public final class CouponNoGenerator {
    private static final DateTimeFormatter TEMPLATE_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final DateTimeFormatter BATCH_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final DateTimeFormatter INSTANCE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String CODE_ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"; // 去掉 0/O/1/I
    private static final int CODE_LENGTH = 16;

    private CouponNoGenerator() {}

    public static String templateNo() {
        return "T" + LocalDateTime.now().format(TEMPLATE_FMT) + random(4);
    }

    public static String batchNo() {
        return "B" + LocalDateTime.now().format(BATCH_FMT) + random(4);
    }

    public static String couponNo() {
        return "U" + LocalDateTime.now().format(INSTANCE_FMT) + random(8);
    }

    public static String couponCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(CODE_ALPHABET.charAt(RANDOM.nextInt(CODE_ALPHABET.length())));
        }
        return sb.toString();
    }

    public static String requestNo() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private static String random(int len) {
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append(CODE_ALPHABET.charAt(RANDOM.nextInt(CODE_ALPHABET.length())));
        }
        return sb.toString();
    }
}
