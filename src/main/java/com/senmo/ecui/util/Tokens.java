package com.senmo.ecui.util;

import java.util.regex.Pattern;

/**
 * EC 的 home / warp 名来自 StringArgumentType.word()，正常情况下只含
 * 字母数字和 _ - . +。这里再挡一层，避免把奇怪的名字拼进点击命令。
 */
public final class Tokens {
    private static final Pattern SAFE = Pattern.compile("[A-Za-z0-9_.+-]{1,64}");

    private Tokens() {}

    public static boolean safe(String token) {
        return token != null && SAFE.matcher(token).matches();
    }
}
