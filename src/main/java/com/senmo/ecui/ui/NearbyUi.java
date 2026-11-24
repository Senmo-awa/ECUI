package com.senmo.ecui.ui;

import com.senmo.ecui.EcUiConfig;
import com.senmo.ecui.EcUiCommands;
import com.senmo.ecui.EcUiMod;
import com.senmo.ecui.Lang;
import com.senmo.ecui.Texts;
import com.senmo.ecui.data.HomeScanner;
import com.senmo.ecui.data.HomeScanner.NearbyHome;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * /homesnear：管理员查询当前位置附近的所有玩家 home（含离线玩家）。
 */
public final class NearbyUi {
    private NearbyUi() {}

    public static int open(CommandSourceStack source, int radiusArgument) {
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (CommandSyntaxException ex) {
            source.sendFailure(Texts.playerOnly());
            return 0;
        }

        if (!EcUiCommands.isAdmin(source)) {
            source.sendFailure(Texts.noPermission());
            return 0;
        }

        EcUiConfig config = EcUiConfig.get();
        int radius = radiusArgument > 0
            ? Math.min(radiusArgument, config.nearbyMaxRadius)
            : config.nearbyDefaultRadius;

        MinecraftServer server = source.getServer();
        UUID playerId = player.getUUID();
        ResourceKey<Level> dimension = player.level().dimension();
        Vec3 center = player.position();
        boolean includeOffline = config.includeOfflineHomes;

        try (Lang.Scope ignored = Lang.open(player)) {
            source.sendSuccess(() -> Texts.scanning(radius), false);
        }

        List<HomeScanner.OwnedHome> online = HomeScanner.collectOnline(server);
        CompletableFuture
            .supplyAsync(() -> HomeScanner.scan(server, online, dimension, center, radius, includeOffline),
                HomeScanner.executor())
            .thenAccept(entries -> server.execute(() -> {
                ServerPlayer current = server.getPlayerList().getPlayer(playerId);
                if (current != null) {
                    show(current, entries, radius, dimension);
                }
            }))
            .exceptionally(throwable -> {
                EcUiMod.LOGGER.warn("扫描附近 home 失败", throwable);
                server.execute(() -> {
                    ServerPlayer current = server.getPlayerList().getPlayer(playerId);
                    if (current != null) {
                        try (Lang.Scope ignored = Lang.open(current)) {
                            current.sendSystemMessage(Texts.scanFailed());
                        }
                    }
                });
                return null;
            });

        return 1;
    }

    private static void show(ServerPlayer player, List<NearbyHome> entries, int radius,
                             ResourceKey<Level> dimension) {
        try (Lang.Scope ignored = Lang.open(player)) {
            int total = entries.size();
            EcUiMod.LOGGER.info("附近 home 扫描完成：{} 个（半径 {}，{}）",
                total, radius, player.getScoreboardName());
            if (total == 0) {
                UiSupport.notice(player, Texts.nearbyTitle(0), Texts.nearbyEmpty(radius));
                return;
            }

            int limit = EcUiConfig.get().nearbyMaxResults;
            List<NearbyHome> shown = total > limit ? entries.subList(0, limit) : entries;

            List<InventoryUi.Entry> inv = new ArrayList<>(shown.size() + 1);
            for (NearbyHome home : shown) {
                String label = (home.ownerName() + " · " + home.homeName())
                    .replace('\n', ' ')
                    .replace('\r', ' ');
                String command = String.format(Locale.ROOT,
                    "/ecui tploc %.3f %.3f %.3f %s %s",
                    home.location().x(), home.location().y(), home.location().z(),
                    home.location().dim().identifier(), label);
                inv.add(new InventoryUi.Entry(
                    Items.RECOVERY_COMPASS,
                    Texts.nearbyButton(home.ownerName(), home.homeName()),
                    Texts.locationLore(home.location(), home.distance()),
                    () -> UiSupport.runCommand(player, command)
                ));
            }

            inv.add(new InventoryUi.Entry(
                Items.CLOCK,
                Texts.reloadButton(),
                List.of(Texts.reloadButtonTooltip(radius)),
                () -> UiSupport.runCommand(player, "/homesnear " + radius)
            ));

            UiSupport.Mode mode = UiSupport.choose();
            if (mode == UiSupport.Mode.NONE) {
                player.sendSystemMessage(Texts.dialogUnavailable());
                return;
            }
            if (mode == UiSupport.Mode.DIALOG) {
                boolean opened = DialogBridge.openNearby(player, shown, radius, dimension, total, shown.size());
                if (opened) {
                    return;
                }
                if (!EcUiConfig.get().fallbackToInventory) {
                    player.sendSystemMessage(Texts.dialogUnavailable());
                    return;
                }
                UiSupport.logFallback("nearby", new IllegalStateException("DialogBridge returned false"));
            }

            InventoryUi.open(player, Texts.nearbyTitle(total), inv);
        }
    }
}
