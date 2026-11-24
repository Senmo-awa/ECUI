package com.senmo.ecui;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Server-side translations.
 *
 * <p>The dialog / chest UI is built on the server and sent to vanilla clients, so we cannot rely
 * on the client having our resource pack. Every visible string is resolved here and sent as a
 * literal component. The JSON files under {@code assets/ec_ui/lang} are still shipped so resource
 * pack / client-side tooling can reuse the same keys.</p>
 *
 * <p>{@link #current()} is a thread-local set by {@link LangContext}. UI code runs on the server
 * thread while it builds a menu, so this keeps the call sites readable without threading a
 * language parameter through every helper.</p>
 */
public final class Lang {
    public static final String ZH_CN = "zh_cn";
    public static final String EN_US = "en_us";

    private static final Gson GSON = new Gson();
    private static final Map<String, String> ZH = load("/assets/ec_ui/lang/zh_cn.json");
    private static final Map<String, String> EN = load("/assets/ec_ui/lang/en_us.json");
    private static final ThreadLocal<Language> CURRENT = ThreadLocal.withInitial(() -> new Language(EN_US, false));

    private Lang() {}

    public record Language(String code, boolean chinese) {
        public static Language of(String code) {
            String normalized = normalize(code);
            return new Language(normalized, ZH_CN.equals(normalized));
        }
    }

    /** Restores the previous language when closed, so nested UI calls are safe. */
    public static final class Scope implements AutoCloseable {
        private final Language previous;

        private Scope(Language previous) {
            this.previous = previous;
        }

        @Override
        public void close() {
            CURRENT.set(previous);
        }
    }

    public static Scope open(ServerPlayer player) {
        Language previous = CURRENT.get();
        CURRENT.set(resolve(player));
        return new Scope(previous);
    }

    public static Scope open(Language language) {
        Language previous = CURRENT.get();
        CURRENT.set(language);
        return new Scope(previous);
    }

    public static Language current() {
        return CURRENT.get();
    }

    public static Language resolve(ServerPlayer player) {
        String configured = EcUiConfig.get().language;
        if (configured != null && !configured.equalsIgnoreCase("auto")) {
            return Language.of(configured);
        }

        if (player != null) {
            try {
                String client = player.clientInformation().language();
                if (client != null && !client.isBlank()) {
                    return Language.of(client);
                }
            } catch (Throwable ignored) {
                // Very old / unusual server implementations may not expose client information.
            }
        }

        try {
            String ecLanguage = com.fibermc.essentialcommands.EssentialCommands.CONFIG.LANGUAGE;
            if (ecLanguage != null && !ecLanguage.isBlank()) {
                return Language.of(ecLanguage);
            }
        } catch (Throwable ignored) {
            // EC not ready yet; fall through to English.
        }
        return new Language(EN_US, false);
    }

    public static String normalize(String code) {
        if (code == null) {
            return EN_US;
        }
        String value = code.toLowerCase(Locale.ROOT).replace('-', '_');
        if (value.startsWith("zh")) {
            return ZH_CN;
        }
        return EN_US;
    }

    public static String tr(String key, Object... args) {
        Language language = current();
        Map<String, String> primary = language.chinese() ? ZH : EN;
        String pattern = primary.get(key);
        if (pattern == null) {
            pattern = EN.getOrDefault(key, key);
        }
        if (args == null || args.length == 0) {
            return pattern;
        }
        try {
            return String.format(Locale.ROOT, pattern, args);
        } catch (RuntimeException ex) {
            EcUiMod.LOGGER.warn("语言键 {} 的格式字符串有问题：{}", key, pattern);
            return pattern;
        }
    }

    public static Component c(String key, Object... args) {
        return Component.literal(tr(key, args));
    }

    public static void reload() {
        // Bundles are immutable at runtime; this method exists so /ecui reload can log a clear line
        // and future hot-reload implementations have one entry point.
        EcUiMod.LOGGER.info("语言包：zh_cn={} keys, en_us={} keys", ZH.size(), EN.size());
    }

    private static Map<String, String> load(String path) {
        try (InputStream stream = Lang.class.getResourceAsStream(path)) {
            if (stream == null) {
                EcUiMod.LOGGER.warn("找不到语言文件 {}", path);
                return Map.of();
            }
            Map<String, String> map = GSON.fromJson(
                new InputStreamReader(stream, StandardCharsets.UTF_8),
                new TypeToken<Map<String, String>>() {}.getType()
            );
            return map == null ? Map.of() : new HashMap<>(map);
        } catch (Exception ex) {
            EcUiMod.LOGGER.warn("读取语言文件 {} 失败：{}", path, ex.toString());
            return Map.of();
        }
    }
}
