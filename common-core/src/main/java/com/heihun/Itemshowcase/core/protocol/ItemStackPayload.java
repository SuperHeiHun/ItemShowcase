package com.heihun.Itemshowcase.core.protocol;

import com.heihun.Itemshowcase.core.util.ByteReader;
import com.heihun.Itemshowcase.core.util.ByteWriter;

/**
 * 序列化后的 ItemStack 载荷。
 *
 * <p>核心模型不直接依赖任何版本的 ItemStack 类：各版本适配器负责把真实
 * ItemStack 编码为字节数组（1.16.5 / 1.20.1 用物品 NBT，1.21.1 用
 * Data Components 兼容编码），并用 {@code dialect} 标明编码方言。</p>
 *
 * <p>客户端解码时按方言分发，不匹配的方言直接丢弃并记录日志。</p>
 */
public final class ItemStackPayload {

    /** 载荷格式版本（物品编码格式的内部修订号，与协议版本独立）。 */
    private final int formatVersion;

    /** 编码方言，见 ItemShowcaseConstants.DIALECT_*。 */
    private final String dialect;

    /** 平台编码的物品数据。 */
    private final byte[] data;

    /** 物品数量（>0）。 */
    private final int count;

    /** 注册表 ID 诊断信息（itemshowcase 不按它做白名单，仅用于日志与统计）。 */
    private final String itemIdHint;

    public ItemStackPayload(int formatVersion, String dialect, byte[] data, int count, String itemIdHint) {
        this.formatVersion = formatVersion;
        this.dialect = (dialect == null) ? "" : dialect;
        this.data = (data == null) ? new byte[0] : data;
        this.count = count;
        this.itemIdHint = (itemIdHint == null) ? "" : itemIdHint;
    }

    public int formatVersion() {
        return formatVersion;
    }

    public String dialect() {
        return dialect;
    }

    public byte[] data() {
        return data;
    }

    public int count() {
        return count;
    }

    public String itemIdHint() {
        return itemIdHint;
    }

    public int sizeBytes() {
        return data.length;
    }

    public ItemStackPayload withCount(int newCount) {
        return new ItemStackPayload(formatVersion, dialect, data, newCount, itemIdHint);
    }

    public void encode(ByteWriter writer) {
        writer.writeVarInt(formatVersion);
        writer.writeString(dialect);
        writer.writeByteArray(data);
        writer.writeVarInt(count);
        writer.writeString(itemIdHint);
    }

    public static ItemStackPayload decode(ByteReader reader) {
        int formatVersion = reader.readVarInt();
        String dialect = reader.readString();
        byte[] data = reader.readByteArray();
        int count = reader.readVarInt();
        String itemIdHint = reader.readString();
        return new ItemStackPayload(formatVersion, dialect, data, count, itemIdHint);
    }
}
