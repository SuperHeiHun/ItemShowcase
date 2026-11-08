package com.heihun.Itemshowcase.core.macros;

import java.util.ArrayList;
import java.util.List;

/**
 * 聊天宏解析器。
 *
 * <p>规则：</p>
 * <ul>
 *   <li>宏 token：{@code [i] [inv] [ec] [c] [pack]}，忽略大小写。</li>
 *   <li>必须按 token 长度从长到短尝试匹配，避免 `[i]` 破坏 `[inv]`、`[pack]`。</li>
 *   <li>未识别的方括号内容（如 `[x]`、`[item]`）一律保留为普通文字。</li>
 *   <li>文字、空格、格式代码（§ 系列）原样保留；单个宏解析失败不得影响整条消息。</li>
 *   <li>同一条消息支持多个宏、相邻宏、重复宏。</li>
 * </ul>
 */
public final class MacroParser {

    /**
     * 全部 (宏, token) 组合，按 token 长度从长到短排列，保证最长匹配优先
     * （例如 "[pack]" 必须先于 "[p]" 与 "[i]"）。
     */
    private static final MacroType[] MATCH_ORDER = buildMatchOrder();

    private static MacroType[] buildMatchOrder() {
        // 收集所有 (长度, 顺序) 后排序；宏种类与别名数量都很小，无需复杂结构
        List<MacroType> byLength = new ArrayList<>();
        for (MacroType type : MacroType.values()) {
            byLength.add(type);
        }
        byLength.sort((a, b) -> {
            int la = a.token().length();
            int lb = b.token().length();
            return Integer.compare(lb, la); // 主 token 长度降序即可覆盖别名场景
        });
        return byLength.toArray(new MacroType[0]);
    }

    private MacroParser() {
    }

    /** 解析整条聊天消息。null / 空串返回无宏结果。 */
    public static MacroParseResult parse(String input) {
        String safeInput = (input == null) ? "" : input;
        List<ParsedSegment> segments = new ArrayList<>();
        List<MacroType> macrosFound = new ArrayList<>();
        StringBuilder pendingText = new StringBuilder();

        int index = 0;
        int length = safeInput.length();
        while (index < length) {
            char current = safeInput.charAt(index);
            if (current == '[') {
                MacroType matched = matchAt(safeInput, index);
                if (matched != null) {
                    if (pendingText.length() > 0) {
                        segments.add(ParsedSegment.ofText(pendingText.toString()));
                        pendingText.setLength(0);
                    }
                    segments.add(ParsedSegment.ofMacro(matched, index, index + matched.token().length()));
                    macrosFound.add(matched);
                    index += matched.token().length();
                    continue;
                }
            }
            pendingText.append(current);
            index++;
        }
        if (pendingText.length() > 0) {
            segments.add(ParsedSegment.ofText(pendingText.toString()));
        }
        if (segments.isEmpty()) {
            segments.add(ParsedSegment.ofText(""));
        }
        return new MacroParseResult(safeInput, segments, macrosFound);
    }

    /**
     * 快速预检：消息中是否含有任何已定义宏。
     * 平台适配层在聊天事件入口用它过滤绝大多数普通消息，避免无谓的对象分配。
     */
    public static boolean containsMacro(String input) {
        if (input == null || input.isEmpty()) {
            return false;
        }
        int bracket = input.indexOf('[');
        while (bracket >= 0) {
            for (MacroType candidate : MATCH_ORDER) {
                if (candidate.matchesToken(input, bracket)) {
                    return true;
                }
            }
            bracket = input.indexOf('[', bracket + 1);
        }
        return false;
    }

    /** 在指定偏移处尝试匹配任何宏 token；无匹配返回 null。 */
    private static MacroType matchAt(String input, int offset) {
        for (MacroType candidate : MATCH_ORDER) {
            if (candidate.matchesToken(input, offset)) {
                return candidate;
            }
        }
        return null;
    }
}
