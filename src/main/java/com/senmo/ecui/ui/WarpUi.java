package com.senmo.ecui.ui;

import com.fibermc.essentialcommands.ECPerms;
import com.fibermc.essentialcommands.EssentialCommands;
import com.fibermc.essentialcommands.ManagerLocator;
import com.fibermc.essentialcommands.WorldDataManager;
import com.fibermc.essentialcommands.types.WarpLocation;
import com.senmo.ecui.EcUiCommands;
import com.senmo.ecui.EcUiConfig;
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
import java.util.Map;

public final class WarpUi {
    private WarpUi() {}

    /** 与 UI 后端无关的一条 warp 记录。 */
    public record WarpItem(String name, WarpLocation warp, double distance,
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
            if (!warpEnabled()) {
                source.sendFailure(Texts.warpDisabled());
                return 0;
            }

            EcUiConfig config = EcUiConfig.get();
            if (config.checkWarpPermission && !canTeleport(source)) {
                source.sendFailure(Texts.warpPermission());
                return 0;
            }

            try {
                return show(source, player, config);
            } catch (Throwable throwable) {
                // 绝不让异常穿出去：穿出去 Brigadier 只会给玩家一句「命令执行失败」，
                // 而且 EC 的 Identifier 校验在 26.3 下随时可能因为数据不合规抛异常。
                EcUiMod.LOGGER.error("打开传送点 UI 失败", throwable);
                source.sendFailure(Texts.warpError());
                return 0;
            }
        }
    }

    private static int show(CommandSourceStack source, ServerPlayer player, EcUiConfig config) {
        WorldDataManager worldData = ManagerLocator.getInstance().getWorldDataManager();
        if (worldData == null) {
            source.sendFailure(Texts.warpDisabled());
            return 0;
        }

        List<WarpLocation> warps = new ArrayList<>();
        int broken = 0;
        for (Map.Entry<String, WarpLocation> entry : worldData.getWarpEntries()) {
            WarpLocation warp = entry.getValue();
            if (warp == null) {
                continue;
            }
            if (permissionBroken(warp)) {
                broken++;
            }
            if (visible(warp, source)) {
                warps.add(warp);
            }
        }
        warps.sort(Comparator.comparing(WarpLocation::getName, String.CASE_INSENSITIVE_ORDER));

        int total = warps.size();
        int limit = config.maxWarpButtons;
        boolean truncated = total > limit;
        if (truncated) {
            warps = warps.subList(0, limit);
        }

        if (warps.isEmpty()) {
            UiSupport.notice(player, Texts.warpTitle(0), Texts.warpEmpty());
            return 1;
        }

        Vec3 origin = player.position();
        List<WarpItem> items = new ArrayList<>(warps.size());
        for (WarpLocation warp : warps) {
            String name = warp.getName();
            double distance = warp.pos().distanceTo(origin);
            boolean safe = Tokens.safe(name);
            String command = safe ? "/essentialcommands warp tp " + name : null;
            items.add(new WarpItem(name, warp, distance, command, safe));
        }

        boolean admin = EcUiCommands.isAdmin(source);
        UiSupport.Mode mode = UiSupport.choose();
        if (mode == UiSupport.Mode.NONE) {
            source.sendFailure(Texts.dialogUnavailable());
            return 0;
        }

        if (mode == UiSupport.Mode.DIALOG) {
            if (DialogBridge.openWarp(player, total, items, truncated, broken, admin)) {
                return 1;
            }
            if (!config.fallbackToInventory) {
                source.sendFailure(Texts.dialogUnavailable());
                return 0;
            }
            UiSupport.logFallback("warp", new IllegalStateException("DialogBridge returned false"));
        }

        showInventory(player, items, total);
        return 1;
    }

    private static void showInventory(ServerPlayer player, List<WarpItem> warps, int total) {
        List<InventoryUi.Entry> entries = new ArrayList<>(warps.size() + 1);
        for (WarpItem warp : warps) {
            Runnable action;
            if (warp.safe()) {
                String command = warp.command();
                action = () -> UiSupport.runCommand(player, command);
            } else {
                action = () -> player.sendSystemMessage(Texts.unsafeNameTooltip(Texts.warpButton(warp.name())));
            }
            entries.add(new InventoryUi.Entry(
                Items.ENDER_PEARL,
                Texts.warpButton(warp.name()),
                Texts.locationLore(warp.warp(), warp.distance()),
                action
            ));
        }

        entries.add(new InventoryUi.Entry(
            Items.RECOVERY_COMPASS,
            Texts.switchToHomes(),
            List.of(Texts.switchTooltip()),
            () -> UiSupport.runCommand(player, "/home")
        ));

        InventoryUi.open(player, Texts.warpTitle(total), entries);
    }

    /**
     * 能不能把某个传送点列给这个人。
     *
     * <p>EC 0.42 的 {@code WarpLocation#hasPermission} 会把 {@code permissionString} 拼成
     * {@code essentialcommands:warp.tp_named.<名字>}，而 26.3 的 {@code Identifier} 只允许
     * {@code [a-z0-9/._-]}——名字带大写字母就直接抛 {@code IdentifierException}。
     * 这是 EC 的缺陷（它的 {@code /warp} 与补全同样会炸），我们不能改上游，
     * 于是逐个传送点兜住异常：能判就按 EC 判，判不了（权限名非法）只对管理员显示。</p>
     */
    public static boolean visible(WarpLocation warp, CommandSourceStack source) {
        try {
            String permission = warp.getPermissionString();
            if (permission == null || permission.isEmpty()) {
                return true;
            }
            ServerPlayer player = source.getPlayerOrException();
            return warp.hasPermission(player);
        } catch (Throwable ignored) {
            return EcUiCommands.isAdmin(source);
        }
    }

    public static List<String> suggestableNames(CommandSourceStack source) {
        List<String> names = new ArrayList<>();
        try {
            WorldDataManager worldData = ManagerLocator.getInstance().getWorldDataManager();
            if (worldData == null) {
                return names;
            }
            for (Map.Entry<String, WarpLocation> entry : worldData.getWarpEntries()) {
                WarpLocation warp = entry.getValue();
                if (warp != null && visible(warp, source)) {
                    names.add(warp.getName());
                }
            }
        } catch (Throwable ignored) {
            // 补全失败就返回空表，绝不让异常冒到 Brigadier 的 suggestion 线程
        }
        names.sort(String.CASE_INSENSITIVE_ORDER);
        return names;
    }

    private static boolean canTeleport(CommandSourceStack source) {
        try {
            return ECPerms.check(source, ECPerms.Registry.warp_tp, 0);
        } catch (Throwable throwable) {
            EcUiMod.LOGGER.warn("检查 warp 传送权限失败，按有权限处理：{}", throwable.toString());
            return true;
        }
    }

    private static boolean warpEnabled() {
        try {
            return EssentialCommands.CONFIG != null && EssentialCommands.CONFIG.ENABLE_WARP;
        } catch (Throwable ignored) {
            return false;
        }
    }

    /** 权限名里含 Identifier 非法字符（26.3 下 EC 自己都会抛异常）。 */
    private static boolean permissionBroken(WarpLocation warp) {
        try {
            String permission = warp.getPermissionString();
            if (permission == null || permission.isEmpty()) {
                return false;
            }
            return !permission.matches("[a-z0-9/._-]+");
        } catch (Throwable ignored) {
            return true;
        }
    }
}
