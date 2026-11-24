package com.senmo.ecui.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.senmo.ecui.EcUiMod;
import net.minecraft.server.MinecraftServer;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 从服务器根目录的 usercache.json 取 UUID -> 玩家名。
 * 不调用 Mojang API，离线玩家也能立刻显示。
 */
final class UserNames {
    private static volatile Map<UUID, String> cache = Map.of();
    private static volatile long loadedAt;
    private static volatile Path loadedFrom;

    private UserNames() {}

    static Map<UUID, String> load(MinecraftServer server) {
        Path path = server.getServerDirectory().resolve("usercache.json");
        long now = System.currentTimeMillis();
        if (path.equals(loadedFrom) && now - loadedAt < 60_000L) {
            return cache;
        }

        Map<UUID, String> names = new HashMap<>();
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonArray array = JsonParser.parseReader(reader).getAsJsonArray();
            for (JsonElement element : array) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject object = element.getAsJsonObject();
                if (!object.has("uuid") || !object.has("name")) {
                    continue;
                }
                try {
                    names.put(UUID.fromString(object.get("uuid").getAsString()), object.get("name").getAsString());
                } catch (RuntimeException ignored) {
                    // 单条坏数据不影响整体
                }
            }
        } catch (Exception ex) {
            EcUiMod.LOGGER.debug("读取 usercache.json 失败：{}", ex.toString());
        }

        cache = Map.copyOf(names);
        loadedAt = now;
        loadedFrom = path;
        return cache;
    }
}
