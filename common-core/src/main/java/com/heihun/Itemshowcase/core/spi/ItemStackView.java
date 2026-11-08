package com.heihun.Itemshowcase.core.spi;

import com.heihun.Itemshowcase.core.protocol.ItemStackPayload;

/**
 * 真实 ItemStack 的版本无关视图。
 *
 * <p>由各版本适配器包装原生 ItemStack 实现；common-core 只通过该接口
 * 读取信息，绝不直接引用 Minecraft 类型。</p>
 *
 * <p>约束：isEmpty() == true 的视图不得进入任何展示数据。</p>
 */
public interface ItemStackView {

    /** 平台原生对象句柄（例如 net.minecraft.world.item.ItemStack）。集成层可安全向下转型。 */
    Object platformStack();

    /** 物品数量，>= 1。 */
    int getCount();

    /** 是否为空物品。 */
    boolean isEmpty();

    /** 注册表 ID（例如 "minecraft:diamond_sword"），仅用于诊断与统计。 */
    String itemId();

    /** 纯文本显示名（服务端语言），用于原版回退与日志。 */
    String plainDisplayName();

    /** 用该版本的序列化机制编码为网络载荷。 */
    ItemStackPayload toPayload();
}
