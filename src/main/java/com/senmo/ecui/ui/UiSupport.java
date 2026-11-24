package com.senmo.ecui.ui;

import com.senmo.ecui.EcUiConfig;
import com.senmo.ecui.EcUiMod;
import com.senmo.ecui.Texts;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * 选择当前服务器可用的 UI 后端。
 *
 * <p>1.21.6+ / 26.x 有原版服务端对话框；更早的版本没有。所有直接引用对话框类的代码都放在
 * {@link DialogBridge} 后面，只有确认对话框 API 存在时才加载，这样同一份源码在“只有物品栏”
 * 的版本上也能启动。</p>
 */
public final class UiSupport {
    private UiSupport() {}

    public enum Mode {
        DIALOG,
        INVENTORY,
        NONE
    }

    private static volatile Boolean dialogAvailable;

    public static boolean dialogAvailable() {
        Boolean cached = dialogAvailable;
        if (cached != null) {
            return cached;
        }
        boolean available;
        try {
            Class.forName("net.minecraft.server.dialog.MultiActionDialog", false,
                UiSupport.class.getClassLoader());
            available = true;
        } catch (Throwable ignored) {
            available = false;
        }
        dialogAvailable = available;
        return available;
    }

    public static Mode choose() {
        EcUiConfig config = EcUiConfig.get();
        if (config.wantsInventory()) {
            return Mode.INVENTORY;
        }
        if (dialogAvailable()) {
            return Mode.DIALOG;
        }
        if (config.wantsDialog() && !config.fallbackToInventory) {
            return Mode.NONE;
        }
        return Mode.INVENTORY;
    }

    public static void notice(ServerPlayer player, Component title, Component body) {
        if (choose() == Mode.DIALOG && DialogBridge.notice(player, title, body)) {
            return;
        }
        player.sendSystemMessage(title);
        player.sendSystemMessage(body);
    }

    public static void logFallback(String what, Throwable throwable) {
        EcUiMod.LOGGER.warn("{} 的自定义屏幕不可用，尝试物品栏 UI：{}", what, throwable.toString());
    }

    public static void runCommand(ServerPlayer player, String command) {
        if (command == null || command.isBlank()) {
            return;
        }
        String normalized = command.startsWith("/") ? command.substring(1) : command;
        try {
            player.level().getServer().getCommands().performPrefixedCommand(
                player.createCommandSourceStack(), normalized);
        } catch (Throwable throwable) {
            EcUiMod.LOGGER.warn("执行 UI 命令失败：/{}", normalized, throwable);
            player.sendSystemMessage(Texts.commandFailed());
        }
    }
}
