package com.heihun.Itemshowcase.core.spi;

import java.util.List;

/**
 * 一个被识别出的容器类物品（如 Sophisticated Backpacks 背包）及其内部库存。
 *
 * <p>contents 深度已由适配层限制（默认 1 层），不允许无限递归。</p>
 */
public final class BackpackData {

    private final ItemStackView backpack;
    private final List<ItemStackView> contents;
    private final String providerId;

    public BackpackData(ItemStackView backpack, List<ItemStackView> contents, String providerId) {
        this.backpack = backpack;
        this.contents = contents;
        this.providerId = (providerId == null) ? "unknown" : providerId;
    }

    public ItemStackView backpack() {
        return backpack;
    }

    /** 背包内部物品（已过滤空物品）。 */
    public List<ItemStackView> contents() {
        return contents;
    }

    /** 提供内部库存读取的集成标识（如 "sophisticatedbackpacks"）。 */
    public String providerId() {
        return providerId;
    }

    public boolean isEmpty() {
        return backpack == null || backpack.isEmpty();
    }
}
