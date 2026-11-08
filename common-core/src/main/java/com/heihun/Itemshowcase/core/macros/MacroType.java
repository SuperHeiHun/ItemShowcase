package com.heihun.Itemshowcase.core.macros;

/**
 * 聊天展示宏定义。
 *
 * <p>匹配顺序必须按 token 从长到短排列（由 {@link MacroParser} 保证），
 * 否则 `[i]` 会截断 `[inv]`。</p>
 */
public enum MacroType {

    /** 手持物品（主手）。 */
    ITEM("[i]"),

    /** 玩家背包（主背包 + 快捷栏 + 盔甲 + 副手）。 */
    INVENTORY("[inv]"),

    /** 末影箱（服务端真实数据）。 */
    ENDER_CHEST("[ec]"),

    /** 饰品栏（动态枚举全部已注册饰品槽位）。 */
    CURIOS("[c]"),

    /** 饰品栏中的背包及其内部物品。 */
    PACK("[pack]");

    private final String token;

    MacroType(String token) {
        this.token = token;
    }

    /** 宏标记原文，例如 "[i]"。 */
    public String token() {
        return token;
    }

    /** 位掩码（用于 ClientCapabilities）。 */
    public int mask() {
        return 1 << ordinal();
    }

    /** 忽略大小写匹配宏 token。 */
    public boolean matchesToken(String text, int offset) {
        return text.regionMatches(true, offset, token, 0, token.length());
    }

    /** 翻译键：该宏的展示标题（客户端 / 服务端语言文件各自解析）。 */
    public String titleKey() {
        return "itemshowcase.macro." + name().toLowerCase(java.util.Locale.ROOT) + ".title";
    }

    /** 翻译键：宏被服务端禁用时的提示。 */
    public String disabledKey() {
        return "itemshowcase.macro." + name().toLowerCase(java.util.Locale.ROOT) + ".disabled";
    }

    /** 翻译键：宏执行后没有内容可展示时的提示。 */
    public String emptyKey() {
        return "itemshowcase.macro." + name().toLowerCase(java.util.Locale.ROOT) + ".empty";
    }

    /** 按 token 精确（忽略大小写）查找宏类型。 */
    public static MacroType byToken(String token) {
        if (token == null) {
            return null;
        }
        for (MacroType type : values()) {
            if (type.token.equalsIgnoreCase(token)) {
                return type;
            }
        }
        return null;
    }
}
