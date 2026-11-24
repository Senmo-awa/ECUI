package com.senmo.ecui.data;

import com.fibermc.essentialcommands.playerdata.PlayerData;
import com.fibermc.essentialcommands.types.MinecraftLocation;
import com.senmo.ecui.EcUiMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.Vec3;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 读取所有玩家的 home。
 *
 * <p>在线玩家直接读 EC 的 PlayerData；离线玩家读 world/modplayerdata/&lt;uuid&gt;.dat。
 * 离线文件是 gzip NBT，用 EC 自己的 Codec 解析，格式随 EC 升级自动跟随。</p>
 */
public final class HomeScanner {
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "ec-ui-home-scan");
        thread.setDaemon(true);
        return thread;
    });

    private HomeScanner() {}

    public record OwnedHome(UUID owner, String ownerName, String homeName,
                            MinecraftLocation location, boolean online) {}

    public record NearbyHome(UUID owner, String ownerName, String homeName, MinecraftLocation location,
                             boolean online, double distance) {}

    public static ExecutorService executor() {
        return EXECUTOR;
    }

    public static void shutdown() {
        EXECUTOR.shutdownNow();
    }

    /** 在服务端线程调用：拿在线玩家的最新 home（可能还没落盘）。 */
    public static List<OwnedHome> collectOnline(MinecraftServer server) {
        List<OwnedHome> result = new ArrayList<>();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            PlayerData data = PlayerData.access(player);
            if (data == null) {
                continue;
            }
            String name = player.getScoreboardName();
            for (Map.Entry<String, ?> entry : data.getHomeEntries()) {
                if (entry.getValue() instanceof MinecraftLocation location) {
                    result.add(new OwnedHome(player.getUUID(), name, entry.getKey(), location, true));
                }
            }
        }
        return result;
    }

    /**
     * 在扫描线程调用。只读文件 + 纯计算，不碰服务端状态。
     */
    public static List<NearbyHome> scan(MinecraftServer server, List<OwnedHome> online,
                                        ResourceKey<Level> dimension, Vec3 center,
                                        double radius, boolean includeOffline) {
        List<OwnedHome> all = new ArrayList<>(online);
        if (includeOffline) {
            Set<UUID> onlineIds = online.stream().map(OwnedHome::owner).collect(Collectors.toSet());
            all.addAll(readOffline(server, onlineIds));
        }

        Map<UUID, String> names = UserNames.load(server);
        Map<String, NearbyHome> unique = new LinkedHashMap<>();
        for (OwnedHome home : all) {
            MinecraftLocation location = home.location();
            if (location == null || !dimension.equals(location.dim())) {
                continue;
            }
            double distance = location.pos().distanceTo(center);
            if (distance > radius) {
                continue;
            }

            String ownerName = home.ownerName();
            if (ownerName == null) {
                ownerName = names.get(home.owner());
            }
            if (ownerName == null) {
                ownerName = home.owner().toString().substring(0, 8);
            }

            NearbyHome entry = new NearbyHome(home.owner(), ownerName, home.homeName(), location,
                home.online(), distance);
            // 在线数据优先：collectOnline 在 readOffline 之前，putIfAbsent 保证在线版本留下。
            unique.putIfAbsent(home.owner() + "|" + home.homeName(), entry);
        }

        List<NearbyHome> result = new ArrayList<>(unique.values());
        result.sort(Comparator.comparingDouble(NearbyHome::distance));
        return result;
    }

    private static List<OwnedHome> readOffline(MinecraftServer server, Set<UUID> skip) {
        Path directory = server.getWorldPath(LevelResource.ROOT).resolve("modplayerdata");
        if (!Files.isDirectory(directory)) {
            return List.of();
        }

        List<OwnedHome> result = new ArrayList<>();
        List<Path> files;
        try (Stream<Path> stream = Files.list(directory)) {
            files = stream
                .filter(Files::isRegularFile)
                .filter(path -> path.getFileName().toString().endsWith(".dat"))
                .toList();
        } catch (IOException ex) {
            EcUiMod.LOGGER.warn("无法列出 {}：{}", directory, ex.toString());
            return List.of();
        }

        for (Path file : files) {
            String fileName = file.getFileName().toString();
            UUID uuid;
            try {
                uuid = UUID.fromString(fileName.substring(0, fileName.length() - 4));
            } catch (RuntimeException ex) {
                continue;
            }
            if (skip.contains(uuid)) {
                continue;
            }

            try {
                CompoundTag tag = NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap());
                PlayerData data = PlayerData.CODEC
                    .parse(NbtOps.INSTANCE, PlayerData.fixData(tag))
                    .getOrThrow();
                for (Map.Entry<String, ?> entry : data.getHomeEntries()) {
                    if (entry.getValue() instanceof MinecraftLocation location) {
                        result.add(new OwnedHome(uuid, null, entry.getKey(), location, false));
                    }
                }
            } catch (Exception ex) {
                EcUiMod.LOGGER.debug("跳过无法解析的玩家数据 {}：{}", fileName, ex.toString());
            }
        }
        return result;
    }
}
