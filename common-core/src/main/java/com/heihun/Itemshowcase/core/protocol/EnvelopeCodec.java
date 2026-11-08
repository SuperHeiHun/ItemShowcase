package com.heihun.Itemshowcase.core.protocol;

import com.heihun.Itemshowcase.core.ItemShowcaseConstants;
import com.heihun.Itemshowcase.core.util.ByteReader;
import com.heihun.Itemshowcase.core.util.ByteWriter;

/**
 * {@link Envelope} 的线格式编解码。
 *
 * <p>解码是防御性的：魔数、协议版本、包类型、长度一致性全部校验，
 * 任何不匹配都抛出 {@link ByteReader.FormatException}，由平台适配层决定丢弃并记录日志。</p>
 */
public final class EnvelopeCodec {

    private EnvelopeCodec() {
    }

    public static byte[] encode(Envelope envelope) {
        ByteWriter writer = new ByteWriter(envelope.payload().length + 32);
        writer.writeIntBE(ItemShowcaseConstants.ENVELOPE_MAGIC);
        writer.writeVarInt(envelope.protocolVersion());
        writer.writeVarInt(envelope.type().id());
        writer.writeUuid(envelope.requestId());
        writer.writeVarInt(envelope.payload().length);
        writer.writeRawBytes(envelope.payload());
        return writer.toByteArray();
    }

    public static Envelope decode(byte[] raw) {
        ByteReader reader = new ByteReader(raw);
        int magic = reader.readIntBE();
        if (magic != ItemShowcaseConstants.ENVELOPE_MAGIC) {
            throw new ByteReader.FormatException("Envelope 魔数不匹配: 0x" + Integer.toHexString(magic));
        }
        int protocolVersion = reader.readVarInt();
        if (protocolVersion != ItemShowcaseConstants.PROTOCOL_VERSION) {
            throw new ByteReader.FormatException(
                    "协议版本不匹配: 收到 " + protocolVersion + ", 当前 " + ItemShowcaseConstants.PROTOCOL_VERSION);
        }
        int typeId = reader.readVarInt();
        PacketType type = PacketType.byId(typeId);
        if (type == null) {
            throw new ByteReader.FormatException("未知包类型 ID: " + typeId);
        }
        java.util.UUID requestId = reader.readUuid();
        int payloadLength = reader.readVarInt();
        if (payloadLength < 0 || payloadLength != reader.remaining()) {
            throw new ByteReader.FormatException(
                    "载荷长度不一致: 声明 " + payloadLength + ", 实际剩余 " + reader.remaining());
        }
        // 长度已在上面单独读出，这里必须按已知长度读原始字节
        byte[] payload = reader.readRawBytes(payloadLength);
        reader.expectEnd();
        return new Envelope(protocolVersion, type, requestId, payload);
    }
}
