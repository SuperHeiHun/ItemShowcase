package com.heihun.Itemshowcase.core.model;

/**
 * 展示消息的片段基类。
 * 具体类型：{@link TextSegment}、{@link ItemSegment}、{@link ItemGroupSegment}、{@link SectionSegment}。
 */
public abstract class ShowcaseSegment {

    /** 片段类型，写入协议时使用。 */
    public enum Type {
        TEXT(0), ITEM(1), ITEM_GROUP(2), SECTION(3);

        private final int wireId;

        Type(int wireId) {
            this.wireId = wireId;
        }

        public int wireId() {
            return wireId;
        }

        public static Type byWireId(int id) {
            for (Type type : values()) {
                if (type.wireId == id) {
                    return type;
                }
            }
            return null;
        }
    }

    public abstract Type type();
}
