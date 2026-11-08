package com.heihun.Itemshowcase.forge1201.server;

import com.heihun.Itemshowcase.core.model.SourceKind;
import com.heihun.Itemshowcase.core.spi.AccessorySlotData;
import com.heihun.Itemshowcase.core.spi.BackpackData;
import com.heihun.Itemshowcase.core.spi.ItemStackView;
import com.heihun.Itemshowcase.core.spi.PlayerDataViews;
import com.heihun.Itemshowcase.core.spi.SlotItem;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link PlayerDataViews} 的 Forge 1.20.1 实现：从服务端真实数据读取。
 *
 * <p>必须在服务器线程上构建与使用（ServerChatEvent / 网络主线程处理器均满足）。</p>
 *
 * <p>饰品 API（[c]）与容器物品集成（[pack]）在阶段三接入：当前返回空数据，
 * 对应宏将给出“未找到”提示而不是崩溃。</p>
 */
public final class ForgeDataViews implements PlayerDataViews {

    private final ServerPlayer player;

    public ForgeDataViews(ServerPlayer player) {
        this.player = player;
    }

    /** ItemStack 的 common-core 视图包装。 */
    private static ItemStackView view(ItemStack stack) {
        return new ForgeItemStackView(stack);
    }

    private static SlotItem slot(ItemStack stack, int index, SourceKind source) {
        return new SlotItem(view(stack), index, source);
    }

    @Override
    public SlotItem getMainHand() {
        return slot(player.getMainHandItem(), player.getInventory().selected, SourceKind.HAND);
    }

    @Override
    public List<SlotItem> getInventory() {
        Inventory inventory = player.getInventory();
        List<SlotItem> result = new ArrayList<>(inventory.items.size() + inventory.armor.size() + 1);
        // 显示顺序：快捷栏 → 主背包 → 盔甲 → 副手
        for (int i = 0; i < inventory.items.size(); i++) {
            result.add(slot(inventory.items.get(i), i, SourceKind.INVENTORY));
        }
        for (int i = 0; i < inventory.armor.size(); i++) {
            result.add(slot(inventory.armor.get(i), i, SourceKind.ARMOR));
        }
        result.add(slot(inventory.offhand.get(0), 0, SourceKind.OFFHAND));
        return result;
    }

    @Override
    public List<SlotItem> getEnderChest() {
        // Player.getEnderChestInventory()：ServerPlayer 上为服务端真实末影箱数据
        net.minecraft.world.inventory.PlayerEnderChestContainer enderChest = player.getEnderChestInventory();
        List<SlotItem> result = new ArrayList<>(enderChest.getContainerSize());
        for (int i = 0; i < enderChest.getContainerSize(); i++) {
            result.add(slot(enderChest.getItem(i), i, SourceKind.ENDER_CHEST));
        }
        return result;
    }

    @Override
    public List<AccessorySlotData> getAccessories() {
        // 阶段三接入：Curios / Trinkets / Accessories 集成
        return List.of();
    }

    @Override
    public List<BackpackData> getEquippedBackpacks() {
        // 阶段三接入：饰品 API + ItemContainerProvider
        return List.of();
    }

    @Override
    public BackpackData inspectBackpack(SlotItem stack, int maxDepth) {
        // 阶段三接入：SophisticatedBackpacksIntegration 等容器 Provider
        return null;
    }

    /** Forge 1.20.1 的 ItemStackView 实现。 */
    static final class ForgeItemStackView implements ItemStackView {

        private final ItemStack stack;

        ForgeItemStackView(ItemStack stack) {
            this.stack = stack;
        }

        @Override
        public Object platformStack() {
            return stack;
        }

        @Override
        public int getCount() {
            return stack.getCount();
        }

        @Override
        public boolean isEmpty() {
            return stack.isEmpty();
        }

        @Override
        public String itemId() {
            return stack.getItem().builtInRegistryHolder().key().location().toString();
        }

        @Override
        public String plainDisplayName() {
            return stack.getHoverName().getString();
        }

        @Override
        public com.heihun.Itemshowcase.core.protocol.ItemStackPayload toPayload() {
            return ItemStackCodec.encode(stack);
        }
    }
}
