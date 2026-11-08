package com.heihun.Itemshowcase.core.macros;

/**
 * 聊天展示宏定义。
 *
 * <p>匹配顺序必须按 token 从长到短排列（由 {@link MacroParser} 保证），
 * 否则 `[i]` 会截断 `[inv]`。</p>
 */
public enum MacroType {

    /** 手持物品（主手）。 */
    ITEM("[i]", "[i]"),

    /** 玩家背包（主背包 + 快捷栏 + 盔甲 + 副手）。 */
    INVENTORY("[inv]", "[inv]"),

    /** 末影箱（服务端真实数据）。 */
    ENDER_CHEST("[ec]", "[ec]"),

    /** 饰品栏（动态枚举全部已注册饰品槽位）。 */
    CURIOS("[c]", "[c]"),

    /** 饰品栏中的背包及其内部物品。 */
    PACK("[pack]", "[pack]", "[p]");

    /** 主 token（显示 / 翻译 / 文档用）。 */
    private final String token;
    /** 全部可触发 token（含别名，按声明顺序）。 */
    private final String[] tokens;

    MacroType(String token, String... tokens) {
        this.token = token;
        this.tokens = tokens;
    }

    /** 主宏标记原文，例如 "[i]"。 */
    public String token() {
        return token;
    }

    /** 全部可触发 token（含别名）。 */
    public String[] tokens() {
        return tokens;
    }

    /** 位掩码（用于 ClientCapabilities）。 */
    public int mask() {
        return 1 << ordinal();
    }

    /** 忽略大小写匹配任意一个宏 token（含别名）。 */
    public boolean matchesToken(String text, int offset) {
        for (String candidate : tokens) {
            if (text.regionMatches(true, offset, candidate, 0, candidate.length())) {
                return true;
            }
        }
        return false;
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
