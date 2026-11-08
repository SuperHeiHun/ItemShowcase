package com.heihun.Itemshowcase.core.model;

import com.heihun.Itemshowcase.core.text.TextPayload;

/**
 * 分组标题片段：在消息中插入一个独立的小标题行
 * （例如“饰品 · ring”“背包 · 旅行背包”）。
 */
public final class SectionSegment extends ShowcaseSegment {

    private final TextPayload title;

    public SectionSegment(TextPayload title) {
        if (title == null) {
            throw new IllegalArgumentException("SectionSegment 标题不能为空");
        }
        this.title = title;
    }

    public TextPayload title() {
        return title;
    }

    @Override
    public Type type() {
        return Type.SECTION;
    }
}
