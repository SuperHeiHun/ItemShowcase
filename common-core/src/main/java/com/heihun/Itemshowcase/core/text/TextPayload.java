package com.heihun.Itemshowcase.core.text;

import com.heihun.Itemshowcase.core.util.ByteReader;
import com.heihun.Itemshowcase.core.util.ByteWriter;

/**
 * 文本载荷：可序列化的聊天文本。
 *
 * <p>核心模型不依赖具体版本的聊天组件类，因此显示名称 / 分组标题以
 * “Minecraft 聊天组件 JSON 字符串”形式携带（1.16 ~ 1.21.1 的组件 JSON 格式
 * 兼容；1.21.5+ 才换新格式，不在支持范围内）。平台适配层负责各自版本的
 * Component ↔ JSON 转换。</p>
 */
public final class TextPayload {

    public enum Kind {
        /** 字面文本，直接显示。 */
        LITERAL,
        /** 翻译键 + 参数，客户端用本地语言解析。 */
        TRANSLATABLE,
        /** 预序列化的聊天组件 JSON。 */
        JSON
    }

    private final Kind kind;
    /** LITERAL: 文本；TRANSLATABLE: 键；JSON: 组件 JSON。 */
    private final String value;
    /** TRANSLATABLE 时的参数（字符串化；平台适配层负责还原为组件 / 数字）。 */
    private final String[] args;

    private TextPayload(Kind kind, String value, String[] args) {
        this.kind = kind;
        this.value = (value == null) ? "" : value;
        this.args = (args == null) ? new String[0] : args;
    }

    public static TextPayload literal(String text) {
        return new TextPayload(Kind.LITERAL, text, null);
    }

    public static TextPayload translatable(String key, String... args) {
        return new TextPayload(Kind.TRANSLATABLE, key, args);
    }

    public static TextPayload json(String componentJson) {
        return new TextPayload(Kind.JSON, componentJson, null);
    }

    public Kind kind() {
        return kind;
    }

    public String value() {
        return value;
    }

    public String[] args() {
        return args;
    }

    public boolean isEmpty() {
        return value.isEmpty();
    }

    public void encode(ByteWriter writer) {
        writer.writeVarInt(kind.ordinal());
        writer.writeString(value);
        writer.writeVarInt(args.length);
        for (String arg : args) {
            writer.writeString(arg);
        }
    }

    public static TextPayload decode(ByteReader reader) {
        Kind kind = Kind.values()[reader.readVarInt()];
        String value = reader.readString();
        int argCount = reader.readVarInt();
        String[] args = new String[argCount];
        for (int i = 0; i < argCount; i++) {
            args[i] = reader.readString();
        }
        return new TextPayload(kind, value, args);
    }
}
