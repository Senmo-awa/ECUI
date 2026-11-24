package com.senmo.ecui.ui;

import com.senmo.ecui.Texts;
import net.minecraft.network.chat.Component;
import net.minecraft.server.dialog.ActionButton;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * 原版服务端对话框版本的家列表。只在确认 {@code MultiActionDialog} 存在后由
 * {@link DialogBridge} 加载。
 */
final class DialogHomeUi {
    private DialogHomeUi() {}

    static void open(ServerPlayer player, int total, List<HomeUi.HomeItem> homes,
                     boolean truncated, boolean admin) {
        List<ActionButton> buttons = new ArrayList<>(homes.size() + 2);
        for (HomeUi.HomeItem home : homes) {
            Component tooltip = Texts.locationTooltip(home.location(), home.distance());
            String command = null;
            if (home.safe()) {
                command = home.command();
                tooltip = Texts.safeTeleportHint(tooltip);
            } else {
                tooltip = Texts.unsafeNameTooltip(tooltip);
            }
            buttons.add(Dialogs.button(Texts.homeButton(home.name()), tooltip, command));
        }

        if (admin) {
            buttons.add(Dialogs.button(
                Texts.nearbyButtonAdmin(),
                Texts.nearbyButtonAdminTooltip(),
                "/homesnear"
            ));
        }

        buttons.add(Dialogs.button(Texts.switchToWarps(), Texts.switchTooltip(), "/warp"));
        Dialogs.show(player, Texts.homeTitle(total), Texts.homeBody(truncated), buttons, 2);
    }
}
