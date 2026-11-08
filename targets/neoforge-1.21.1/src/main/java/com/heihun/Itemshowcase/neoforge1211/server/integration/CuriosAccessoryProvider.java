package com.heihun.Itemshowcase.neoforge1211.server.integration;

import com.heihun.Itemshowcase.core.logging.ItemShowcaseLogger;
import com.heihun.Itemshowcase.core.spi.AccessoryProvider;
import com.heihun.Itemshowcase.core.spi.AccessorySlotData;
import com.heihun.Itemshowcase.core.spi.ItemStackView;
import com.heihun.Itemshowcase.neoforge1211.server.NeoDataViews;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Curios API 集成（NeoForge 1.21.1，Curios 9.x）。
 *
 * <p>本类只应在 Curios 已加载时被实例化（IntegrationSetup 负责）；
 * 槽位类型动态枚举，不硬编码任何槽位名；API 故障降级为空列表。</p>
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
        if (!(platformPlayer instanceof LivingEntity entity) || !isAvailable()) {
            return List.of();
        }
        List<AccessorySlotData> result = new ArrayList<>();
        try {
            Optional<ICuriosItemHandler> handler = CuriosApi.getCuriosInventory(entity);
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
                    ItemStackView view = new NeoDataViews.NeoItemStackView(stack,
                            net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer().registryAccess());
                    result.add(new AccessorySlotData(id(), slotTypeId, i, view));
                }
            }
        } catch (Throwable apiFailure) {
            ItemShowcaseLogger.warn("Curios 饰品槽位读取失败, 已降级为空", apiFailure);
            return List.of();
        }
        return result;
    }
}
