package com.heihun.Itemshowcase.neoforge1211;

import com.heihun.Itemshowcase.core.config.ItemShowcaseConfig;
import com.heihun.Itemshowcase.core.spi.IntegrationRegistry;
import com.heihun.Itemshowcase.core.validate.RateLimiter;

/**
 * 模组级服务：配置值、限流器、集成注册表。
 * 配置值由 NeoForge 原生配置（NeoConfigs，TOML）在加载/热重载事件中同步。
 */
public final class ShowcaseServices {

    private static final ItemShowcaseConfig.Server SERVER_VALUES = new ItemShowcaseConfig.Server();
    private static final ItemShowcaseConfig.Client CLIENT_VALUES = new ItemShowcaseConfig.Client();

    private static volatile RateLimiter rateLimiter = new RateLimiter(6, 10);
    private static volatile int lastRateLimitMax = 6;
    private static volatile int lastRateLimitWindow = 10;

    private static final IntegrationRegistry INTEGRATIONS = new IntegrationRegistry();

    private ShowcaseServices() {
    }

    /**
     * 集成注册。注意：NeoForge 21.1 中 COMMON（服务端）配置在 mod 构造期同样未加载，
     * 此处绝不读配置值——配置同步只发生在 ModConfigEvent.Loading / Reloading 中。
     */
    public static void init() {
        com.heihun.Itemshowcase.neoforge1211.server.integration.IntegrationSetup.registerAll(INTEGRATIONS);
    }

    /** 配置同步后的钩子：仅当限流参数真正变化时重建限流器。 */
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
