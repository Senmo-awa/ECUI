package com.senmo.ecui;

import com.fibermc.essentialcommands.ECPerms;
import com.fibermc.essentialcommands.playerdata.PlayerData;
import com.fibermc.essentialcommands.teleportation.PlayerTeleporter;
import com.fibermc.essentialcommands.types.MinecraftLocation;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.senmo.ecui.ui.DialogBridge;
import com.senmo.ecui.ui.UiSupport;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.world.level.Level;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

public final class EcUiCommands {
    private EcUiCommands() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            registerNearby(dispatcher, "homesnear");
            registerNearby(dispatcher, "nearbyhomes");

            dispatcher.register(Commands.literal("ecui")
                .requires(EcUiCommands::isAdmin)
                .executes(context -> {
                    context.getSource().sendSuccess(Texts::ecuiHelp, false);
                    return 1;
                })
                .then(Commands.literal("reload")
                    .executes(context -> {
                        EcUiConfig.reload();
                        context.getSource().sendSuccess(Texts::reloaded, false);
                        return 1;
                    }))
                .then(Commands.literal("selftest")
                    .executes(context -> selfTest(context.getSource())))
                .then(Commands.literal("tploc")
                    .then(Commands.argument("x", DoubleArgumentType.doubleArg())
                        .then(Commands.argument("y", DoubleArgumentType.doubleArg())
                            .then(Commands.argument("z", DoubleArgumentType.doubleArg())
                                .then(Commands.argument("dimension", IdentifierArgument.id())
                                    .then(Commands.argument("label", StringArgumentType.greedyString())
                                        .executes(EcUiCommands::teleportLocation))))))));
        });
    }

    private static void registerNearby(CommandDispatcher<CommandSourceStack> dispatcher, String name) {
        dispatcher.register(Commands.literal(name)
            .requires(EcUiCommands::isAdmin)
            .executes(context -> com.senmo.ecui.ui.NearbyUi.open(context.getSource(), -1))
            .then(Commands.argument("radius", IntegerArgumentType.integer(1, 100_000))
                .executes(context -> com.senmo.ecui.ui.NearbyUi.open(
                    context.getSource(),
                    IntegerArgumentType.getInteger(context, "radius")))));
    }

    /**
     * 用真实的 ClientboundShowDialogPacket 编解码一次，验证动态对话框
     * （Holder.direct + StaticAction）在当前 MC 版本上能过网络层。
     */
    private static int selfTest(CommandSourceStack source) {
        if (!UiSupport.dialogAvailable()) {
            source.sendFailure(Component.literal(
                "ECUI dialog selftest skipped: this Minecraft version has no dialog API."));
            return 0;
        }
        return DialogBridge.selfTest(source) ? 1 : 0;
    }

    /**
     * /ecui tploc：管理员从“附近的家”列表点击传送时走这里。
     * 直接按扫描到的坐标传送，不依赖 Mojang 在线档案查询，离线玩家也能用。
     */
    private static int teleportLocation(CommandContext<CommandSourceStack> context)
        throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();

        Identifier dimensionId = IdentifierArgument.getId(context, "dimension");
        ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, dimensionId);
        if (source.getServer().getLevel(dimension) == null) {
            source.sendFailure(Component.literal("Dimension is not loaded: " + dimensionId));
            return 0;
        }

        double x = DoubleArgumentType.getDouble(context, "x");
        double y = DoubleArgumentType.getDouble(context, "y");
        double z = DoubleArgumentType.getDouble(context, "z");
        String label = StringArgumentType.getString(context, "label");

        MinecraftLocation location = new MinecraftLocation(dimension, x, y, z);
        PlayerTeleporter.requestTeleport(PlayerData.access(player), location, Component.literal(label));
        return 1;
    }

    /**
     * 管理员判定沿用 EC 的权限节点 essentialcommands.home_tp_others（默认 OP 2 级），
     * 这样 LuckPerms 里已经给管理员配好的节点可以直接复用。
     */
    public static boolean isAdmin(CommandSourceStack source) {
        if (source.permissions().hasPermission(
            new Permission.HasCommandLevel(PermissionLevel.GAMEMASTERS))) {
            return true;
        }
        try {
            return ECPerms.check(source, ECPerms.Registry.home_tp_others, 2);
        } catch (Throwable ignored) {
            return false;
        }
    }
}
