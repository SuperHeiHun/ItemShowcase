package com.heihun.Itemshowcase.forge1201;

import com.heihun.Itemshowcase.core.config.ConfigManager;
import com.heihun.Itemshowcase.core.config.ItemShowcaseConfig;
import com.heihun.Itemshowcase.core.logging.ItemShowcaseLogger;
import com.heihun.Itemshowcase.core.validate.RateLimiter;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Path;

/**
 * 模组级服务：配置管理、限流器。
 * 服务端与客户端共用同一个进程时各持有一套配置文件。
 */
public final class ShowcaseServices {

    private static final ConfigManager<ItemShowcaseConfig.Server> SERVER_CONFIG =
            new ConfigManager<>(configPath("itemshowcase-server.json"),
                    ItemShowcaseConfig.Server.class, ItemShowcaseConfig.Server::new, "服务端");

    private static final ConfigManager<ItemShowcaseConfig.Client> CLIENT_CONFIG =
            new ConfigManager<>(configPath("itemshowcase-client.json"),
                    ItemShowcaseConfig.Client.class, ItemShowcaseConfig.Client::new, "客户端");

    /** 展示消息频率限制（参数随服务端配置变化）。 */
    private static volatile RateLimiter rateLimiter = new RateLimiter(6, 10);

    /** 集成注册表：容器物品 Provider 与饰品 API Provider。 */
    private static final com.heihun.Itemshowcase.core.spi.IntegrationRegistry INTEGRATIONS =
            new com.heihun.Itemshowcase.core.spi.IntegrationRegistry();

    private ShowcaseServices() {
    }

    public static void init() {
        SERVER_CONFIG.load();
        CLIENT_CONFIG.load();
        rateLimiter = new RateLimiter(SERVER_CONFIG.get().rateLimitMaxPerWindow, SERVER_CONFIG.get().rateLimitWindowSeconds);
        // 可选 MOD 集成注册（缺失时安全降级，注册表内部会记录日志）
        com.heihun.Itemshowcase.forge1201.server.integration.IntegrationSetup.registerAll(INTEGRATIONS);
        ItemShowcaseLogger.info("ItemShowcase 配置已加载");
    }

    /** 每个服务端 tick 调用：配置热加载。 */
    public static void tickConfigReload() {
        SERVER_CONFIG.reloadIfChanged();
        CLIENT_CONFIG.reloadIfChanged();
        rateLimiter = new RateLimiter(SERVER_CONFIG.get().rateLimitMaxPerWindow, SERVER_CONFIG.get().rateLimitWindowSeconds);
    }

    public static ItemShowcaseConfig.Server serverConfig() {
        return SERVER_CONFIG.get();
    }

    public static ItemShowcaseConfig.Client clientConfig() {
        return CLIENT_CONFIG.get();
    }

    public static RateLimiter rateLimiter() {
        return rateLimiter;
    }

    public static com.heihun.Itemshowcase.core.spi.IntegrationRegistry integrations() {
        return INTEGRATIONS;
    }

    private static Path configPath(String fileName) {
        return FMLPaths.CONFIGDIR.get().resolve(fileName);
    }
}
