package com.heihun.Itemshowcase.neoforge1211.server;

import com.heihun.Itemshowcase.core.logging.ItemShowcaseLogger;
import com.heihun.Itemshowcase.core.model.SourceKind;
import com.heihun.Itemshowcase.core.protocol.ItemStackPayload;
import com.heihun.Itemshowcase.core.spi.AccessoryProvider;
import com.heihun.Itemshowcase.core.spi.AccessorySlotData;
import com.heihun.Itemshowcase.core.spi.BackpackData;
import com.heihun.Itemshowcase.core.spi.IntegrationRegistry;
import com.heihun.Itemshowcase.core.spi.ItemContainerProvider;
import com.heihun.Itemshowcase.core.spi.ItemStackView;
import com.heihun.Itemshowcase.core.spi.PlayerDataViews;
import com.heihun.Itemshowcase.core.spi.SlotItem;
import net.minecraft.core.HolderLookup;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link PlayerDataViews} 的 NeoForge 1.21.1 实现：从服务端真实数据读取。
 *
 * <p>必须在服务器线程上构建与使用（ServerChatEvent / 网络主线程处理器均满足）。</p>
 *
 * <p>饰品（[c]）与容器物品（[pack] / [i] 展开）通过集成注册表动态读取，
 * 不硬编码任何槽位名或物品 ID；API 未安装时对应方法返回空数据。</p>
 */
public final class NeoDataViews implements PlayerDataViews {

    private final ServerPlayer player;
    private final IntegrationRegistry integrations;
    /** 1.21.1 物品编码需要注册表访问（Data Components）。 */
    private final HolderLookup.Provider registries;

    public NeoDataViews(ServerPlayer player, IntegrationRegistry integrations, HolderLookup.Provider registries) {
        this.player = player;
        this.integrations = integrations;
        this.registries = registries;
    }

    private ItemStackView view(ItemStack stack) {
        return new NeoItemStackView(stack, registries);
    }

    private SlotItem slot(ItemStack stack, int index, SourceKind source) {
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
        // 显示顺序：快捷栏 → 主背包 → 盔甲 → 副手（slotIndex 为原库存槽位号）
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
        // ServerPlayer 上为服务端真实末影箱数据
        var enderChest = player.getEnderChestInventory();
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

    /** NeoForge 1.21.1 的 ItemStackView 实现（1.21 物品编码需要注册表）。 */
    public static final class NeoItemStackView implements ItemStackView {

        private final ItemStack stack;
        private final HolderLookup.Provider registries;

        public NeoItemStackView(ItemStack stack, HolderLookup.Provider registries) {
            this.stack = stack;
            this.registries = registries;
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
        public ItemStackPayload toPayload() {
            return ItemStackCodec.encode(stack, registries);
        }
    }
}
