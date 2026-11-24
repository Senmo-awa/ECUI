package com.senmo.ecui.util;

import java.util.Locale;

public final class Fmt {
    private Fmt() {}

    public static String coord(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    public static String distance(double value) {
        return String.format(Locale.ROOT, "%.0f", value);
    }
}
