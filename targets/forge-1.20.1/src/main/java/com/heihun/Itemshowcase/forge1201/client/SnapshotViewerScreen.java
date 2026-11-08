package com.heihun.Itemshowcase.forge1201.client;

import com.heihun.Itemshowcase.core.model.SourceKind;
import com.heihun.Itemshowcase.core.logging.ItemShowcaseLogger;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.SimpleContainer;
import net.minecraft.util.Mth;
import org.anti_ad.mc.ipn.api.IPNIgnore;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 快照查看 GUI：直接使用<b>原版箱子界面</b>（ChestMenu + ContainerScreen）展示物品，
 * 渲染、槽位格子、玩家背包栏全部为原版实现。
 *
 * <p>布局按入口类型分三种（参照 HBetterChat 插件的展示样式）：</p>
 * <ul>
 *   <li><b>背包（[inv]）</b>：9x6 —— 顶部两行装饰玻璃板（含玩家头颅）+
 *       盔甲/副手行 + 主背包 27 格 + 快捷栏 9 格。</li>
 *   <li><b>末影箱（[ec]）</b>：9x5 —— 顶部两行装饰（含玩家头颅）+ 27 格内容。</li>
 *   <li><b>其他容器（[pack] / [i] 容器展开）</b>：9xN 平铺，超过 54 格自动分页。</li>
 * </ul>
 *
 * <p>只读保证：菜单覆写 {@link #clicked} 为空实现——所有点击/拖拽/shift 移动都被
 * 忽略，不产生任何物品移动/复制，也不向服务端发送 container click 包。</p>
 *
 * <p>{@code @IPNIgnore}：Inventory Profiles Next 对本界面禁用（无排序按钮等注入）。
 * IPN 未安装时注解缺失被自动忽略，无副作用。</p>
 */
@IPNIgnore
public final class SnapshotViewerScreen extends ContainerScreen {

    private static final int COLUMNS = 9;
    private static final int MAX_ROWS = 6;

    /** 每页物品数（非背包/末影箱布局的平铺分页）。 */
    private static final int PAGE_SIZE = COLUMNS * MAX_ROWS;

    private final List<ItemStack> allItems;
    private final ItemStack[][] structuredSlots;
    private final int structuredRows;
    private final String senderName;
    private final UUID senderId;

    private int page = 0;
    private int totalPages = 1;
    private Button prevButton;
    private Button nextButton;

    private SnapshotViewerScreen(ChestMenu menu, Inventory playerInventory, Component title,
                                 List<ItemStack> allItems, ItemStack[][] structuredSlots, int structuredRows,
                                 String senderName, UUID senderId) {
        super(menu, playerInventory, title);
        this.allItems = allItems;
        this.structuredSlots = structuredSlots;
        this.structuredRows = structuredRows;
        this.senderName = senderName;
        this.senderId = senderId;
        this.totalPages = Math.max(1, (allItems.size() + PAGE_SIZE - 1) / PAGE_SIZE);
    }

    /** 打开快照查看界面（仅客户端；按入口类型选择布局）。 */
    public static void open(ShowcaseMessageHandler.ContainerEntry entry) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            return;
        }
        switch (entry.source()) {
            case INVENTORY -> openInventoryView(minecraft, entry);
            case ENDER_CHEST -> openEnderChestView(minecraft, entry);
            default -> openPagedFlatView(minecraft, entry);
        }
    }

    // ---------------- 背包布局（9x6，HBetterChat 插件样式） ----------------

    private static void openInventoryView(Minecraft minecraft, ShowcaseMessageHandler.ContainerEntry entry) {
        int rows = 6;
        SimpleContainer container = new SimpleContainer(rows * COLUMNS);
        ItemStack[][] slots = new ItemStack[rows][COLUMNS];

        // 顶部两行：装饰玻璃板（中间放玩家头颅）
        for (int i = 0; i < 2 * COLUMNS; i++) {
            slots[i / COLUMNS][i % COLUMNS] = decorationPane();
        }
        slots[0][1] = playerHead(entry.senderName(), entry.senderId());

        // 按原库存槽位号精确摆放（与真实库存位置一致；空槽位保持空）
        for (ShowcaseMessageHandler.DisplayItem item : entry.items()) {
            ItemStack stack = item.stack().isEmpty() ? ItemStack.EMPTY : item.stack().copy();
            int index = item.slotIndex();
            switch (item.source()) {
                case ARMOR -> {
                    // 原版 Inventory.armor 顺序为 0=脚、1=腿、2=胸、3=头（自下而上），
                    // GUI 装饰行 4-7 从左到右是 头/胸/腿/脚 → 映射反转
                    if (index >= 0 && index < 4) {
                        slots[0][7 - index] = stack;
                    }
                }
                case OFFHAND -> slots[0][8] = stack;
                case INVENTORY -> {
                    // 库存槽位号：0-8 快捷栏 → GUI 底行；9-35 主背包 → GUI 行 3-5
                    if (index >= 0 && index < 9) {
                        slots[5][index] = stack;
                    } else if (index >= 9 && index < 36) {
                        slots[2 + (index - 9) / COLUMNS][(index - 9) % COLUMNS] = stack;
                    }
                }
                default -> { /* 其他来源不属于背包布局 */ }
            }
        }

        fillContainer(container, slots);
        Component title = Component.translatable("itemshowcase.gui.inventory_title", entry.senderName());
        ChestMenu menu = new ReadOnlyChestMenu(MenuType.GENERIC_9x6, LOCAL_CONTAINER_ID,
                minecraft.player.getInventory(), container, rows);
        minecraft.setScreen(new SnapshotViewerScreen(menu, minecraft.player.getInventory(), title,
                List.of(), slots, rows, entry.senderName(), entry.senderId()));
    }

    // ---------------- 末影箱布局（9x5，HBetterChat 插件样式） ----------------

    private static void openEnderChestView(Minecraft minecraft, ShowcaseMessageHandler.ContainerEntry entry) {
        int rows = 5;
        SimpleContainer container = new SimpleContainer(rows * COLUMNS);
        ItemStack[][] slots = new ItemStack[rows][COLUMNS];

        for (int i = 0; i < 2 * COLUMNS; i++) {
            slots[i / COLUMNS][i % COLUMNS] = decorationPane();
        }
        slots[0][4] = playerHead(entry.senderName(), entry.senderId());

        // 按末影箱原槽位号（0-26）精确摆放：GUI 行 3-5，位置与真实末影箱一致
        for (ShowcaseMessageHandler.DisplayItem item : entry.items()) {
            if (item.source() != SourceKind.ENDER_CHEST || item.stack().isEmpty()) {
                continue;
            }
            int index = item.slotIndex();
            if (index >= 0 && index < 27) {
                slots[2 + index / COLUMNS][index % COLUMNS] = item.stack().copy();
            }
        }

        fillContainer(container, slots);
        Component title = Component.translatable("itemshowcase.gui.enderchest_title", entry.senderName());
        ChestMenu menu = new ReadOnlyChestMenu(MenuType.GENERIC_9x5, LOCAL_CONTAINER_ID,
                minecraft.player.getInventory(), container, rows);
        minecraft.setScreen(new SnapshotViewerScreen(menu, minecraft.player.getInventory(), title,
                List.of(), slots, rows, entry.senderName(), entry.senderId()));
    }

    // ---------------- 平铺 + 分页（[pack] / [i] 容器展开） ----------------

    private static void openPagedFlatView(Minecraft minecraft, ShowcaseMessageHandler.ContainerEntry entry) {
        List<ItemStack> items = new ArrayList<>();
        for (ShowcaseMessageHandler.DisplayItem item : entry.items()) {
            if (!item.stack().isEmpty()) {
                items.add(item.stack().copy());
            }
        }
        int rows = Mth.clamp((items.size() + COLUMNS - 1) / COLUMNS, 1, MAX_ROWS);
        SimpleContainer container = new SimpleContainer(rows * COLUMNS);
        ChestMenu menu = new ReadOnlyChestMenu(menuTypeFor(rows), LOCAL_CONTAINER_ID,
                minecraft.player.getInventory(), container, rows);
        Component title = Component.translatable("itemshowcase.gui.snapshot_title",
                entry.title(), items.size());
        SnapshotViewerScreen screen = new SnapshotViewerScreen(menu, minecraft.player.getInventory(), title,
                items, null, rows, entry.senderName(), entry.senderId());
        screen.applyPage(container);
        minecraft.setScreen(screen);
    }

    /** 将当前页物品写入容器。 */
    private void applyPage(SimpleContainer container) {
        container.clearContent();
        if (this.structuredSlots != null) {
            for (int row = 0; row < structuredRows; row++) {
                for (int col = 0; col < COLUMNS; col++) {
                    ItemStack stack = this.structuredSlots[row][col];
                    container.setItem(row * COLUMNS + col, stack == null ? ItemStack.EMPTY : stack);
                }
            }
            return;
        }
        int start = this.page * PAGE_SIZE;
        for (int i = 0; i < PAGE_SIZE; i++) {
            int index = start + i;
            container.setItem(i, index < this.allItems.size() ? this.allItems.get(index) : ItemStack.EMPTY);
        }
    }

    // ---------------- 共用辅助 ----------------

    private static void fillContainer(SimpleContainer container, ItemStack[][] slots) {
        for (int row = 0; row < slots.length; row++) {
            for (int col = 0; col < COLUMNS; col++) {
                ItemStack stack = slots[row][col];
                container.setItem(row * COLUMNS + col, stack == null ? ItemStack.EMPTY : stack);
            }
        }
    }

    /** 装饰玻璃板（浅蓝，无悬浮名）。 */
    private static ItemStack decorationPane() {
        ItemStack pane = new ItemStack(Items.LIGHT_BLUE_STAINED_GLASS_PANE);
        pane.setHoverName(Component.empty());
        return pane;
    }

    /** 玩家头颅（本地 SkullOwner NBT，客户端自行请求皮肤）。 */
    private static ItemStack playerHead(String name, UUID playerId) {
        ItemStack head = new ItemStack(Items.PLAYER_HEAD);
        try {
            CompoundTag owner = new CompoundTag();
            owner.putString("Name", name);
            if (playerId != null) {
                owner.put("Id", NbtUtils.createUUID(playerId));
            }
            CompoundTag tag = head.getOrCreateTag();
            tag.put("SkullOwner", owner);
        } catch (RuntimeException nbtFailure) {
            ItemShowcaseLogger.warn("玩家头颅构造失败", nbtFailure);
        }
        head.setHoverName(Component.literal(name).withStyle(ChatFormatting.GREEN));
        return head;
    }

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

    // ---------------- Screen 行为 ----------------

    @Override
    protected void init() {
        super.init();
        if (this.totalPages > 1) {
            this.prevButton = addRenderableWidget(Button.builder(
                    Component.literal("<"), button -> flipPage(-1))
                    .bounds(this.leftPos - 24, this.topPos + 4, 20, 20).build());
            this.nextButton = addRenderableWidget(Button.builder(
                    Component.literal(">"), button -> flipPage(1))
                    .bounds(this.leftPos + this.imageWidth + 4, this.topPos + 4, 20, 20).build());
        }
    }

    private void flipPage(int delta) {
        this.page = Mth.clamp(this.page + delta, 0, this.totalPages - 1);
        this.menu.setCarried(ItemStack.EMPTY);
        if (this.menu instanceof ReadOnlyChestMenu readOnlyMenu) {
            readOnlyMenu.refreshFrom(this::applyPageToContainer);
        }
        this.prevButton.active = this.page > 0;
        this.nextButton.active = this.page < this.totalPages - 1;
    }

    private void applyPageToContainer(SimpleContainer container) {
        applyPage(container);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.totalPages > 1) {
            if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT) {
                flipPage(-1);
                return true;
            }
            if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT) {
                flipPage(1);
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        if (this.totalPages > 1) {
            guiGraphics.drawCenteredString(this.font,
                    (this.page + 1) + " / " + this.totalPages,
                    this.leftPos + this.imageWidth / 2, this.topPos - 24, 0xFF404040);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** 本地展示菜单 ID：与真实服务器菜单无关联（点击被拦截，不发包）。 */
    private static final int LOCAL_CONTAINER_ID = 1;

    /** 只读箱子菜单：吞掉全部点击操作；支持翻页时回填数据。 */
    @IPNIgnore
    private static final class ReadOnlyChestMenu extends ChestMenu {

        private SimpleContainer backingContainer;

        ReadOnlyChestMenu(MenuType<?> type, int containerId, Inventory playerInventory,
                          SimpleContainer container, int rows) {
            super(type, containerId, playerInventory, container, rows);
            this.backingContainer = container;
        }

        void refreshFrom(java.util.function.Consumer<SimpleContainer> refiller) {
            refiller.accept(this.backingContainer);
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
