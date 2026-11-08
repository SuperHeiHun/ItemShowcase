package com.heihun.Itemshowcase.core.macros;

/**
 * 解析结果中的一个片段：普通文字或宏标记。
 */
public final class ParsedSegment {

    public enum Kind {
        TEXT, MACRO
    }

    private final Kind kind;
    private final String text;
    private final MacroType macro;
    private final int start;
    private final int end;

    private ParsedSegment(Kind kind, String text, MacroType macro, int start, int end) {
        this.kind = kind;
        this.text = text;
        this.macro = macro;
        this.start = start;
        this.end = end;
    }

    public static ParsedSegment ofText(String text) {
        return new ParsedSegment(Kind.TEXT, text, null, -1, -1);
    }

    public static ParsedSegment ofMacro(MacroType macro, int start, int end) {
        return new ParsedSegment(Kind.MACRO, null, macro, start, end);
    }

    public Kind kind() {
        return kind;
    }

    public boolean isText() {
        return kind == Kind.TEXT;
    }

    public boolean isMacro() {
        return kind == Kind.MACRO;
    }

    /** 仅在 kind == TEXT 时非 null。 */
    public String text() {
        return text;
    }

    /** 仅在 kind == MACRO 时非 null。 */
    public MacroType macro() {
        return macro;
    }

    /** 片段在原始消息中的起始偏移（TEXT 片段为 -1）。 */
    public int start() {
        return start;
    }

    /** 片段在原始消息中的结束偏移（不含，TEXT 片段为 -1）。 */
    public int end() {
        return end;
    }
}
