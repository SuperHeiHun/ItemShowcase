package com.heihun.Itemshowcase.forge1201.network.packet;

import com.heihun.Itemshowcase.core.protocol.ClientCapabilities;
import com.heihun.Itemshowcase.forge1201.server.CapabilityTracker;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * C2S：客户端能力握手。
 * 客户端在进入世界后立即发送；服务端据此决定对每个接收者使用结构化包或回退文本。
 */
public final class ClientHandshakePacket {

    private final byte[] capabilitiesPayload;

    public ClientHandshakePacket(byte[] capabilitiesPayload) {
        this.capabilitiesPayload = capabilitiesPayload;
    }

    public static void encode(ClientHandshakePacket packet, FriendlyByteBuf buf) {
        buf.writeByteArray(packet.capabilitiesPayload);
    }

    public static ClientHandshakePacket decode(FriendlyByteBuf buf) {
        return new ClientHandshakePacket(buf.readByteArray());
    }

    public static void handle(ClientHandshakePacket packet, Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer sender = ctx.get().getSender();
        if (sender == null) {
            return;
        }
        ClientCapabilities capabilities;
        try {
            capabilities = ClientCapabilities.decode(packet.capabilitiesPayload);
        } catch (RuntimeException malformed) {
            // 非法握手一律按原版客户端处理，不影响其他玩家
            capabilities = ClientCapabilities.vanilla();
        }
        CapabilityTracker.set(sender.getUUID(), capabilities);
    }
}
