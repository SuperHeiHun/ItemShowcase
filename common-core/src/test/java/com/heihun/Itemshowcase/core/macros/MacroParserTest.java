package com.heihun.Itemshowcase.core.macros;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MacroParserTest {

    private static boolean isTextAt(MacroParseResult result, int index, String expected) {
        ParsedSegment segment = result.segments().get(index);
        return segment.isText() && expected.equals(segment.text());
    }

    private static boolean isMacroAt(MacroParseResult result, int index, MacroType type) {
        ParsedSegment segment = result.segments().get(index);
        return segment.isMacro() && segment.macro() == type;
    }

    @Test
    void parsesSingleMacro() {
        MacroParseResult result = MacroParser.parse("[i]");
        assertEquals(1, result.segments().size());
        assertTrue(isMacroAt(result, 0, MacroType.ITEM));
        assertTrue(result.hasMacros());
    }

    @Test
    void preservesChineseTextAndWhitespace() {
        MacroParseResult result = MacroParser.parse("看看我今天的装备 [i] [c]");
        assertEquals(4, result.segments().size());
        assertTrue(isTextAt(result, 0, "看看我今天的装备 "));
        assertTrue(isMacroAt(result, 1, MacroType.ITEM));
        assertTrue(isTextAt(result, 2, " "));
        assertTrue(isMacroAt(result, 3, MacroType.CURIOS));
    }

    @Test
    void parsesMultipleAndAdjacentMacros() {
        MacroParseResult result = MacroParser.parse("[inv][ec][pack][i]");
        assertEquals(4, result.segments().size());
        assertTrue(isMacroAt(result, 0, MacroType.INVENTORY));
        assertTrue(isMacroAt(result, 1, MacroType.ENDER_CHEST));
        assertTrue(isMacroAt(result, 2, MacroType.PACK));
        assertTrue(isMacroAt(result, 3, MacroType.ITEM));
        assertEquals(4, result.macrosFound().size());
    }

    @Test
    void isCaseInsensitiveButPreservesOriginalInText() {
        MacroParseResult result = MacroParser.parse("看看 [I] 和 [INV]");
        assertTrue(isMacroAt(result, 1, MacroType.ITEM));
        assertTrue(isMacroAt(result, 3, MacroType.INVENTORY));
        assertEquals("看看 [I] 和 [INV]", result.original());
    }

    @Test
    void unknownBracketsRemainLiteralText() {
        MacroParseResult result = MacroParser.parse("数组 [0] 不是宏 [item] [x]");
        assertEquals(1, result.segments().size());
        assertTrue(isTextAt(result, 0, "数组 [0] 不是宏 [item] [x]"));
        assertFalse(result.hasMacros());
    }

    @Test
    void unmatchedBracketDoesNotBreakParse() {
        MacroParseResult result = MacroParser.parse("[i 和 [inv] 不闭合");
        assertEquals(3, result.segments().size());
        assertTrue(isTextAt(result, 0, "[i 和 "));
        assertTrue(isMacroAt(result, 1, MacroType.INVENTORY));
        assertTrue(isTextAt(result, 2, " 不闭合"));
    }

    @Test
    void repeatedMacrosAllRecognized() {
        MacroParseResult result = MacroParser.parse("[i] [i] [i]");
        assertEquals(5, result.segments().size());
        assertEquals(3, result.macrosFound().size());
    }

    @Test
    void nullAndEmptyInputAreSafe() {
        MacroParseResult nullResult = MacroParser.parse(null);
        assertFalse(nullResult.hasMacros());
        assertEquals("", nullResult.original());
        MacroParseResult emptyResult = MacroParser.parse("");
        assertFalse(emptyResult.hasMacros());
    }

    @Test
    void containsMacroQuickCheck() {
        assertTrue(MacroParser.containsMacro("普通消息 [i]"));
        assertTrue(MacroParser.containsMacro("[PACK]"));
        assertFalse(MacroParser.containsMacro("完全普通的消息"));
        assertFalse(MacroParser.containsMacro("[unclosed"));
        assertFalse(MacroParser.containsMacro(null));
    }

    @Test
    void formatCodesArePreservedInsideText() {
        MacroParseResult result = MacroParser.parse("§a绿色文字 [i]§r 恢复");
        assertTrue(isTextAt(result, 0, "§a绿色文字 "));
        assertTrue(isMacroAt(result, 1, MacroType.ITEM));
        assertTrue(isTextAt(result, 2, "§r 恢复"));
    }

    @Test
    void macroTypeTokenHelpers() {
        assertEquals(MacroType.PACK, MacroType.byToken("[pack]"));
        assertEquals(MacroType.CURIOS, MacroType.byToken("[C]"));
        assertEquals(null, MacroType.byToken("[nope]"));
        assertTrue(MacroType.ITEM.mask() != MacroType.INVENTORY.mask());
    }
}
