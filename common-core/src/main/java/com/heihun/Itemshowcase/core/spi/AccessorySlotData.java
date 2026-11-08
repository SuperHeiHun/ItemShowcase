package com.heihun.Itemshowcase.core.spi;

/**
 * 饰品栏中的一个槽位。
 *
 * <p>slotTypeId 来自饰品 API 动态查询（Curios / Trinkets / Accessories 各自的
 * 注册机制），ItemShowcase 不对槽位名称做任何假设，只负责分组展示。</p>
 */
public final class AccessorySlotData {

    private final String providerId;
    private final String slotTypeId;
    private final int slotIndex;
    private final ItemStackView stack;

    public AccessorySlotData(String providerId, String slotTypeId, int slotIndex, ItemStackView stack) {
        this.providerId = providerId;
        this.slotTypeId = (slotTypeId == null) ? "unknown" : slotTypeId;
        this.slotIndex = slotIndex;
        this.stack = stack;
    }

    /** 提供该槽位的饰品 API 标识（curios / trinkets / accessories）。 */
    public String providerId() {
        return providerId;
    }

    /** 槽位类型 ID（如 "ring"），仅作分组与展示，核心逻辑不作判断。 */
    public String slotTypeId() {
        return slotTypeId;
    }

    /** 同类型槽位内的索引。 */
    public int slotIndex() {
        return slotIndex;
    }

    public ItemStackView stack() {
        return stack;
    }

    public boolean isEmpty() {
        return stack == null || stack.isEmpty();
    }
}
