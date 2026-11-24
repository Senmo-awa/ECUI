package com.senmo.ecui;

import com.fibermc.essentialcommands.commands.suggestions.WarpSuggestion;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import com.senmo.ecui.ui.HomeUi;
import com.senmo.ecui.ui.WarpUi;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.server.MinecraftServer;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

/**
 * 把 EC 已经注册好的 /home、/warp 节点的“无参数执行”换成打开对话框。
 *
 * <p>不重建命令树：/home &lt;名字&gt;、/warp &lt;名字&gt;、/essentialcommands ...
 * 以及 EC 自己的权限/补全逻辑全部保持原样，只是裸 /home、/warp 变成 UI 入口。</p>
 *
 * <p>另外顺手修一处 EC 的补全缺陷：EC 的 {@code WarpSuggestion} 会调用
 * {@code getAccessibleWarps}，后者对每个传送点拼 {@code Identifier}——
 * 26.3 只允许 {@code [a-z0-9/._-]}，名字带大写字母就抛
 * {@code IdentifierException}，表现为「每按一次补全刷一次 ERROR」。
 * 我们只把<b>同一个 provider 实例</b>替换掉（按对象身份匹配，不碰其它补全）。</p>
 *
 * <p>反射拿的是 Brigadier 内部字段，跨版本可能改名——所以一律<b>软失败</b>：
 * 找不到字段就记一条错误日志、这个增强不生效，绝不把服务器带崩。</p>
 */
public final class EcCommandHook {
    /** 与 EC 的 WarpSuggestion.STRING_SUGGESTIONS_PROVIDER 同身份，用于定位。 */
    private static final SuggestionProvider<CommandSourceStack> WARP_SUGGESTIONS =
        (context, builder) -> SharedSuggestionProvider.suggest(
            WarpUi.suggestableNames(context.getSource()), builder);

    private EcCommandHook() {}

    public static void register() {
        ServerLifecycleEvents.SERVER_STARTED.register(EcCommandHook::install);
        // /reload 之后如果命令树被重建，再接管一次（幂等）。
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resourceManager, success) -> install(server));
    }

    private static void install(MinecraftServer server) {
        CommandDispatcher<CommandSourceStack> dispatcher = server.getCommands().getDispatcher();
        hook(dispatcher, "home", context -> HomeUi.open(context.getSource()));
        hook(dispatcher, "warp", context -> WarpUi.open(context.getSource()));
        patchWarpSuggestions(dispatcher);
    }

    private static void hook(CommandDispatcher<CommandSourceStack> dispatcher, String name,
                             Command<CommandSourceStack> command) {
        CommandNode<CommandSourceStack> node = dispatcher.getRoot().getChild(name);
        if (node == null) {
            // EC 的 enable_home / enable_warp 关掉时节点本来就不存在，属于正常情况。
            EcUiMod.LOGGER.info("没有找到 /{} 节点，跳过 UI 接管（可能被 EC 配置禁用）。", name);
            return;
        }

        Field field = findField(CommandNode.class, "command");
        if (field == null) {
            EcUiMod.LOGGER.error("CommandNode.command 字段不存在（Brigadier 版本变了），/{} 的 UI 不会生效。", name);
            return;
        }
        try {
            field.set(node, command);
            EcUiMod.LOGGER.info("已接管 /{} 的无参数执行（打开 UI）。", name);
        } catch (ReflectiveOperationException ex) {
            EcUiMod.LOGGER.error("无法替换 /{} 的执行器，UI 不会生效。", name, ex);
        }
    }

    /** 把 EC 那个会抛异常的传送点补全换成我们自己的（按实例身份匹配）。 */
    private static void patchWarpSuggestions(CommandDispatcher<CommandSourceStack> dispatcher) {
        Field field = findField(ArgumentCommandNode.class, "customSuggestions");
        if (field == null) {
            EcUiMod.LOGGER.warn("ArgumentCommandNode.customSuggestions 字段不存在，跳过补全修复（补全仍可能刷 ERROR）。");
            return;
        }
        Object target;
        try {
            target = WarpSuggestion.STRING_SUGGESTIONS_PROVIDER;
        } catch (Throwable throwable) {
            EcUiMod.LOGGER.warn("读不到 EC 的传送点补全提供器，跳过补全修复。", throwable);
            return;
        }
        int replaced = walkAndPatch(dispatcher.getRoot(), new HashSet<>(), field, target);
        if (replaced > 0) {
            EcUiMod.LOGGER.info("已替换 {} 处传送点补全（原补全在名字含大写字母时会抛 IdentifierException）。", replaced);
        }
    }

    private static int walkAndPatch(CommandNode<CommandSourceStack> node,
                                    Set<CommandNode<CommandSourceStack>> seen,
                                    Field field, Object target) {
        if (!seen.add(node)) {
            return 0;
        }
        int replaced = 0;
        if (node instanceof ArgumentCommandNode<?, ?> argument) {
            try {
                if (field.get(argument) == target) {
                    field.set(argument, WARP_SUGGESTIONS);
                    replaced++;
                }
            } catch (ReflectiveOperationException ex) {
                EcUiMod.LOGGER.warn("替换传送点补全失败", ex);
            }
        }
        for (CommandNode<CommandSourceStack> child : node.getChildren()) {
            replaced += walkAndPatch(child, seen, field, target);
        }
        return replaced;
    }

    private static Field findField(Class<?> owner, String name) {
        try {
            Field field = owner.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException ex) {
            return null;
        }
    }
}
