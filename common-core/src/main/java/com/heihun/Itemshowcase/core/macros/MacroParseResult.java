package com.heihun.Itemshowcase.core.macros;

import java.util.Collections;
import java.util.List;

/**
 * 宏解析结果：完整保留原始消息与片段顺序。
 *
 * <p>即使消息中没有任何宏，也返回一个包含整条文本的结果，
 * 保证调用方不需要做特殊分支。</p>
 */
public final class MacroParseResult {

    private final String original;
    private final List<ParsedSegment> segments;
    private final List<MacroType> macrosFound;

    public MacroParseResult(String original, List<ParsedSegment> segments, List<MacroType> macrosFound) {
        this.original = (original == null) ? "" : original;
        this.segments = Collections.unmodifiableList(segments);
        this.macrosFound = Collections.unmodifiableList(macrosFound);
    }

    public String original() {
        return original;
    }

    public List<ParsedSegment> segments() {
        return segments;
    }

    public List<MacroType> macrosFound() {
        return macrosFound;
    }

    public boolean hasMacros() {
        return !macrosFound.isEmpty();
    }
}
