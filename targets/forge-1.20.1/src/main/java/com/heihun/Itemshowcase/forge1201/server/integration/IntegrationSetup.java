package com.heihun.Itemshowcase.forge1201.server.integration;

import com.heihun.Itemshowcase.core.spi.IntegrationRegistry;
import net.minecraftforge.fml.ModList;

/**
 * 可选 MOD 集成的统一注册入口。
 *
 * <p>每个集成都先做运行时加载检测再实例化，保证：</p>
 * <ul>
 *   <li>MOD 未安装时相关类完全不被加载（无 NoClassDefFoundError）。</li>
 *   <li>单个集成初始化失败不影响其他集成（IntegrationRegistry 内部兜底）。</li>
 * </ul>
 *
 * <p>新增集成（如 Traveler's Backpack）时只需在此追加一行注册。</p>
 */
public final class IntegrationSetup {

    private IntegrationSetup() {
    }

    public static void registerAll(IntegrationRegistry registry) {
        if (ModList.get().isLoaded("curios")) {
            registry.registerAccessory(new CuriosAccessoryProvider());
        }
        if (ModList.get().isLoaded("sophisticatedbackpacks")
                && ModList.get().isLoaded("sophisticatedcore")) {
            registry.registerContainer(new SophisticatedBackpacksIntegration());
        }
    }
}
