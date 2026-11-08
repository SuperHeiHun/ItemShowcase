package com.heihun.Itemshowcase.core.model;

/**
 * 物品在展示消息中的来源分类。
 * 用于客户端分组显示与来源标注，不参与任何物品识别白名单逻辑。
 */
public enum SourceKind {

    /** 主手物品（[i] 宏）。 */
    HAND,

    /** 玩家背包（[inv] 宏，含快捷栏 / 主背包）。 */
    INVENTORY,

    /** 盔甲栏。 */
    ARMOR,

    /** 副手。 */
    OFFHAND,

    /** 末影箱（[ec] 宏）。 */
    ENDER_CHEST,

    /** 饰品栏（[c] 宏）。 */
    CURIO,

    /** 饰品栏中装备的背包内部物品（[pack] 宏或 [i] 的容器展开）。 */
    BACKPACK;

    /** 翻译键：来源分组标题。 */
    public String titleKey() {
        return "itemshowcase.source." + name().toLowerCase(java.util.Locale.ROOT);
    }
}
