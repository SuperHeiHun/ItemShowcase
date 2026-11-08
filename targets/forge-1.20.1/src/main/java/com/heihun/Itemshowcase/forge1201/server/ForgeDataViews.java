package com.heihun.Itemshowcase.forge1201.server;

import com.heihun.Itemshowcase.core.logging.ItemShowcaseLogger;
import com.heihun.Itemshowcase.core.model.SourceKind;
import com.heihun.Itemshowcase.core.spi.AccessoryProvider;
import com.heihun.Itemshowcase.core.spi.AccessorySlotData;
import com.heihun.Itemshowcase.core.spi.BackpackData;
import com.heihun.Itemshowcase.core.spi.IntegrationRegistry;
import com.heihun.Itemshowcase.core.spi.ItemContainerProvider;
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
 * <p>饰品（[c]）通过 {@link AccessoryProvider} 集成读取（Curios 已接入，
 * 未安装任何饰品 API 时返回空列表）；容器物品（[pack] 与 [i] 展开）通过
 * {@link ItemContainerProvider} 集成识别（Sophisticated Backpacks 已接入）。
 * 全部不硬编码槽位名或物品 ID。</p>
 */
public final class ForgeDataViews implements PlayerDataViews {

    private final ServerPlayer player;
    private final IntegrationRegistry integrations;

    public ForgeDataViews(ServerPlayer player, IntegrationRegistry integrations) {
        this.player = player;
        this.integrations = integrations;
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
        List<AccessorySlotData> result = new ArrayList<>();
        for (AccessoryProvider provider : integrations.accessoryProviders()) {
            try {
                result.addAll(provider.getSlots(player));
            } catch (Throwable providerFailure) {
                // 单个饰品 API 故障不影响其他 API 与整个消息
                ItemShowcaseLogger.warn("饰品 API 读取失败: " + provider.id(), providerFailure);
            }
        }
        return result;
    }

    @Override
    public List<BackpackData> getEquippedBackpacks() {
        List<BackpackData> result = new ArrayList<>();
        for (AccessorySlotData accessory : getAccessories()) {
            if (accessory.isEmpty()) {
                continue;
            }
            // 识别能提供内部库存的背包：按 ItemContainerProvider 能力判断，
            // 不假定饰品槽位名称（back/backpack 等都不假设）
            ItemContainerProvider provider = integrations.findContainerProvider(accessory.stack());
            if (provider == null) {
                continue;
            }
            try {
                List<ItemStackView> contents = provider.getContents(accessory.stack(), 1);
                if (!contents.isEmpty()) {
                    result.add(new BackpackData(accessory.stack(), contents, provider.id()));
                }
            } catch (Throwable providerFailure) {
                ItemShowcaseLogger.warn("装备背包内部库存读取失败: " + provider.id(), providerFailure);
            }
        }
        return result;
    }

    @Override
    public BackpackData inspectBackpack(SlotItem stack, int maxDepth) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        ItemContainerProvider provider = integrations.findContainerProvider(stack.stack());
        if (provider == null) {
            return null;
        }
        try {
            List<ItemStackView> contents = provider.getContents(stack.stack(), maxDepth);
            return new BackpackData(stack.stack(), contents, provider.id());
        } catch (Throwable providerFailure) {
            ItemShowcaseLogger.warn("容器物品检查失败: " + provider.id(), providerFailure);
            return null;
        }
    }

    /** Forge 1.20.1 的 ItemStackView 实现（集成层也使用）。 */
    public static final class ForgeItemStackView implements ItemStackView {

        private final ItemStack stack;

        public ForgeItemStackView(ItemStack stack) {
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
