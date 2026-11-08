package com.heihun.Itemshowcase.core.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.heihun.Itemshowcase.core.text.TextPayload;

/**
 * 一组物品片段：一次宏展开产生的物品集合，可带标题（例如饰品槽位类型名、
 * 背包名）。客户端按组渲染，组间有间距。
 */
public final class ItemGroupSegment extends ShowcaseSegment {

    private final SourceKind source;
    private final TextPayload title;
    private final List<ItemSegment> items;

    public ItemGroupSegment(SourceKind source, TextPayload title, List<ItemSegment> items) {
        if (source == null) {
            throw new IllegalArgumentException("SourceKind 不能为空");
        }
        this.source = source;
        this.title = title;
        this.items = Collections.unmodifiableList(new ArrayList<>(items));
    }

    public SourceKind source() {
        return source;
    }

    /** 组标题（可空，例如普通背包物品组不显示标题）。 */
    public TextPayload title() {
        return title;
    }

    public List<ItemSegment> items() {
        return items;
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    @Override
    public Type type() {
        return Type.ITEM_GROUP;
    }
}
