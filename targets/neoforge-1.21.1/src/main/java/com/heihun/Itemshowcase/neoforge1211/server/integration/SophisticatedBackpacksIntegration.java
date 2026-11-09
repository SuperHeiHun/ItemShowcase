package com.heihun.Itemshowcase.neoforge1211.server.integration;

import com.heihun.Itemshowcase.core.logging.ItemShowcaseLogger;
import com.heihun.Itemshowcase.core.spi.ItemContainerProvider;
import com.heihun.Itemshowcase.core.spi.ItemStackView;
import com.heihun.Itemshowcase.core.text.TextPayload;
import com.heihun.Itemshowcase.neoforge1211.server.NeoDataViews;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackItem;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.IBackpackWrapper;

import java.util.ArrayList;
import java.util.List;

/**
 * Sophisticated Backpacks 集成（NeoForge 1.21.1，SB 3.26+）。
 *
 * <p>设计要点（与 Forge 1.20.1 参考实现一致）：</p>
 * <ul>
 *   <li>通过公开 API {@code new BackpackWrapper(stack)} 获取包装器，
 *       再用 {@code getInventoryHandler()} 遍历内部库存——不依赖固定 NBT/组件路径
 *       （1.21.1 背包数据已组件化，包装器内部处理）。</li>
 *   <li>类型判断使用 {@link BackpackItem} 类检查（注册表类型的自然判断，
 *       不是物品 ID 白名单），普通背包不会被误识别。</li>
 *   <li>读取失败时返回空列表并记录日志，不导致聊天系统崩溃。</li>
 * </ul>
 */
public final class SophisticatedBackpacksIntegration implements ItemContainerProvider {

    @Override
    public String id() {
        return "sophisticatedbackpacks";
    }

    @Override
    public boolean isAvailable() {
        return ModList.get().isLoaded("sophisticatedbackpacks")
                && ModList.get().isLoaded("sophisticatedcore");
    }

    @Override
    public boolean supports(ItemStackView stack) {
        if (!(stack.platformStack() instanceof ItemStack mcStack) || mcStack.isEmpty()) {
            return false;
        }
        try {
            // 精确类型判断：必须是 SB 的 BackpackItem 实例
            if (!(mcStack.getItem() instanceof BackpackItem)) {
                return false;
            }
            IBackpackWrapper wrapper = new BackpackWrapper(mcStack);
            return wrapper != null && wrapper.getInventoryHandler() != null;
        } catch (Throwable apiFailure) {
            ItemShowcaseLogger.warn("Sophisticated Backpacks 包装器获取失败", apiFailure);
            return false;
        }
    }

    @Override
    public List<ItemStackView> getContents(ItemStackView stack, int maxDepth) {
        if (!(stack.platformStack() instanceof ItemStack mcStack) || mcStack.isEmpty()) {
            return List.of();
        }
        List<ItemStackView> contents = new ArrayList<>();
        try {
            // 公开构造器：从物品创建包装器（不依赖固定 NBT/组件路径）
            IBackpackWrapper wrapper = new BackpackWrapper(mcStack);
            if (wrapper == null) {
                return contents;
            }
            var inventory = wrapper.getInventoryHandler();
            if (inventory == null) {
                return contents;
            }
            int slots = inventory.getSlots();
            for (int i = 0; i < slots; i++) {
                ItemStack inner = inventory.getStackInSlot(i);
                if (inner.isEmpty()) {
                    continue;
                }
                contents.add(new NeoDataViews.NeoItemStackView(inner,
                        net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer().registryAccess()));
            }
        } catch (Throwable apiFailure) {
            // 读取失败降级为空内容：背包本身仍正常展示
            ItemShowcaseLogger.warn("Sophisticated Backpacks 内部库存读取失败, 已降级为空", apiFailure);
            return List.of();
        }
        return contents;
    }

    @Override
    public TextPayload getDisplayName(ItemStackView stack) {
        return TextPayload.literal(stack.plainDisplayName());
    }
}
