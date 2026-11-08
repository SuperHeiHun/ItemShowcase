package com.heihun.Itemshowcase.core.protocol;

import java.util.UUID;

import com.heihun.Itemshowcase.core.util.ByteReader;
import com.heihun.Itemshowcase.core.util.ByteWriter;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProtocolTest {

    @Test
    void envelopeRoundTrip() {
        byte[] payload = new byte[] { 1, 2, 3, 4, 5 };
        UUID requestId = UUID.randomUUID();
        Envelope envelope = Envelope.request(PacketType.SELECTED_ITEM_REQUEST, requestId, payload);
        byte[] encoded = EnvelopeCodec.encode(envelope);
        Envelope decoded = EnvelopeCodec.decode(encoded);

        assertEquals(PacketType.SELECTED_ITEM_REQUEST, decoded.type());
        assertEquals(requestId, decoded.requestId());
        assertArrayEquals(payload, decoded.payload());
        assertEquals(envelope.protocolVersion(), decoded.protocolVersion());
    }

    @Test
    void envelopeRejectsWrongMagic() {
        byte[] corrupted = EnvelopeCodec.encode(Envelope.message(PacketType.SHOWCASE_MESSAGE, new byte[] { 9 }));
        corrupted[0] = 'X';
        assertThrows(ByteReader.FormatException.class, () -> EnvelopeCodec.decode(corrupted));
    }

    @Test
    void envelopeRejectsUnknownType() {
        com.heihun.Itemshowcase.core.util.ByteWriter writer = new com.heihun.Itemshowcase.core.util.ByteWriter();
        writer.writeIntBE(com.heihun.Itemshowcase.core.ItemShowcaseConstants.ENVELOPE_MAGIC);
        writer.writeVarInt(com.heihun.Itemshowcase.core.ItemShowcaseConstants.PROTOCOL_VERSION);
        writer.writeVarInt(999);
        writer.writeUuid(new UUID(0, 0));
        writer.writeVarInt(0);
        byte[] bogus = writer.toByteArray();
        assertThrows(ByteReader.FormatException.class, () -> EnvelopeCodec.decode(bogus));
    }

    @Test
    void envelopeRejectsTruncatedPayload() {
        byte[] encoded = EnvelopeCodec.encode(Envelope.message(PacketType.SHOWCASE_MESSAGE, new byte[] { 1, 2, 3 }));
        byte[] truncated = new byte[encoded.length - 1];
        System.arraycopy(encoded, 0, truncated, 0, truncated.length);
        assertThrows(Exception.class, () -> EnvelopeCodec.decode(truncated));
    }

    @Test
    void capabilitiesRoundTrip() {
        ClientCapabilities caps = ClientCapabilities.modded();
        byte[] encoded = caps.encode();
        ClientCapabilities decoded = ClientCapabilities.decode(encoded);
        assertTrue(decoded.structuredChatSupported());
        assertTrue(decoded.iconRenderingSupported());
        for (com.heihun.Itemshowcase.core.macros.MacroType type : com.heihun.Itemshowcase.core.macros.MacroType.values()) {
            assertTrue(decoded.supportsMacro(type), "应支持宏 " + type);
        }
    }

    @Test
    void capabilitiesRejectForeignProtocolVersion() {
        ClientCapabilities caps = new ClientCapabilities(true, true, 0xFF, 999);
        ClientCapabilities decoded = ClientCapabilities.decode(caps.encode());
        // 协议版本不匹配 → 按原版客户端处理，走回退文本
        assertNotNull(decoded);
        assertTrue(!decoded.structuredChatSupported());
    }

    @Test
    void itemStackPayloadRoundTrip() {
        byte[] data = new byte[] { 0x0A, 0x0B, 0x0C };
        ItemStackPayload payload = new ItemStackPayload(1, com.heihun.Itemshowcase.core.ItemShowcaseConstants.DIALECT_NBT_1_20,
                data, 64, "minecraft:diamond");
        com.heihun.Itemshowcase.core.util.ByteWriter writer = new com.heihun.Itemshowcase.core.util.ByteWriter();
        payload.encode(writer);
        ItemStackPayload decoded = ItemStackPayload.decode(new com.heihun.Itemshowcase.core.util.ByteReader(writer.toByteArray()));

        assertEquals(1, decoded.formatVersion());
        assertEquals(com.heihun.Itemshowcase.core.ItemShowcaseConstants.DIALECT_NBT_1_20, decoded.dialect());
        assertArrayEquals(data, decoded.data());
        assertEquals(64, decoded.count());
        assertEquals("minecraft:diamond", decoded.itemIdHint());
        assertEquals(3, decoded.sizeBytes());
    }
}
