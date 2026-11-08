package com.heihun.Itemshowcase.core.spi;

import java.util.List;

/**
 * 饰品 API 集成接口（Curios / Trinkets / Accessories 各版本各实现一个）。
 *
 * <p>实现必须动态枚举已注册的饰品槽位——不允许硬编码 ring、necklace、back
 * 等槽位名称。API 未安装时 {@link #isAvailable()} 返回 false。</p>
 */
public interface AccessoryProvider {

    /** 集成标识（"curios" / "trinkets" / "accessories"）。 */
    String id();

    /** 该饰品 API 是否已加载（运行时检测）。 */
    boolean isAvailable();

    /**
     * 读取玩家的全部非空饰品槽位。
     * 同一玩家可同时被多个饰品 API 覆盖（不同 MOD 注册不同槽位），实现间互不干扰。
     * 任何失败都应返回空列表并记录日志，不允许抛异常。
     *
     * @param platformPlayer 平台原生玩家对象（由适配器创建时已知类型，向下转型）
     */
    List<AccessorySlotData> getSlots(Object platformPlayer);
}
