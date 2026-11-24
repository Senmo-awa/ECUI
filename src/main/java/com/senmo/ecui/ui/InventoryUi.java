package com.senmo.ecui.ui;

import com.senmo.ecui.EcUiConfig;
import com.senmo.ecui.EcUiMod;
import com.senmo.ecui.Lang;
import com.senmo.ecui.Texts;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;

/**
 * 箱子式物品栏 UI。
 *
 * <p>每一项是一个带 {@code ec_ui} 自定义数据的物品；玩家把物品拿起来（左键/右键/Shift 点击）
 * 时，服务端 {@link SimpleContainer#removeItem(int, int)} 会被调用，我们据此判断玩家选了哪一项。
 * 选中后关闭箱子、清掉标记物品，再执行对应的 EC 命令。</p>
 *
 * <p>这样不需要客户端 mod，也不依赖原版对话框 API；1.21.5 及更早版本可以只靠这条路径。</p>
 */
public final class InventoryUi {
    private static final String MARKER = "ec_ui";
    private static final String SESSION = "session";
    private static final String SLOT = "slot";
    private static final String KIND = "kind";

    private InventoryUi() {}

    /**
     * @param item   显示的物品
     * @param name   物品名（已本地化）
     * @param lore   说明行（已本地化）
     * @param action 玩家拿走物品后执行的动作
     */
    public record Entry(Item item, Component name, List<Component> lore, Runnable action) {
        public Entry {
            if (item == null) {
                item = Items.PAPER;
            }
            if (name == null) {
                name = Component.empty();
            }
            if (lore == null) {
                lore = List.of();
            }
            if (action == null) {
                action = () -> {};
            }
        }
    }

    public static void open(ServerPlayer player, Component title, List<Entry> entries) {
        open(player, title, entries, 0);
    }

    public static void open(ServerPlayer player, Component title, List<Entry> entries, int page) {
        try (Lang.Scope ignored = Lang.open(player)) {
            if (entries == null || entries.isEmpty()) {
                player.sendSystemMessage(Texts.inventoryEmpty());
                return;
            }

            EcUiConfig config = EcUiConfig.get();
            int rows = config.inventoryRows == 3 ? 3 : 6;
            int contentRows = rows - 1;
            int pageSize = contentRows * 9;
            int pages = Math.max(1, (entries.size() + pageSize - 1) / pageSize);
            int current = Math.max(0, Math.min(page, pages - 1));
            int start = current * pageSize;
            int end = Math.min(entries.size(), start + pageSize);

            UUID session = UUID.randomUUID();
            int size = rows * 9;
            List<ItemStack> items = new ArrayList<>(Collections.nCopies(size, ItemStack.EMPTY));
            List<Runnable> actions = new ArrayList<>(Collections.nCopies(size, null));

            for (int i = start; i < end; i++) {
                int slot = i - start;
                Entry entry = entries.get(i);
                items.set(slot, marked(entry.item(), entry.name(), entry.lore(), session, slot, "entry"));
                actions.set(slot, entry.action());
            }

            int navRow = (rows - 1) * 9;
            if (current > 0) {
                Entry prev = new Entry(Items.ARROW, Texts.inventoryPrev(), List.of(),
                    () -> open(player, title, entries, current - 1));
                items.set(navRow, marked(prev.item(), prev.name(), prev.lore(), session, navRow, "nav"));
                actions.set(navRow, prev.action());
            }

            Entry pageEntry = new Entry(Items.PAPER, Texts.inventoryPage(current + 1, pages),
                List.of(Texts.inventoryPageLore(current + 1, pages)), () -> {});
            items.set(navRow + 1, marked(pageEntry.item(), pageEntry.name(), pageEntry.lore(),
                session, navRow + 1, "page"));
            actions.set(navRow + 1, pageEntry.action());

            if (current + 1 < pages) {
                Entry next = new Entry(Items.ARROW, Texts.inventoryNext(), List.of(),
                    () -> open(player, title, entries, current + 1));
                items.set(navRow + 2, marked(next.item(), next.name(), next.lore(), session, navRow + 2, "nav"));
                actions.set(navRow + 2, next.action());
            }

            Entry close = new Entry(Items.BARRIER, Texts.inventoryClose(), List.of(),
                player::closeContainer);
            items.set(navRow + 8, marked(close.item(), close.name(), close.lore(), session, navRow + 8, "close"));
            actions.set(navRow + 8, close.action());

            SelectableContainer container = new SelectableContainer(items, Set.of(navRow + 1),
                slot -> handleTake(player, session, slot, actions));
            MenuType<?> type = rows == 3 ? MenuType.GENERIC_9x3 : MenuType.GENERIC_9x6;

            OptionalInt result = player.openMenu(new SimpleMenuProvider(
                (syncId, inventory, p) ->
                    new SelectableChestMenu(type, syncId, inventory, container, rows, container),
                title));
            if (result.isEmpty()) {
                EcUiMod.LOGGER.warn("无法为 {} 打开物品栏 UI（openMenu 返回空）", player.getScoreboardName());
                player.sendSystemMessage(Texts.inventoryUnavailable());
            }
        } catch (Throwable throwable) {
            EcUiMod.LOGGER.error("打开物品栏 UI 失败", throwable);
            player.sendSystemMessage(Texts.inventoryUnavailable());
        }
    }

