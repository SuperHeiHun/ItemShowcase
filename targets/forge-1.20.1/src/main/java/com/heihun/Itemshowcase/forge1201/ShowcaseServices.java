package com.heihun.Itemshowcase.forge1201;

import com.heihun.Itemshowcase.core.config.ItemShowcaseConfig;
import com.heihun.Itemshowcase.core.logging.ItemShowcaseLogger;
import com.heihun.Itemshowcase.core.spi.IntegrationRegistry;
import com.heihun.Itemshowcase.core.validate.RateLimiter;

/**
 * 模组级服务：配置值、限流器、集成注册表。
 *
 * <p>配置值由 Forge 原生配置（ForgeConfigs，TOML）在加载/热重载事件中同步到
 * 这里的配置对象；服务端管线与客户端渲染只读这些对象，对配置来源无感知。</p>
 */
public final class ShowcaseServices {

    /** 服务端配置值（ForgeConfigs.syncAll 更新）。 */
    private static final ItemShowcaseConfig.Server SERVER_VALUES = new ItemShowcaseConfig.Server();

    /** 客户端配置值（ForgeConfigs.syncAll 更新）。 */
    private static final ItemShowcaseConfig.Client CLIENT_VALUES = new ItemShowcaseConfig.Client();

    /** 展示消息频率限制（参数随服务端配置变化）。 */
    private static volatile RateLimiter rateLimiter = new RateLimiter(6, 10);
    private static volatile int lastRateLimitMax = 6;
    private static volatile int lastRateLimitWindow = 10;

    /** 集成注册表：容器物品 Provider 与饰品 API Provider。 */
    private static final IntegrationRegistry INTEGRATIONS =
            new IntegrationRegistry();

    private ShowcaseServices() {
    }

    public static void init() {
        // 可选 MOD 集成注册（缺失时安全降级，注册表内部会记录日志）
        com.heihun.Itemshowcase.forge1201.server.integration.IntegrationSetup.registerAll(INTEGRATIONS);
        // 初始同步（此时 Forge spec 已就绪，使用 TOML 默认值）
        ForgeConfigs.syncAll();
        ItemShowcaseLogger.info("ItemShowcase 配置已加载");
    }

    /** 配置同步后的钩子：仅当限流参数真正变化时重建限流器，避免重置玩家令牌。 */
    static void onConfigSynced() {
        ItemShowcaseConfig.Server sc = SERVER_VALUES;
        if (sc.rateLimitMaxPerWindow != lastRateLimitMax || sc.rateLimitWindowSeconds != lastRateLimitWindow) {
            lastRateLimitMax = sc.rateLimitMaxPerWindow;
            lastRateLimitWindow = sc.rateLimitWindowSeconds;
            rateLimiter = new RateLimiter(sc.rateLimitMaxPerWindow, sc.rateLimitWindowSeconds);
        }
    }

    public static ItemShowcaseConfig.Server serverConfig() {
        return SERVER_VALUES;
    }

    public static ItemShowcaseConfig.Client clientConfig() {
        return CLIENT_VALUES;
    }

    /** ForgeConfigs.syncAll 使用（package-private）。 */
    static ItemShowcaseConfig.Server serverValues() {
        return SERVER_VALUES;
    }

    static ItemShowcaseConfig.Client clientValues() {
        return CLIENT_VALUES;
    }

    public static RateLimiter rateLimiter() {
        return rateLimiter;
    }

    public static IntegrationRegistry integrations() {
        return INTEGRATIONS;
    }
}
