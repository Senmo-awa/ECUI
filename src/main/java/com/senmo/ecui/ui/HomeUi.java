package com.senmo.ecui.ui;

import com.fibermc.essentialcommands.ECPerms;
import com.fibermc.essentialcommands.EssentialCommands;
import com.fibermc.essentialcommands.playerdata.PlayerData;
import com.fibermc.essentialcommands.types.NamedMinecraftLocation;
import com.senmo.ecui.EcUiConfig;
import com.senmo.ecui.EcUiCommands;
import com.senmo.ecui.EcUiMod;
import com.senmo.ecui.Lang;
import com.senmo.ecui.Texts;
import com.senmo.ecui.util.Tokens;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class HomeUi {
    private HomeUi() {}

    /** 与 UI 后端无关的一条 home 记录。 */
    public record HomeItem(String name, NamedMinecraftLocation location, double distance,
                           String command, boolean safe) {}

    public static int open(CommandSourceStack source) {
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (CommandSyntaxException ex) {
            source.sendFailure(Texts.playerOnly());
            return 0;
        }

        try (Lang.Scope ignored = Lang.open(player)) {
            if (!homeEnabled()) {
                source.sendFailure(Texts.homeDisabled());
                return 0;
            }

            EcUiConfig config = EcUiConfig.get();
            if (config.checkHomePermission && !canTeleport(source)) {
                source.sendFailure(Texts.homePermission());
                return 0;
            }

            PlayerData data = PlayerData.access(player);
            List<Map.Entry<String, NamedMinecraftLocation>> entries = new ArrayList<>(data.getHomeEntries());
            entries.sort(Comparator.comparing(entry -> entry.getKey().toLowerCase(Locale.ROOT)));

            int total = entries.size();
            int limit = config.maxHomeButtons;
            boolean truncated = total > limit;
            if (truncated) {
                entries = entries.subList(0, limit);
            }

            if (entries.isEmpty()) {
                UiSupport.notice(player, Texts.homeTitle(0), Texts.homeEmpty());
                return 1;
            }

            Vec3 origin = player.position();
            List<HomeItem> homes = new ArrayList<>(entries.size());
            for (Map.Entry<String, NamedMinecraftLocation> entry : entries) {
                String name = entry.getKey();
                NamedMinecraftLocation location = entry.getValue();
                double distance = location.pos().distanceTo(origin);
                boolean safe = Tokens.safe(name);
                String command = safe ? "/essentialcommands home tp " + name : null;
                homes.add(new HomeItem(name, location, distance, command, safe));
            }

            boolean admin = EcUiCommands.isAdmin(source);
            UiSupport.Mode mode = UiSupport.choose();
            if (mode == UiSupport.Mode.NONE) {
                source.sendFailure(Texts.dialogUnavailable());
                return 0;
            }

            if (mode == UiSupport.Mode.DIALOG) {
                if (DialogBridge.openHome(player, total, homes, truncated, admin)) {
                    return 1;
                }
                if (!config.fallbackToInventory) {
                    source.sendFailure(Texts.dialogUnavailable());
                    return 0;
                }
                UiSupport.logFallback("home", new IllegalStateException("DialogBridge returned false"));
            }

            showInventory(player, homes, total, admin);
            return 1;
        } catch (Throwable throwable) {
            EcUiMod.LOGGER.error("打开 home UI 失败", throwable);
            source.sendFailure(Texts.homeError());
            return 0;
        }
    }

    private static void showInventory(ServerPlayer player, List<HomeItem> homes, int total, boolean admin) {
        List<InventoryUi.Entry> entries = new ArrayList<>(homes.size() + 2);
        for (HomeItem home : homes) {
            Runnable action;
            if (home.safe()) {
                String command = home.command();
                action = () -> UiSupport.runCommand(player, command);
            } else {
                action = () -> player.sendSystemMessage(Texts.unsafeNameTooltip(Texts.homeButton(home.name())));
            }
            entries.add(new InventoryUi.Entry(
                Items.RECOVERY_COMPASS,
                Texts.homeButton(home.name()),
                Texts.locationLore(home.location(), home.distance()),
                action
            ));
        }

        if (admin) {
            entries.add(new InventoryUi.Entry(
                Items.SPYGLASS,
                Texts.nearbyButtonAdmin(),
                List.of(Texts.nearbyButtonAdminTooltip()),
                () -> UiSupport.runCommand(player, "/homesnear")
            ));
        }

        entries.add(new InventoryUi.Entry(
            Items.ENDER_PEARL,
            Texts.switchToWarps(),
            List.of(Texts.switchTooltip()),
            () -> UiSupport.runCommand(player, "/warp")
        ));

        InventoryUi.open(player, Texts.homeTitle(total), entries);
    }

    private static boolean canTeleport(CommandSourceStack source) {
        try {
            return ECPerms.check(source, ECPerms.Registry.home_tp, 0);
        } catch (Throwable throwable) {
            EcUiMod.LOGGER.warn("检查 home 传送权限失败，按有权限处理：{}", throwable.toString());
            return true;
        }
    }

    private static boolean homeEnabled() {
        try {
            return EssentialCommands.CONFIG != null && EssentialCommands.CONFIG.ENABLE_HOME;
        } catch (Throwable ignored) {
            return false;
        }
    }
}