    private static void handleTake(ServerPlayer player, UUID session, int slot,
                                   List<Runnable> actions) {
        Runnable action = slot >= 0 && slot < actions.size() ? actions.get(slot) : null;
        player.level().getServer().execute(() -> {
            player.closeContainer();
            clearMarkers(player, session);
            if (action != null) {
                try {
                    action.run();
                } catch (Throwable throwable) {
                    EcUiMod.LOGGER.error("执行物品栏 UI 动作失败", throwable);
                }
            }
            // Closing a chest can return the carried stack to the inventory one tick later.
            player.level().getServer().execute(() -> clearMarkers(player, session));
        });
    }

    private static ItemStack marked(Item item, Component name, List<Component> lore,
                                    UUID session, int slot, String kind) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.CUSTOM_NAME, name);
        if (lore != null && !lore.isEmpty()) {
            stack.set(DataComponents.LORE, new ItemLore(lore));
        }

        CompoundTag tag = new CompoundTag();
        tag.putInt(MARKER, 1);
        tag.putString(SESSION, session.toString());
        tag.putInt(SLOT, slot);
        tag.putString(KIND, kind);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return stack;
    }

    private static boolean isMarker(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return false;
        }
        CompoundTag tag = data.copyTag();
        return tag.getIntOr(MARKER, 0) == 1 && tag.contains(SESSION);
    }

    public static void clearMarkers(ServerPlayer player, UUID session) {
        String expected = session.toString();
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (isSessionMarker(stack, expected)) {
                player.getInventory().setItem(i, ItemStack.EMPTY);
            }
        }
        ItemStack carried = player.containerMenu.getCarried();
        if (isSessionMarker(carried, expected)) {
            player.containerMenu.setCarried(ItemStack.EMPTY);
        }
    }

    private static boolean isSessionMarker(ItemStack stack, String session) {
        if (!isMarker(stack)) {
            return false;
        }
        CompoundTag tag = stack.get(DataComponents.CUSTOM_DATA).copyTag();
        return session.equals(tag.getStringOr(SESSION, ""));
    }

    /**
     * 只允许拿走带标记的物品；不能往菜单里放东西，避免玩家把箱子当成免费仓库。
     */
    private static final class SelectableContainer extends SimpleContainer {
        private final Set<Integer> locked;
        private final java.util.function.IntConsumer onTake;
        private boolean active = true;

        private SelectableContainer(List<ItemStack> items, Set<Integer> locked,
                                    java.util.function.IntConsumer onTake) {
            super(items.size());
            this.locked = locked;
            this.onTake = onTake;
            for (int i = 0; i < items.size(); i++) {
                super.setItem(i, items.get(i));
            }
        }

        @Override
        public ItemStack removeItem(int slot, int amount) {
            if (!active || slot < 0 || slot >= getContainerSize() || locked.contains(slot)) {
                return ItemStack.EMPTY;
            }
            ItemStack current = getItem(slot);
            if (!isMarker(current)) {
                return ItemStack.EMPTY;
            }
            ItemStack result = super.removeItem(slot, amount);
            if (!result.isEmpty()) {
                active = false;
                onTake.accept(slot);
            }
            return result;
        }

        @Override
        public boolean canPlaceItem(int slot, ItemStack stack) {
            return false;
        }

        @Override
        public boolean canTakeItem(net.minecraft.world.Container container, int slot, ItemStack stack) {
            return !locked.contains(slot) && isMarker(stack);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        private void deactivate() {
            active = false;
        }
    }

    /**
     * 正常的箱子菜单，只是在关闭时先把容器标记为失效，避免死亡/关闭时的 clearContainer
     * 被误判成玩家选择。
     */
    private static final class SelectableChestMenu extends ChestMenu {
        private final SelectableContainer selectable;

        private SelectableChestMenu(MenuType<?> type, int syncId,
                                    net.minecraft.world.entity.player.Inventory inventory,
                                    SelectableContainer container, int rows,
                                    SelectableContainer selectable) {
            super(type, syncId, inventory, container, rows);
            this.selectable = selectable;
        }

        @Override
        public void removed(net.minecraft.world.entity.player.Player player) {
            selectable.deactivate();
            super.removed(player);
        }
    }
}
