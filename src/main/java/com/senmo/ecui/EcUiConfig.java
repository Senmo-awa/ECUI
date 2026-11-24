package com.senmo.ecui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/**
 * mod 自己的配置。故意保持很小，只放 UI 行为相关的东西。
 */
public final class EcUiConfig {
    private static final Gson GSON = new GsonBuilder()
        .setPrettyPrinting()
        .disableHtmlEscaping()
        .create();
    private static final Path PATH = FabricLoader.getInstance()
        .getConfigDir()
        .resolve("ec_ui.json");

    private static volatile EcUiConfig current = new EcUiConfig();

    /** auto = 跟随玩家客户端语言（控制台回退到 EC 的 language）。 */
    public String language = "auto";

    /** auto / dialog / inventory。 */
    public String uiMode = "auto";

    /** 当配置要求 dialog、但当前 MC 没有对话框 API 时，是否自动改用物品栏 UI。 */
    public boolean fallbackToInventory = true;

    /** 打开 home UI 前是否检查 essentialcommands.home.tp。 */
    public boolean checkHomePermission = true;

    /** 打开 warp UI 前是否检查 essentialcommands.warp.tp。 */
    public boolean checkWarpPermission = true;

    /** 物品栏 UI 的箱子行数：3 或 6。 */
    public int inventoryRows = 6;

    public int maxHomeButtons = 60;
    public int maxWarpButtons = 60;
    public int nearbyDefaultRadius = 128;
    public int nearbyMaxRadius = 20000;
    public int nearbyMaxResults = 45;
    public boolean includeOfflineHomes = true;

    public static EcUiConfig get() {
        return current;
    }

    public static void load() {
        current = read();
    }

    public static void reload() {
        load();
        Lang.reload();
    }

    private static EcUiConfig read() {
        try {
            if (Files.notExists(PATH)) {
                EcUiConfig defaults = new EcUiConfig();
                defaults.sanitize();
                Files.createDirectories(PATH.getParent());
                Files.writeString(PATH, GSON.toJson(defaults), StandardCharsets.UTF_8);
                return defaults;
            }

            String json = Files.readString(PATH, StandardCharsets.UTF_8);
            EcUiConfig parsed = GSON.fromJson(json, EcUiConfig.class);
            if (parsed == null) {
                parsed = new EcUiConfig();
            }
            parsed.sanitize();
            return parsed;
        } catch (IOException | RuntimeException ex) {
            EcUiMod.LOGGER.warn("读取 {} 失败，使用默认配置：{}", PATH, ex.toString());
            EcUiConfig fallback = new EcUiConfig();
            fallback.sanitize();
            return fallback;
        }
    }

    private void sanitize() {
        if (language == null || language.isBlank()) {
            language = "auto";
        }
        language = language.toLowerCase(Locale.ROOT).replace('-', '_');
        if (language.equals("us_en")) {
            language = "en_us";
        } else if (language.startsWith("zh")) {
            language = "zh_cn";
        } else if (language.startsWith("en")) {
            language = "en_us";
        } else if (!language.equals("auto")) {
            language = "auto";
        }

        if (uiMode == null || uiMode.isBlank()) {
            uiMode = "auto";
        }
        uiMode = uiMode.toLowerCase(Locale.ROOT);
        if (!uiMode.equals("auto") && !uiMode.equals("dialog") && !uiMode.equals("inventory")) {
            uiMode = "auto";
        }

        inventoryRows = inventoryRows == 3 ? 3 : 6;
        maxHomeButtons = clamp(maxHomeButtons, 1, 300);
        maxWarpButtons = clamp(maxWarpButtons, 1, 300);
        nearbyMaxRadius = clamp(nearbyMaxRadius, 16, 100_000);
        nearbyDefaultRadius = clamp(nearbyDefaultRadius, 1, nearbyMaxRadius);
        nearbyMaxResults = clamp(nearbyMaxResults, 1, 200);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public boolean wantsInventory() {
        return "inventory".equals(uiMode);
    }

    public boolean wantsDialog() {
        return "dialog".equals(uiMode);
    }

    /**
     * 是否使用中文 UI。保留给旧代码/外部调用；实际语言由 {@link Lang#current()} 决定。
     */
    public boolean chinese() {
        return Lang.current().chinese();
    }
}
