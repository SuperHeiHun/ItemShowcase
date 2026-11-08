package com.heihun.Itemshowcase.forge1201.server;

import com.heihun.Itemshowcase.core.config.ItemShowcaseConfig;
import com.heihun.Itemshowcase.core.macros.MacroParser;
import com.heihun.Itemshowcase.core.model.ShowcaseMessage;
import com.heihun.Itemshowcase.core.model.ShowcaseMessageFactory;
import com.heihun.Itemshowcase.forge1201.ShowcaseServices;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerPlayer;

/**
 * Ctrl+Enter 快捷展示的服务端逻辑。
 *
 * <p>安全模型：客户端只提交“菜单 ID + 槽位 ID”；服务端校验菜单一致性、
 * 槽位归属（必须为玩家自己的库存），随后从服务端重新读取真实 ItemStack——
 * 客户端提交的任何物品数据都不被信任。</p>
 */
public final class QuickShowHandler {

    private QuickShowHandler() {
    }

    public static void handleRequest(ServerPlayer player, int containerId, int slotIndex) {
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
        // 2. 槽位索引合法
        if (slotIndex < 0 || slotIndex >= menu.slots.size()) {
            return;
        }
        Slot slot = menu.slots.get(slotIndex);
        // 3. 槽位必须属于玩家自己的库存（背包/快捷栏/盔甲/副手），
        //    不允许通过该快捷键读取容器中其他玩家或容器的物品
        if (slot.container != player.getInventory()) {
            return;
        }
        // 4. 频率限制
        if (!ShowcaseServices.rateLimiter().tryAcquire(player.getUUID())) {
            BroadcastService.notice(player, "itemshowcase.chat.rate_limited");
            return;
        }
        // 5. 从服务端重新获取真实物品（不信任客户端）
        ItemStack stack = slot.getItem();
        if (stack.isEmpty()) {
            return;
        }

        // 6. 构建单物品展示消息（来源按库存区域判定）
        com.heihun.Itemshowcase.core.model.SourceKind source = sourceForContainerSlot(slot.getContainerSlot());
        com.heihun.Itemshowcase.core.spi.SlotItem slotItem = new com.heihun.Itemshowcase.core.spi.SlotItem(
                new ForgeDataViews.ForgeItemStackView(stack), slot.getContainerSlot(), source);
        java.util.List<com.heihun.Itemshowcase.core.spi.SlotItem> handOnly =
                java.util.List.of(slotItem);

        ShowcaseMessage message = buildSingleItemMessage(player, handOnly);
        BroadcastService.broadcast(player.server, message);
    }

    private static ShowcaseMessage buildSingleItemMessage(ServerPlayer player,
                                                          java.util.List<com.heihun.Itemshowcase.core.spi.SlotItem> slots) {
        // 复用 PlayerDataViews：快捷展示视为“只读该槽位”的数据视图
        com.heihun.Itemshowcase.core.spi.PlayerDataViews views = new com.heihun.Itemshowcase.core.spi.PlayerDataViews() {
            @Override
            public com.heihun.Itemshowcase.core.spi.SlotItem getMainHand() {
                return new com.heihun.Itemshowcase.core.spi.SlotItem(
                        new ForgeDataViews.ForgeItemStackView(player.getMainHandItem()),
                        player.getInventory().selected,
                        com.heihun.Itemshowcase.core.model.SourceKind.HAND);
            }

            @Override
            public java.util.List<com.heihun.Itemshowcase.core.spi.SlotItem> getInventory() {
                return slots;
            }

            @Override
            public java.util.List<com.heihun.Itemshowcase.core.spi.SlotItem> getEnderChest() {
                return java.util.List.of();
            }

            @Override
            public java.util.List<com.heihun.Itemshowcase.core.spi.AccessorySlotData> getAccessories() {
                return java.util.List.of();
            }

            @Override
            public java.util.List<com.heihun.Itemshowcase.core.spi.BackpackData> getEquippedBackpacks() {
                return java.util.List.of();
            }

            @Override
            public com.heihun.Itemshowcase.core.spi.BackpackData inspectBackpack(
                    com.heihun.Itemshowcase.core.spi.SlotItem stack, int maxDepth) {
                // 快捷展示同样支持容器物品展开（如 SB 背包）
                return new ForgeDataViews(player, ShowcaseServices.integrations()).inspectBackpack(stack, maxDepth);
            }
        };
        ShowcaseMessageFactory factory = new ShowcaseMessageFactory(views, ShowcaseServices.serverConfig());
        return factory.build(MacroParser.parse("[inv]"), player.getUUID(), player.getGameProfile().getName());
    }

    /** Inventory 内部槽位号 → 来源分类（0-8 快捷栏, 9-35 主背包, 36-39 盔甲, 40 副手）。 */
    private static com.heihun.Itemshowcase.core.model.SourceKind sourceForContainerSlot(int containerSlot) {
        if (containerSlot < 9) {
            return com.heihun.Itemshowcase.core.model.SourceKind.INVENTORY;
        }
        if (containerSlot < 36) {
            return com.heihun.Itemshowcase.core.model.SourceKind.INVENTORY;
        }
        if (containerSlot < 40) {
            return com.heihun.Itemshowcase.core.model.SourceKind.ARMOR;
        }
        return com.heihun.Itemshowcase.core.model.SourceKind.OFFHAND;
    }
}
