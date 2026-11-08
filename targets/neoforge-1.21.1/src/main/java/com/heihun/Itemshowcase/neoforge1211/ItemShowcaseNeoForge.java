package com.heihun.Itemshowcase.neoforge1211;

import com.heihun.Itemshowcase.core.logging.ItemShowcaseLogger;
import com.heihun.Itemshowcase.neoforge1211.network.NeoNetwork;
import com.heihun.Itemshowcase.neoforge1211.client.ClientInit;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ItemShowcase 主类（NeoForge 1.21.1）。
 * 生命周期、网络（PayloadRegistrar）、配置（ModConfigSpec）与游戏事件注册入口。
 */
@Mod(ItemShowcaseNeoForge.MOD_ID)
public class ItemShowcaseNeoForge {

    public static final String MOD_ID = "itemshowcase";

    private static final Logger LOGGER = LoggerFactory.getLogger("ItemShowcase");

    public ItemShowcaseNeoForge(IEventBus modBus, ModContainer modContainer) {
        bindLogger();
        // Forge 原生风格配置（NeoForge ModConfigSpec，自带文件监听热重载）
        NeoConfigs.register(modContainer);
        // 网络（PayloadRegistrar）
        NeoNetwork.register(modBus);
        // 配置加载/热重载事件（Loading 与 Reloading，按 spec 类型分别同步）
        modBus.addListener(this::onModConfigEvent);
        // 集成注册 + 服务端配置初始同步（CLIENT 配置构造期未加载，仅在事件中同步）
        ShowcaseServices.init();

        // 游戏事件总线：服务端聊天处理（含退出清理）
        NeoForge.EVENT_BUS.register(com.heihun.Itemshowcase.neoforge1211.server.ChatHandler.class);

        // 客户端事件（类加载隔离：dedicated server 绝不加载客户端类）
        if (net.neoforged.fml.loading.FMLEnvironment.dist.isClient()) {
            ClientInit.register(modBus);
        }
        ItemShowcaseLogger.info("ItemShowcase 初始化完成 (neoforge/1.21.1)");
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
    }

    private void onModConfigEvent(ModConfigEvent event) {
        // Loading（首次加载）与 Reloading（外部修改热重载）：按 spec 类型分别同步
        if (event.getConfig().getSpec() == NeoConfigs.SERVER_SPEC) {
            NeoConfigs.syncServer();
        } else if (event.getConfig().getSpec() == NeoConfigs.CLIENT_SPEC) {
            NeoConfigs.syncClient();
        }
    }

    /** 把平台 Logger 接入 common-core 统一日志门面。 */
    private static void bindLogger() {
        ItemShowcaseLogger.use((severity, message, throwable) -> {
            switch (severity) {
                case DEBUG -> LOGGER.debug(message, throwable);
                case INFO -> LOGGER.info(message, throwable);
                case WARN -> LOGGER.warn(message, throwable);
                case ERROR -> LOGGER.error(message, throwable);
            }
        });
    }
}
