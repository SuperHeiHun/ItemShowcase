package com.heihun.Itemshowcase.core.util;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * 简单字节写出器，提供 Minecraft 风格的 varint / varlong 编码。
 *
 * <p>仅服务于 common-core 内部的 Envelope 与 Showcase 载荷编解码；
 * ItemStack 的序列化由各版本适配器完成，不经过本类。</p>
 */
public final class ByteWriter {

    private byte[] buffer;
    private int count;

    public ByteWriter() {
        this(64);
    }

    public ByteWriter(int initialCapacity) {
        this.buffer = new byte[Math.max(16, initialCapacity)];
    }

    private void ensure(int extra) {
        if (count + extra > buffer.length) {
            int newCapacity = Math.max(buffer.length * 2, count + extra);
            byte[] grown = new byte[newCapacity];
            System.arraycopy(buffer, 0, grown, 0, count);
            buffer = grown;
        }
    }

    public ByteWriter writeByte(int value) {
        ensure(1);
        buffer[count++] = (byte) value;
        return this;
    }

    public ByteWriter writeBoolean(boolean value) {
        return writeByte(value ? 1 : 0);
    }

    /** 大端 32 位整数（仅用于魔数等固定字段；可变长度字段一律用 varint）。 */
    public ByteWriter writeIntBE(int value) {
        ensure(4);
        buffer[count] = (byte) (value >>> 24);
        buffer[count + 1] = (byte) (value >>> 16);
        buffer[count + 2] = (byte) (value >>> 8);
        buffer[count + 3] = (byte) value;
        count += 4;
        return this;
    }

    /** 大端 64 位整数（用于时间戳等固定字段）。 */
    public ByteWriter writeLongBE(long value) {
        writeIntBE((int) (value >>> 32));
        writeIntBE((int) value);
        return this;
    }

    /** Minecraft 风格无符号 varint，可完整表示任意 32 位值（含负数）。 */
    public ByteWriter writeVarInt(int value) {
        while ((value & ~0x7F) != 0) {
            writeByte((value & 0x7F) | 0x80);
            value >>>= 7;
        }
        writeByte(value);
        return this;
    }

    public ByteWriter writeVarLong(long value) {
        while ((value & ~0x7FL) != 0) {
            writeByte((int) ((value & 0x7F) | 0x80));
            value >>>= 7;
        }
        writeByte((int) value);
        return this;
    }

    public ByteWriter writeUuid(UUID value) {
        if (value == null) {
            value = new UUID(0L, 0L);
        }
        writeLongBE(value.getMostSignificantBits());
        writeLongBE(value.getLeastSignificantBits());
        return this;
    }

    /** 原样写入给定字节，不写入长度前缀。 */
    public ByteWriter writeRawBytes(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return this;
        }
        ensure(bytes.length);
        System.arraycopy(bytes, 0, buffer, count, bytes.length);
        count += bytes.length;
        return this;
    }

    /** 写入“varint 长度 + 内容”。null 视为空数组。 */
    public ByteWriter writeByteArray(byte[] bytes) {
        if (bytes == null) {
            writeVarInt(0);
            return this;
        }
        writeVarInt(bytes.length);
        writeRawBytes(bytes);
        return this;
    }

    /** 写入“varint UTF-8 字节长度 + 内容”。 */
    public ByteWriter writeString(String value) {
        String safe = (value == null) ? "" : value;
        byte[] encoded = safe.getBytes(StandardCharsets.UTF_8);
        if (encoded.length > com.heihun.Itemshowcase.core.ItemShowcaseConstants.MAX_STRING_BYTES) {
            throw new IllegalArgumentException("字符串超过协议上限: " + encoded.length + " 字节");
        }
        writeVarInt(encoded.length);
        writeRawBytes(encoded);
        return this;
    }

    /** 写入可空字符串：1 字节存在标志 + 内容。 */
    public ByteWriter writeNullableString(String value) {
        if (value == null) {
            writeBoolean(false);
        } else {
            writeBoolean(true);
            writeString(value);
        }
        return this;
    }

    public int size() {
        return count;
    }

    public byte[] toByteArray() {
        byte[] result = new byte[count];
        System.arraycopy(buffer, 0, result, 0, count);
        return result;
    }
}
