package com.heihun.Itemshowcase.core.spi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 集成注册表：管理所有可选 MOD 集成（容器物品 Provider 与饰品 API Provider）。
 *
 * <p>单个集成缺失或初始化失败不会影响注册表与其他集成——注册方法内部
 * 捕获异常并记录日志（这是“可选依赖安全降级”的落点，不是掩盖业务错误）。</p>
 */
public final class IntegrationRegistry {

    private final List<ItemContainerProvider> containerProviders = new ArrayList<>();
    private final List<AccessoryProvider> accessoryProviders = new ArrayList<>();

    public void registerContainer(ItemContainerProvider provider) {
        if (provider == null) {
            return;
        }
        try {
            boolean available = provider.isAvailable();
            if (available) {
                containerProviders.add(provider);
                com.heihun.Itemshowcase.core.logging.ItemShowcaseLogger.info(
                        "已启用容器物品集成: " + provider.id());
            } else {
                // 正常状态（可选 MOD 未安装）不打 info，避免日志噪音
                com.heihun.Itemshowcase.core.logging.ItemShowcaseLogger.debug(
                        "容器物品集成不可用（MOD 未安装）: " + provider.id());
            }
        } catch (Throwable t) {
            // 可选集成的探测失败必须降级而不是崩溃；记录完整异常便于排查。
            com.heihun.Itemshowcase.core.logging.ItemShowcaseLogger.warn(
                    "容器物品集成初始化失败，已跳过: " + provider.id(), t);
        }
    }

    public void registerAccessory(AccessoryProvider provider) {
        if (provider == null) {
            return;
        }
        try {
            boolean available = provider.isAvailable();
            if (available) {
                accessoryProviders.add(provider);
                com.heihun.Itemshowcase.core.logging.ItemShowcaseLogger.info(
                        "已启用饰品 API 集成: " + provider.id());
            } else {
                // 正常状态（可选 MOD 未安装）不打 info，避免日志噪音
                com.heihun.Itemshowcase.core.logging.ItemShowcaseLogger.debug(
                        "饰品 API 集成不可用（MOD 未安装）: " + provider.id());
            }
        } catch (Throwable t) {
            com.heihun.Itemshowcase.core.logging.ItemShowcaseLogger.warn(
                    "饰品 API 集成初始化失败，已跳过: " + provider.id(), t);
        }
    }

    /** 查找第一个支持该物品的容器 Provider（注册顺序即优先级）。 */
    public ItemContainerProvider findContainerProvider(ItemStackView stack) {
        for (ItemContainerProvider provider : containerProviders) {
            try {
                if (provider.supports(stack)) {
                    return provider;
                }
            } catch (Throwable t) {
                com.heihun.Itemshowcase.core.logging.ItemShowcaseLogger.warn(
                        "容器 Provider.supports 抛出异常, 已跳过: " + provider.id(), t);
            }
        }
        return null;
    }

    public List<ItemContainerProvider> containerProviders() {
        return Collections.unmodifiableList(containerProviders);
    }

    public List<AccessoryProvider> accessoryProviders() {
        return Collections.unmodifiableList(accessoryProviders);
    }
}
