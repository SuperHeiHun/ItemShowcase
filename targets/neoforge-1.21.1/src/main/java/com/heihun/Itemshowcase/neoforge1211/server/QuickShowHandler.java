package com.heihun.Itemshowcase.neoforge1211.server;

import com.heihun.Itemshowcase.core.config.ItemShowcaseConfig;
import com.heihun.Itemshowcase.core.macros.MacroParser;
import com.heihun.Itemshowcase.core.model.ShowcaseMessage;
import com.heihun.Itemshowcase.core.model.ShowcaseMessageFactory;
import com.heihun.Itemshowcase.core.spi.SlotItem;
import com.heihun.Itemshowcase.neoforge1211.ShowcaseServices;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/**
 * Ctrl+Enter 快捷展示的服务端逻辑（NeoForge 1.21.1）。
 *
 * <p>安全模型：客户端只提交“菜单 ID + 槽位 ID”；服务端校验菜单一致性、
 * 槽位归属（必须为玩家自己的库存），随后从服务端重新读取真实 ItemStack——
 * 客户端提交的任何物品数据都不被信任。</p>
 */
public final class QuickShowHandler {

    private QuickShowHandler() {
    }

    /**
     * 批量处理：Alt+Enter 发送的全部选中槽位。
     * 每个槽位独立校验（菜单一致性 + 槽位归属），有效物品合并为一条消息广播。
     */
    public static void handleRequest(ServerPlayer player, int containerId, java.util.List<Integer> slotIndices) {
        ItemShowcaseConfig.Server config = ShowcaseServices.serverConfig();
        if (!config.allowCtrlEnterShowcase) {
            BroadcastService.notice(player, "itemshowcase.chat.quick_show_disabled");
            return;
        }
        // 1. 菜单一致性：玩家当前打开的菜单必须是请求中声明的那个
        AbstractContainerMenu menu = player.containerMenu;
        if (menu == null || menu.containerId != containerId) {
            return;
        }
        if (slotIndices == null || slotIndices.isEmpty()) {
            return;
        }
        if (slotIndices.size() > com.heihun.Itemshowcase.neoforge1211.network.SelectedItemRequestPayload.MAX_SLOTS) {
            return;
        }
        // 2. 频率限制（整批一次）
        if (!ShowcaseServices.rateLimiter().tryAcquire(player.getUUID())) {
            BroadcastService.notice(player, "itemshowcase.chat.rate_limited");
            return;
        }
        // 3. 逐槽位校验并从服务端重新读取真实物品（不信任客户端）
        List<SlotItem> slots = new java.util.ArrayList<>();
        for (int slotIndex : slotIndices) {
            if (slotIndex < 0 || slotIndex >= menu.slots.size()) {
                continue;
            }
            Slot slot = menu.slots.get(slotIndex);
            if (slot.container != player.getInventory()) {
                continue;
            }
            ItemStack stack = slot.getItem();
            if (stack.isEmpty()) {
                continue;
            }
            com.heihun.Itemshowcase.core.model.SourceKind source =
                    sourceForContainerSlot(slot.getContainerSlot());
            slots.add(new SlotItem(
                    new NeoDataViews.NeoItemStackView(stack, player.registryAccess()),
                    slot.getContainerSlot(), source));
        }
        if (slots.isEmpty()) {
            return;
        }

        // 4. 构建展示消息（单物品或多物品共用 [inv] 管线，预算 maxItemsPerMessage 把关）
        ShowcaseMessage message = buildSingleItemMessage(player, slots);
        BroadcastService.broadcast(player.level().getServer(), message);
    }

    /** Inventory 内部槽位号 → 来源分类（0-8 快捷栏、9-35 主背包、36-39 盔甲、40 副手）。 */
    private static com.heihun.Itemshowcase.core.model.SourceKind sourceForContainerSlot(int containerSlot) {
        if (containerSlot < 36) {
            return com.heihun.Itemshowcase.core.model.SourceKind.INVENTORY;
        }
        if (containerSlot < 40) {
            return com.heihun.Itemshowcase.core.model.SourceKind.ARMOR;
        }
        return com.heihun.Itemshowcase.core.model.SourceKind.OFFHAND;
    }

    private static ShowcaseMessage buildSingleItemMessage(ServerPlayer player, List<SlotItem> slots) {
        // 快捷展示视为“只读该槽位”的数据视图
        com.heihun.Itemshowcase.core.spi.PlayerDataViews views = new com.heihun.Itemshowcase.core.spi.PlayerDataViews() {
            @Override
            public SlotItem getMainHand() {
                return new SlotItem(
                        new NeoDataViews.NeoItemStackView(player.getMainHandItem(), player.registryAccess()),
                        player.getInventory().selected,
                        com.heihun.Itemshowcase.core.model.SourceKind.HAND);
            }

            @Override
            public List<SlotItem> getInventory() {
                return slots;
            }

            @Override
            public List<SlotItem> getEnderChest() {
                return List.of();
            }

            @Override
            public List<com.heihun.Itemshowcase.core.spi.AccessorySlotData> getAccessories() {
                return List.of();
            }

            @Override
            public List<com.heihun.Itemshowcase.core.spi.BackpackData> getEquippedBackpacks() {
                return List.of();
            }

            @Override
            public com.heihun.Itemshowcase.core.spi.BackpackData inspectBackpack(SlotItem stack, int maxDepth) {
                // 快捷展示同样支持容器物品展开（如精妙背包）
                return new NeoDataViews(player, ShowcaseServices.integrations(),
                        player.registryAccess()).inspectBackpack(stack, maxDepth);
            }
        };
        ShowcaseMessageFactory factory = new ShowcaseMessageFactory(views, ShowcaseServices.serverConfig());
        return factory.build(MacroParser.parse("[inv]"), player.getUUID(), player.getGameProfile().getName());
    }
}
