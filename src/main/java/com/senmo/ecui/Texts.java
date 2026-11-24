package com.senmo.ecui;

import com.fibermc.essentialcommands.types.MinecraftLocation;
import com.senmo.ecui.util.Dimensions;
import com.senmo.ecui.util.Fmt;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * 双语文本。服务端只能发字面文本（客户端不一定有我们的语言文件），
 * 所以这里通过 {@link Lang} 在服务端解析，再发送给客户端。
 */
public final class Texts {
    private Texts() {}

    private static MutableComponent c(String key, Object... args) {
        return Component.literal(Lang.tr(key, args));
    }

    public static Component homeTitle(int count) {
        return c("ui.home.title").withStyle(ChatFormatting.GOLD)
            .append(Component.literal("  ·  " + count).withStyle(ChatFormatting.GRAY));
    }

    public static Component warpTitle(int count) {
        return c("ui.warp.title").withStyle(ChatFormatting.AQUA)
            .append(Component.literal("  ·  " + count).withStyle(ChatFormatting.GRAY));
    }

    public static Component nearbyTitle(int count) {
        return c("ui.nearby.title").withStyle(ChatFormatting.LIGHT_PURPLE)
            .append(Component.literal("  ·  " + count).withStyle(ChatFormatting.GRAY));
    }

    public static Component close() {
        return c("ui.close").withStyle(ChatFormatting.GRAY);
    }

    public static Component playerOnly() {
        return c("ui.player_only").withStyle(ChatFormatting.RED);
    }

    public static Component noPermission() {
        return c("ui.no_permission").withStyle(ChatFormatting.RED);
    }

    public static Component homePermission() {
        return c("ui.permission_home").withStyle(ChatFormatting.RED);
    }

    public static Component warpPermission() {
        return c("ui.permission_warp").withStyle(ChatFormatting.RED);
    }

    public static Component homeDisabled() {
        return c("ui.home_disabled").withStyle(ChatFormatting.RED);
    }

    public static Component warpDisabled() {
        return c("ui.warp_disabled").withStyle(ChatFormatting.RED);
    }

    public static Component homeEmpty() {
        return c("ui.home_empty").withStyle(ChatFormatting.GRAY);
    }

    public static Component warpEmpty() {
        return c("ui.warp_empty").withStyle(ChatFormatting.GRAY);
    }

    public static Component warpError() {
        return c("ui.warp_error").withStyle(ChatFormatting.RED);
    }

    public static Component homeError() {
        return c("ui.home_error").withStyle(ChatFormatting.RED);
    }

    public static Component commandFailed() {
        return c("ui.command_failed").withStyle(ChatFormatting.RED);
    }

    public static Component dialogUnavailable() {
        return c("ui.dialog_unavailable").withStyle(ChatFormatting.RED);
    }

    public static Component inventoryUnavailable() {
        return c("ui.inventory_unavailable").withStyle(ChatFormatting.RED);
    }

    /**
     * 26.3 的 Identifier 只允许 [a-z0-9/._-]；EC 把传送点的 permissionString 拼进 Identifier，
     * 名字里带大写字母就会抛异常，这类传送点判不了权限，只给管理员显示。
     */
    public static Component warpPermissionBroken(int count) {
        return c("ui.warp_permission_broken", count).withStyle(ChatFormatting.DARK_GRAY);
    }

    public static Component nearbyEmpty(int radius) {
        return c("ui.nearby_empty", radius).withStyle(ChatFormatting.GRAY);
    }

    public static Component scanning(int radius) {
        return c("ui.scanning", radius).withStyle(ChatFormatting.GRAY);
    }

    public static Component scanFailed() {
        return c("ui.scan_failed").withStyle(ChatFormatting.RED);
    }

    public static Component homeBody(boolean truncated) {
        MutableComponent body = c("ui.home_body").withStyle(ChatFormatting.GRAY);
        if (truncated) {
            body.append(Component.literal("\n"))
                .append(c("ui.home_truncated").withStyle(ChatFormatting.DARK_GRAY));
        }
        return body;
    }

    public static Component warpBody(boolean truncated) {
        MutableComponent body = c("ui.warp_body").withStyle(ChatFormatting.GRAY);
        if (truncated) {
            body.append(Component.literal("\n"))
                .append(c("ui.warp_truncated").withStyle(ChatFormatting.DARK_GRAY));
        }
        return body;
    }

    public static Component nearbyBody(int radius, ResourceKey<Level> dim, int total, int shown) {
        MutableComponent body = c("ui.nearby_body", radius, Dimensions.of(dim), total)
            .withStyle(ChatFormatting.GRAY);
        body.append(Component.literal("\n"))
            .append(c("ui.nearby_body_hint").withStyle(ChatFormatting.DARK_GRAY));
        if (shown < total) {
            body.append(Component.literal("\n"))
                .append(c("ui.nearby_truncated", shown).withStyle(ChatFormatting.DARK_GRAY));
        }
        return body;
    }

