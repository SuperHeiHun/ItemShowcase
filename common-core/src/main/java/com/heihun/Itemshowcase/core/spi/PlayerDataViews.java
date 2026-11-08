package com.heihun.Itemshowcase.core.spi;

import java.util.List;

/**
 * 服务端数据源：一次宏处理所需的全部玩家数据。
 *
 * <p>各版本适配器在【服务器线程】上实现本接口——从真实玩家库存、末影箱、
 * 饰品 API 读取。common-core 的 {@code ShowcaseMessageFactory} 只消费此接口，
 * 因此整套消息构建流程可以用桩实现做单元测试。</p>
 *
 * <p>实现要求：</p>
 * <ul>
 *   <li>必须在服务器主线程调用（不得在渲染线程访问库存）。</li>
 *   <li>饰品 API 未安装时 getAccessories() 返回空列表，不得抛异常。</li>
 *   <li>所有返回列表可以为空，但不得为 null。</li>
 * </ul>
 */
public interface PlayerDataViews {

    /** 主手物品；空手返回 isEmpty 的 SlotItem。 */
    SlotItem getMainHand();

    /**
     * 玩家可见库存（显示顺序）：快捷栏 + 主背包 + 盔甲 + 副手。
     * 允许包含空槽位，由核心统一过滤。
     */
    List<SlotItem> getInventory();

    /** 服务端末影箱内容（27 格）。 */
    List<SlotItem> getEnderChest();

    /** 已安装饰品 API 提供的所有非空饰品槽位；未安装任何 API 时为空列表。 */
    List<AccessorySlotData> getAccessories();

    /** 饰品栏中装备的、且能被任何 ItemContainerProvider 识别的背包；找不到时为空列表。 */
    List<BackpackData> getEquippedBackpacks();

    /**
     * 检查一个物品是否为可展开的容器（如 Sophisticated Backpacks）并读取内部库存。
     * 不可识别或读取失败时返回 null（调用方按普通物品处理）。
     */
    BackpackData inspectBackpack(SlotItem stack, int maxDepth);
}
