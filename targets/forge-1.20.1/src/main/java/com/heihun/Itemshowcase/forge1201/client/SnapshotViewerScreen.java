package com.heihun.Itemshowcase.forge1201.client;

import com.heihun.Itemshowcase.core.logging.ItemShowcaseLogger;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.SimpleContainer;
import net.minecraft.util.Mth;

import java.util.List;

/**
 * 快照查看 GUI：直接使用<b>原版箱子界面</b>（ChestScreen + ChestMenu）展示物品，
 * 行数按物品数量自动选择 9x1 ~ 9x6，渲染、槽位格子、玩家背包栏全部为原版实现。
 *
 * <p>只读保证：自定义菜单覆写 {@link #clicked} 为空实现——所有点击/拖拽/
 * shift 移动都被忽略，不会产生任何物品移动、复制或库存修改，
 * 也不会向服务端发送 container click 包（本菜单为纯客户端本地展示）。</p>
 */
public final class SnapshotViewerScreen {

    private SnapshotViewerScreen() {
    }

    /** 打开快照查看界面（仅客户端）。 */
    public static void open(String entryTitle, List<ShowcaseMessageHandler.DisplayItem> items) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            return;
        }
        int rows = Mth.clamp((items.size() + COLUMNS - 1) / COLUMNS, 1, MAX_ROWS);
        SimpleContainer container = new SimpleContainer(rows * COLUMNS);
        for (int i = 0; i < Math.min(items.size(), rows * COLUMNS); i++) {
            ItemStack stack = items.get(i).stack();
            // 深拷贝到展示容器：展示数据与真实库存完全隔离
            container.setItem(i, stack.isEmpty() ? ItemStack.EMPTY : stack.copy());
        }
        ChestMenu menu = new ReadOnlyChestMenu(menuTypeFor(rows), LOCAL_CONTAINER_ID,
                minecraft.player.getInventory(), container, rows);
        Component title = Component.translatable("itemshowcase.gui.snapshot_title",
                entryTitle, items.size());
        minecraft.setScreen(new ContainerScreen(menu, minecraft.player.getInventory(), title));
    }

    private static final int COLUMNS = 9;
    private static final int MAX_ROWS = 6;
    /** 本地展示菜单 ID：与真实服务器菜单无关联（点击被拦截，不发包）。 */
    private static final int LOCAL_CONTAINER_ID = 1;

    private static MenuType<?> menuTypeFor(int rows) {
        switch (rows) {
            case 1: return MenuType.GENERIC_9x1;
            case 2: return MenuType.GENERIC_9x2;
            case 3: return MenuType.GENERIC_9x3;
            case 4: return MenuType.GENERIC_9x4;
            case 5: return MenuType.GENERIC_9x5;
            default: return MenuType.GENERIC_9x6;
        }
    }

    /** 只读箱子菜单：吞掉全部点击操作。 */
    private static final class ReadOnlyChestMenu extends ChestMenu {

        ReadOnlyChestMenu(MenuType<?> type, int containerId, Inventory playerInventory,
                          SimpleContainer container, int rows) {
            super(type, containerId, playerInventory, container, rows);
        }

        @Override
        public void clicked(int slotId, int button, ClickType clickType, Player player) {
            // 展示专用：忽略一切点击（拿取/放置/拖拽/quickmove 全部禁止）
        }

        @Override
        public boolean stillValid(Player player) {
            // 纯客户端本地菜单：始终有效
            return true;
        }

        @Override
        public ItemStack quickMoveStack(Player player, int index) {
            return ItemStack.EMPTY;
        }
    }
}
