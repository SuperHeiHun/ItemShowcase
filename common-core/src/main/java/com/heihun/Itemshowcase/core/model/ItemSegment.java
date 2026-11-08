package com.heihun.Itemshowcase.core.model;

import com.heihun.Itemshowcase.core.protocol.ItemStackPayload;

/**
 * 单个物品片段。
 *
 * <p>携带序列化后的真实 ItemStack 数据（客户端用它还原真实物品并调用
 * 原版渲染 / Tooltip 逻辑）、数量、来源、槽位信息与嵌套深度。</p>
 *
 * <p>核心模型不引用任何版本的 ItemStack 类型。</p>
 */
public final class ItemSegment extends ShowcaseSegment {

    /** 序列化的物品数据。 */
    private final ItemStackPayload item;

    /** 显示数量（通常与 item.count() 一致，特殊容器可覆盖）。 */
    private final int count;

    /** 纯文本物品名（服务端生成，用于原版回退与日志；客户端渲染不使用）。 */
    private final String plainName;

    /** 物品显示名的组件 JSON（可空；客户端优先从真实物品数据取）。 */
    private final String displayNameJson;

    /** 来源分类。 */
    private final SourceKind source;

    /** 槽位信息：饰品槽位类型 ID / 库存槽位号的可读形式；无则为 null。 */
    private final String slotInfo;

    /** 嵌套深度：0 = 玩家直接持有；1 = 来自玩家持有的背包内部；以此类推。 */
    private final int depth;

    public ItemSegment(ItemStackPayload item, int count, String plainName, String displayNameJson,
                       SourceKind source, String slotInfo, int depth) {
        if (item == null) {
            throw new IllegalArgumentException("ItemStackPayload 不能为空");
        }
        if (source == null) {
            throw new IllegalArgumentException("SourceKind 不能为空");
        }
        this.item = item;
        this.count = count;
        this.plainName = (plainName == null) ? item.itemIdHint() : plainName;
        this.displayNameJson = displayNameJson;
        this.source = source;
        this.slotInfo = (slotInfo == null || slotInfo.isEmpty()) ? null : slotInfo;
        this.depth = depth;
    }

    public ItemStackPayload item() {
        return item;
    }

    public int count() {
        return count;
    }

    public String plainName() {
        return plainName;
    }

    public String displayNameJson() {
        return displayNameJson;
    }

    public SourceKind source() {
        return source;
    }

    public String slotInfo() {
        return slotInfo;
    }

    public int depth() {
        return depth;
    }

    @Override
    public Type type() {
        return Type.ITEM;
    }
}
