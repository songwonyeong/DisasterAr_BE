package com.example.disaster_ar.util;

import java.security.SecureRandom;

public class CodeUtil {
    private static final String ALPHANUM = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final SecureRandom RND = new SecureRandom();

    public static String randomJoinCode(int len) {
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) sb.append(ALPHANUM.charAt(RND.nextInt(ALPHANUM.length())));
        return sb.toString();
    }
}