    public static Component homeButton(String name) {
        return Component.literal(Lang.tr("ui.home_button_prefix")).withStyle(ChatFormatting.GRAY)
            .append(Component.literal(name).withStyle(ChatFormatting.GOLD));
    }

    public static Component warpButton(String name) {
        return Component.literal(Lang.tr("ui.warp_button_prefix")).withStyle(ChatFormatting.GRAY)
            .append(Component.literal(name).withStyle(ChatFormatting.AQUA));
    }

    public static Component nearbyButton(String owner, String home) {
        return Component.literal(owner).withStyle(ChatFormatting.GOLD)
            .append(Component.literal(" · ").withStyle(ChatFormatting.DARK_GRAY))
            .append(Component.literal(home).withStyle(ChatFormatting.WHITE));
    }

    public static Component nearbyButtonAdmin() {
        return c("ui.nearby_admin").withStyle(ChatFormatting.LIGHT_PURPLE);
    }

    public static Component nearbyButtonAdminTooltip() {
        return c("ui.nearby_admin_tooltip").withStyle(ChatFormatting.GRAY);
    }

    public static Component reloadButton() {
        return c("ui.reload").withStyle(ChatFormatting.AQUA);
    }

    public static Component switchToWarps() {
        return c("ui.switch_warps").withStyle(ChatFormatting.AQUA);
    }

    public static Component switchToHomes() {
        return c("ui.switch_homes").withStyle(ChatFormatting.GOLD);
    }

    public static Component switchTooltip() {
        return c("ui.switch_tooltip").withStyle(ChatFormatting.GRAY);
    }

    public static Component reloadButtonTooltip(int radius) {
        return c("ui.reload_tooltip", radius).withStyle(ChatFormatting.GRAY);
    }

    public static Component locationTooltip(MinecraftLocation loc, double distance) {
        return Component.empty()
            .append(locationLine1(loc))
            .append(Component.literal("\n"))
            .append(locationLine2(distance));
    }

    public static List<Component> locationLore(MinecraftLocation loc, double distance) {
        List<Component> lines = new ArrayList<>(3);
        lines.add(locationLine1(loc));
        lines.add(locationLine2(distance));
        lines.add(c("ui.inventory.hint").withStyle(ChatFormatting.GREEN));
        return lines;
    }

    public static Component locationLine1(MinecraftLocation loc) {
        return c("ui.location_line1", Dimensions.of(loc.dim()),
            Fmt.coord(loc.x()), Fmt.coord(loc.y()), Fmt.coord(loc.z()))
            .withStyle(ChatFormatting.GRAY);
    }

    public static Component locationLine2(double distance) {
        return c("ui.location_line2", Fmt.distance(distance)).withStyle(ChatFormatting.DARK_GRAY);
    }

    public static Component safeTeleportHint(Component base) {
        return Component.empty().append(base).append(Component.literal("\n"))
            .append(c("ui.safe_hint").withStyle(ChatFormatting.GREEN));
    }

    public static Component unsafeNameTooltip(Component base) {
        return Component.empty().append(base).append(Component.literal("\n"))
            .append(c("ui.unsafe").withStyle(ChatFormatting.RED));
    }

    public static Component unknownOwnerTooltip(Component base) {
        return Component.empty().append(base).append(Component.literal("\n"))
            .append(c("ui.unknown_owner").withStyle(ChatFormatting.RED));
    }

    public static Component reloaded() {
        return c("ui.reloaded").withStyle(ChatFormatting.GREEN);
    }

    public static Component ecuiHelp() {
        return c("ui.help").withStyle(ChatFormatting.GRAY);
    }

    public static Component inventoryPage(int page, int pages) {
        return c("ui.inventory.page", page, pages).withStyle(ChatFormatting.WHITE);
    }

    public static Component inventoryPageLore(int page, int pages) {
        return c("ui.inventory.page_tooltip", page, pages).withStyle(ChatFormatting.GRAY);
    }

    public static Component inventoryPrev() {
        return c("ui.inventory.prev").withStyle(ChatFormatting.YELLOW);
    }

    public static Component inventoryNext() {
        return c("ui.inventory.next").withStyle(ChatFormatting.YELLOW);
    }

    public static Component inventoryClose() {
        return c("ui.inventory.close").withStyle(ChatFormatting.RED);
    }

    public static Component inventoryEmpty() {
        return c("ui.inventory.empty").withStyle(ChatFormatting.GRAY);
    }
}
