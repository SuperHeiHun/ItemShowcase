package com.heihun.Itemshowcase.core.util;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * 与 {@link ByteWriter} 配对的字节读取器。
 *
 * <p>所有越界读取都抛出 {@link FormatException}，由上层决定如何降级，
 * 不允许静默吞掉格式错误。</p>
 */
public final class ByteReader {

    /** 协议格式非法时抛出的异常。 */
    public static final class FormatException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public FormatException(String message) {
            super(message);
        }
    }

    private final byte[] data;
    private int position;

    public ByteReader(byte[] data) {
        this.data = (data == null) ? new byte[0] : data;
    }

    public int remaining() {
        return data.length - position;
    }

    public boolean hasRemaining() {
        return position < data.length;
    }

    private void require(int amount) {
        if (amount < 0 || data.length - position < amount) {
            throw new FormatException("数据不足: 需要 " + amount + " 字节, 剩余 " + (data.length - position));
        }
    }

    public int readByte() {
        require(1);
        return data[position++];
    }

    public boolean readBoolean() {
        return readByte() != 0;
    }

    public int readIntBE() {
        require(4);
        int value = ((data[position] & 0xFF) << 24)
                | ((data[position + 1] & 0xFF) << 16)
                | ((data[position + 2] & 0xFF) << 8)
                | (data[position + 3] & 0xFF);
        position += 4;
        return value;
    }

    public long readLongBE() {
        long high = readIntBE() & 0xFFFFFFFFL;
        long low = readIntBE() & 0xFFFFFFFFL;
        return (high << 32) | low;
    }

    public int readVarInt() {
        int value = 0;
        int shift = 0;
        while (true) {
            int b = readByte();
            value |= (b & 0x7F) << shift;
            if ((b & 0x80) == 0) {
                break;
            }
            shift += 7;
            if (shift > 35) {
                throw new FormatException("varint 编码过长");
            }
        }
        return value;
    }

    public long readVarLong() {
        long value = 0;
        int shift = 0;
        while (true) {
            int b = readByte();
            value |= (long) (b & 0x7F) << shift;
            if ((b & 0x80) == 0) {
                break;
            }
            shift += 7;
            if (shift > 70) {
                throw new FormatException("varlong 编码过长");
            }
        }
        return value;
    }

    public UUID readUuid() {
        long high = readLongBE();
        long low = readLongBE();
        return new UUID(high, low);
    }

    /** 读取“varint 长度 + 内容”的字节数组。 */
    public byte[] readByteArray() {
        int length = readVarInt();
        if (length < 0) {
            throw new FormatException("负数数组长度: " + length);
        }
        require(length);
        byte[] result = new byte[length];
        System.arraycopy(data, position, result, 0, length);
        position += length;
        return result;
    }

    /** 读取已知长度的原始字节（长度已由外部读出，不带前缀）。 */
    public byte[] readRawBytes(int length) {
        if (length < 0) {
            throw new FormatException("负数数组长度: " + length);
        }
        require(length);
        byte[] result = new byte[length];
        System.arraycopy(data, position, result, 0, length);
        position += length;
        return result;
    }

    public String readString() {
        byte[] encoded = readByteArray();
        return new String(encoded, StandardCharsets.UTF_8);
    }

    /** 读取可空字符串（对应 writeNullableString）。 */
    public String readNullableString() {
        if (readBoolean()) {
            return readString();
        }
        return null;
    }

    /** 确认整个载荷被完整消费，防止协议错位后静默通过。 */
    public void expectEnd() {
        if (hasRemaining()) {
            throw new FormatException("载荷末尾存在 " + remaining() + " 字节多余数据");
        }
    }
}
