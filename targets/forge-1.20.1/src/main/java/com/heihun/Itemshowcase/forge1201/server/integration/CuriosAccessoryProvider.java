package com.heihun.Itemshowcase.forge1201.server.integration;

import com.heihun.Itemshowcase.core.logging.ItemShowcaseLogger;
import com.heihun.Itemshowcase.core.spi.AccessoryProvider;
import com.heihun.Itemshowcase.core.spi.AccessorySlotData;
import com.heihun.Itemshowcase.core.spi.ItemStackView;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Curios API 集成（Forge 1.20.1，Curios 5.x）。
 *
 * <p>设计要点：</p>
 * <ul>
 *   <li>本类只应在 Curios 已加载时被实例化（IntegrationSetup 负责），保证类加载安全。</li>
 *   <li>槽位类型通过 {@code getCurios()} 动态枚举——不硬编码 ring/necklace 等任何槽位名。</li>
 *   <li>API 调用失败时返回空列表并记录日志，不影响其他饰品 API 或整个聊天系统。</li>
 *   <li>同一玩家可同时被多个饰品 API 覆盖，本 Provider 只负责 Curios 自己的槽位。</li>
 * </ul>
 */
public final class CuriosAccessoryProvider implements AccessoryProvider {

    @Override
    public String id() {
        return "curios";
    }

    @Override
    public boolean isAvailable() {
        return ModList.get().isLoaded("curios");
    }

    @Override
    public List<AccessorySlotData> getSlots(Object platformPlayer) {
        if (!(platformPlayer instanceof ServerPlayer player) || !isAvailable()) {
            return List.of();
        }
        List<AccessorySlotData> result = new ArrayList<>();
        try {
            // Curios 5.x 非过时静态入口：返回 LazyOptional<ICuriosItemHandler>
            Optional<ICuriosItemHandler> handler = CuriosApi.getCuriosInventory(player).resolve();
            if (handler.isEmpty()) {
                return result;
            }
            // 动态枚举全部已注册槽位类型（map key 即槽位类型 ID）
            Map<String, ICurioStacksHandler> curios = handler.get().getCurios();
            for (Map.Entry<String, ICurioStacksHandler> entry : curios.entrySet()) {
                String slotTypeId = entry.getKey();
                ICurioStacksHandler stacksHandler = entry.getValue();
                if (stacksHandler == null || stacksHandler.getStacks() == null) {
                    continue;
                }
                var stacks = stacksHandler.getStacks();
                int slotCount = stacks.getSlots();
                for (int i = 0; i < slotCount; i++) {
                    ItemStack stack = stacks.getStackInSlot(i);
                    if (stack.isEmpty()) {
                        continue; // 不展示空槽位
                    }
                    ItemStackView view = new com.heihun.Itemshowcase.forge1201.server.ForgeDataViews.ForgeItemStackView(stack);
                    result.add(new AccessorySlotData(id(), slotTypeId, i, view));
                }
            }
        } catch (Throwable apiFailure) {
            // Curios API 异常不允许打断聊天处理
            ItemShowcaseLogger.warn("Curios 饰品槽位读取失败, 已降级为空", apiFailure);
            return List.of();
        }
        return result;
    }
}
