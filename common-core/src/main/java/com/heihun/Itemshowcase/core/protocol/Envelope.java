package com.heihun.Itemshowcase.core.protocol;

import java.util.UUID;

/**
 * Showcase 协议信封：所有包共用的最外层结构。
 *
 * <pre>线格式（由 {@link EnvelopeCodec} 编解码）：
 * intBE magic | varint protocolVersion | varint typeId | UUID requestId
 * | varint payloadLength | payload bytes</pre>
 */
public final class Envelope {

    private final int protocolVersion;
    private final PacketType type;
    private final UUID requestId;
    private final byte[] payload;

    public Envelope(int protocolVersion, PacketType type, UUID requestId, byte[] payload) {
        if (type == null) {
            throw new IllegalArgumentException("PacketType 不能为空");
        }
        this.protocolVersion = protocolVersion;
        this.type = type;
        this.requestId = (requestId == null) ? new UUID(0L, 0L) : requestId;
        this.payload = (payload == null) ? new byte[0] : payload;
    }

    public int protocolVersion() {
        return protocolVersion;
    }

    public PacketType type() {
        return type;
    }

    public UUID requestId() {
        return requestId;
    }

    /** 返回载荷的字节数组，内容不应被修改。 */
    public byte[] payload() {
        return payload;
    }

    /** 便捷构造：普通展示消息。requestId 通常为空 UUID。 */
    public static Envelope message(PacketType type, byte[] payload) {
        return new Envelope(com.heihun.Itemshowcase.core.ItemShowcaseConstants.PROTOCOL_VERSION, type, new UUID(0L, 0L), payload);
    }

    /** 便捷构造：带请求标识的请求/响应包。 */
    public static Envelope request(PacketType type, UUID requestId, byte[] payload) {
        return new Envelope(com.heihun.Itemshowcase.core.ItemShowcaseConstants.PROTOCOL_VERSION, type, requestId, payload);
    }
}
