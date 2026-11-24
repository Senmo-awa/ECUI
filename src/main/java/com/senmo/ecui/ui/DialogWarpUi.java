package com.senmo.ecui.ui;

import com.senmo.ecui.Texts;
import net.minecraft.network.chat.Component;
import net.minecraft.server.dialog.ActionButton;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * 原版服务端对话框版本的传送点列表。只在确认 {@code MultiActionDialog} 存在后由
 * {@link DialogBridge} 加载。
 */
final class DialogWarpUi {
    private DialogWarpUi() {}

    static void open(ServerPlayer player, int total, List<WarpUi.WarpItem> warps,
                     boolean truncated, int broken, boolean admin) {
        List<ActionButton> buttons = new ArrayList<>(warps.size() + 1);
        for (WarpUi.WarpItem warp : warps) {
            Component tooltip = Texts.locationTooltip(warp.warp(), warp.distance());
            String command = null;
            if (warp.safe()) {
                command = warp.command();
                tooltip = Texts.safeTeleportHint(tooltip);
            } else {
                tooltip = Texts.unsafeNameTooltip(tooltip);
            }
            buttons.add(Dialogs.button(Texts.warpButton(warp.name()), tooltip, command));
        }

        buttons.add(Dialogs.button(Texts.switchToHomes(), Texts.switchTooltip(), "/home"));

        Component body = Texts.warpBody(truncated);
        if (broken > 0 && admin) {
            body = Component.empty().append(body).append(Component.literal("\n\n"))
                .append(Texts.warpPermissionBroken(broken));
        }
        Dialogs.show(player, Texts.warpTitle(total), body, buttons, 2);
    }
}
