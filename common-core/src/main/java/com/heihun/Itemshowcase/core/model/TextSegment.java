package com.heihun.Itemshowcase.core.model;

import com.heihun.Itemshowcase.core.text.TextPayload;

/**
 * 普通文字片段：原样保留玩家消息中的文字（含空格与格式代码），
 * 或系统插入的提示文本（字面 / 翻译键）。
 */
public final class TextSegment extends ShowcaseSegment {

    private final TextPayload payload;

    public TextSegment(TextPayload payload) {
        if (payload == null) {
            throw new IllegalArgumentException("TextPayload 不能为空");
        }
        this.payload = payload;
    }

    /** 便捷构造：字面文本。 */
    public TextSegment(String literal) {
        this(TextPayload.literal(literal));
    }

    public TextPayload payload() {
        return payload;
    }

    @Override
    public Type type() {
        return Type.TEXT;
    }
}
