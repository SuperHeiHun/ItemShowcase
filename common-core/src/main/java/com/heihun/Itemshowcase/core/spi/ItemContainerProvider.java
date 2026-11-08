package com.heihun.Itemshowcase.core.spi;

import java.util.List;

import com.heihun.Itemshowcase.core.text.TextPayload;

/**
 * 容器类物品集成接口（插件式架构）。
 *
 * <p>任何能够提供“内部库存”的物品 MOD（Sophisticated Backpacks、
 * Traveler's Backpack……）各版本各实现一个 Provider，通过
 * {@link IntegrationRegistry} 注册。核心解析器只依赖本接口，
 * <b>禁止</b>在核心代码中针对具体 MOD 的物品 ID 做判断。</p>
 */
public interface ItemContainerProvider {

    /** 集成标识（通常为 MOD ID，如 "sophisticatedbackpacks"）。 */
    String id();

    /** 该 MOD 是否已加载且 API 可用（运行时检测，不导致崩溃）。 */
    boolean isAvailable();

    /**
     * 是否能读取该物品的内部库存。
     * 实现必须通过 API 的类型/能力判断（例如 BackpackWrapper.fromStack），
     * 不得依赖固定 NBT 路径，也不得把普通背包误判为本 MOD 背包。
     */
    boolean supports(ItemStackView stack);

    /**
     * 读取内部库存。返回非空物品列表；读取失败应返回空列表并记录日志，
     * 不允许抛出异常打断聊天处理。
     */
    List<ItemStackView> getContents(ItemStackView stack, int maxDepth);

    /** 背包显示名（组件 JSON），用于分组标题。 */
    TextPayload getDisplayName(ItemStackView stack);
}
