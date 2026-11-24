package com.senmo.ecui.ui;

import com.senmo.ecui.Texts;
import com.senmo.ecui.data.HomeScanner.NearbyHome;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.dialog.ActionButton;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 原版服务端对话框版本的附近家列表。只在确认 {@code MultiActionDialog} 存在后由
 * {@link DialogBridge} 加载。
 */
final class DialogNearbyUi {
    private DialogNearbyUi() {}

    static void open(ServerPlayer player, List<NearbyHome> entries, int radius,
                     ResourceKey<Level> dimension, int total, int shown) {
        List<ActionButton> buttons = new ArrayList<>(shown + 1);
        for (NearbyHome home : entries) {
            Component base = Texts.locationTooltip(home.location(), home.distance());
            String label = (home.ownerName() + " · " + home.homeName())
                .replace('\n', ' ')
                .replace('\r', ' ');
            String command = String.format(Locale.ROOT,
                "/ecui tploc %.3f %.3f %.3f %s %s",
                home.location().x(), home.location().y(), home.location().z(),
                home.location().dim().identifier(), label);
            buttons.add(Dialogs.button(
                Texts.nearbyButton(home.ownerName(), home.homeName()),
                Texts.safeTeleportHint(base),
                command));
        }

        buttons.add(Dialogs.button(
            Texts.reloadButton(),
            Texts.reloadButtonTooltip(radius),
            "/homesnear " + radius
        ));

        Dialogs.show(player, Texts.nearbyTitle(total),
            Texts.nearbyBody(radius, dimension, total, shown), buttons, 2);
    }
}
