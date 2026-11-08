package com.heihun.Itemshowcase.core.spi;

import com.heihun.Itemshowcase.core.model.SourceKind;

/**
 * 库存中的一个槽位（背包 / 快捷栏 / 盔甲 / 副手 / 末影箱 / 手持）。
 */
public final class SlotItem {

    private final ItemStackView stack;
    private final int slotIndex;
    private final SourceKind source;

    public SlotItem(ItemStackView stack, int slotIndex, SourceKind source) {
        this.stack = stack;
        this.slotIndex = slotIndex;
        this.source = source;
    }

    public ItemStackView stack() {
        return stack;
    }

    /** 原生库存中的槽位号（诊断 / 调试用）。 */
    public int slotIndex() {
        return slotIndex;
    }

    public SourceKind source() {
        return source;
    }

    public boolean isEmpty() {
        return stack == null || stack.isEmpty();
    }
}
