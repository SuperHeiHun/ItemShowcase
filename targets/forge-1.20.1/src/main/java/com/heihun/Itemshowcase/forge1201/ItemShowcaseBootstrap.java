package com.heihun.Itemshowcase.forge1201;

import com.heihun.Itemshowcase.core.logging.ItemShowcaseLogger;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 模组引导：日志绑定与初始化调度。
 * 保证 dedicated server 上绝不加载客户端渲染类。
 */
public final class ItemShowcaseBootstrap {

    private static final Logger LOGGER = LoggerFactory.getLogger("ItemShowcase");

    private ItemShowcaseBootstrap() {
    }

    public static void init() {
        bindLogger();
        ShowcaseServices.init();
        if (FMLEnvironment.dist == Dist.CLIENT) {
            // 类加载隔离：客户端类只在此分支被引用
            com.heihun.Itemshowcase.forge1201.client.ClientInit.init();
        }
        ItemShowcaseLogger.info("ItemShowcase 初始化完成 (forge/1.20.1, dist=" + FMLEnvironment.dist + ")");
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
