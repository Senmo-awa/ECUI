package com.senmo.ecui.ui;

import com.senmo.ecui.Texts;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.dialog.ActionButton;
import net.minecraft.server.dialog.CommonButtonData;
import net.minecraft.server.dialog.CommonDialogData;
import net.minecraft.server.dialog.DialogAction;
import net.minecraft.server.dialog.MultiActionDialog;
import net.minecraft.server.dialog.NoticeDialog;
import net.minecraft.server.dialog.action.Action;
import net.minecraft.server.dialog.action.StaticAction;
import net.minecraft.server.dialog.body.PlainMessage;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Optional;

/**
 * 用原版 1.21.6+ 的对话框系统做服务端 UI：
 * 不需要客户端装 mod，所有按钮都是“点击后执行一条服务端命令”。
 */
public final class Dialogs {
    private static final int BODY_WIDTH = 340;
    private static final int BUTTON_WIDTH = 200;

    private Dialogs() {}

    public static ActionButton button(Component label, Component tooltip, int width, String command) {
        Optional<Action> action = Optional.ofNullable(command)
            .map(cmd -> new StaticAction(new ClickEvent.RunCommand(cmd)));
        Optional<Component> tip = Optional.ofNullable(tooltip);
        return new ActionButton(new CommonButtonData(label, tip, width), action);
    }

    public static ActionButton button(Component label, Component tooltip, String command) {
        return button(label, tooltip, BUTTON_WIDTH, command);
    }

    public static void show(ServerPlayer player, Component title, Component body,
                            List<ActionButton> buttons, int columns) {
        player.openDialog(Holder.direct(build(title, body, buttons, columns)));
    }

    public static MultiActionDialog build(Component title, Component body,
                                          List<ActionButton> buttons, int columns) {
        CommonDialogData common = new CommonDialogData(
            title,
            Optional.empty(),
            true,
            false,
            DialogAction.CLOSE,
            List.of(new PlainMessage(body, BODY_WIDTH)),
            List.of()
        );
        ActionButton close = button(Texts.close(), null, 100, null);
        return new MultiActionDialog(common, buttons, Optional.of(close), Math.max(1, columns));
    }

    public static void notice(ServerPlayer player, Component title, Component body) {
        CommonDialogData common = new CommonDialogData(
            title,
            Optional.empty(),
            true,
            false,
            DialogAction.CLOSE,
            List.of(new PlainMessage(body, BODY_WIDTH)),
            List.of()
        );
        ActionButton close = button(Texts.close(), null, 100, null);
        player.openDialog(Holder.direct(new NoticeDialog(common, close)));
    }
}
