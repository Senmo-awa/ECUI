package com.senmo.ecui.ui;

import io.netty.buffer.Unpooled;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ClientboundShowDialogPacket;
import net.minecraft.server.dialog.MultiActionDialog;

import java.util.List;

/**
 * 用真实的 {@link ClientboundShowDialogPacket} 编解码一次，验证动态对话框在当前 MC 版本上
 * 能过网络层。只有对话框 API 存在时才会加载。
 */
final class DialogSelfTest {
    private DialogSelfTest() {}

    static boolean run(CommandSourceStack source) {
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(
            Unpooled.buffer(), source.getServer().registryAccess());
        try {
            MultiActionDialog dialog = Dialogs.build(
                Component.literal("ECUI selftest"),
                Component.literal("dialog codec"),
                List.of(Dialogs.button(Component.literal("Test"), Component.literal("tooltip"), "/say hi")),
                1
            );

            ClientboundShowDialogPacket packet = new ClientboundShowDialogPacket(Holder.direct(dialog));
            ClientboundShowDialogPacket.STREAM_CODEC.encode(buffer, packet);
            ClientboundShowDialogPacket decoded = ClientboundShowDialogPacket.STREAM_CODEC.decode(buffer);
            String name = decoded.dialog().value().getClass().getSimpleName();
            int trailing = buffer.readableBytes();
            source.sendSuccess(() -> Component.literal(
                "ECUI dialog codec OK: decoded=" + name + ", trailing=" + trailing), false);
            return trailing == 0;
        } finally {
            buffer.release();
        }
    }
}
