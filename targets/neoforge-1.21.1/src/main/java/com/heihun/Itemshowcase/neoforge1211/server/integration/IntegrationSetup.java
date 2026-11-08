package com.heihun.Itemshowcase.neoforge1211.server.integration;

import com.heihun.Itemshowcase.core.spi.IntegrationRegistry;
import net.neoforged.fml.ModList;

/**
 * 可选 MOD 集成的统一注册入口（NeoForge 1.21.1）。
 * 每个集成都先做运行时加载检测再实例化，保证类加载安全。
 * 新增集成（如 Traveler's Backpack）时只需在此追加一行注册。
 */
public final class IntegrationSetup {

    private IntegrationSetup() {
    }

    public static void registerAll(IntegrationRegistry registry) {
        if (ModList.get().isLoaded("curios")) {
            registry.registerAccessory(new CuriosAccessoryProvider());
        }
        // TODO(阶段五)：SophisticatedBackpacksIntegration 待 SB NeoForge 1.21.1 依赖坐标确认后启用
    }
}
