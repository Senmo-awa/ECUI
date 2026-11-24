package com.senmo.ecui.ui;

import com.senmo.ecui.EcUiMod;
import com.senmo.ecui.data.HomeScanner.NearbyHome;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * 对话框实现的唯一入口。这个类本身不引用任何对话框类型，真正的引用在
 * {@link DialogHomeUi} / {@link DialogWarpUi} / {@link DialogNearbyUi} /
 * {@link DialogSelfTest} 里；只有确认 API 存在时才会调用它们。
 */
public final class DialogBridge {
    private DialogBridge() {}

    public static boolean openHome(ServerPlayer player, int total, List<HomeUi.HomeItem> homes,
                                   boolean truncated, boolean admin) {
        try {
            DialogHomeUi.open(player, total, homes, truncated, admin);
            return true;
        } catch (Throwable throwable) {
            EcUiMod.LOGGER.warn("打开 home 对话框失败：{}", throwable.toString());
            return false;
        }
    }

    public static boolean openWarp(ServerPlayer player, int total, List<WarpUi.WarpItem> warps,
                                   boolean truncated, int broken, boolean admin) {
        try {
            DialogWarpUi.open(player, total, warps, truncated, broken, admin);
            return true;
        } catch (Throwable throwable) {
            EcUiMod.LOGGER.warn("打开 warp 对话框失败：{}", throwable.toString());
            return false;
        }
    }

    public static boolean openNearby(ServerPlayer player, List<NearbyHome> entries, int radius,
                                     ResourceKey<Level> dimension, int total, int shown) {
        try {
            DialogNearbyUi.open(player, entries, radius, dimension, total, shown);
            return true;
        } catch (Throwable throwable) {
            EcUiMod.LOGGER.warn("打开附近家对话框失败：{}", throwable.toString());
            return false;
        }
    }

    public static boolean notice(ServerPlayer player, Component title, Component body) {
        try {
            Dialogs.notice(player, title, body);
            return true;
        } catch (Throwable throwable) {
            EcUiMod.LOGGER.warn("打开提示对话框失败：{}", throwable.toString());
            return false;
        }
    }

    public static boolean selfTest(CommandSourceStack source) {
        try {
            return DialogSelfTest.run(source);
        } catch (Throwable throwable) {
            EcUiMod.LOGGER.error("ECUI dialog selftest 失败", throwable);
            source.sendFailure(Component.literal("ECUI dialog selftest failed: " + throwable));
            return false;
        }
    }
}
