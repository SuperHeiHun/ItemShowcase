package com.heihun.Itemshowcase.neoforge1211.network;

import com.heihun.Itemshowcase.core.protocol.ClientCapabilities;
import com.heihun.Itemshowcase.neoforge1211.ItemShowcaseNeoForge;
import com.heihun.Itemshowcase.neoforge1211.server.CapabilityTracker;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * C2S：客户端能力握手。
 * 客户端在进入世界后立即发送；服务端据此决定对每个接收者使用结构化包或回退文本。
 */
public final class ClientHandshakePayload implements CustomPacketPayload {

    public static final ClientHandshakePayload INSTANCE = new ClientHandshakePayload();

    public static final Type<ClientHandshakePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ItemShowcaseNeoForge.MOD_ID, "handshake"));

    public static final StreamCodec<FriendlyByteBuf, ClientHandshakePayload> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);

    /** 私有构造：unit codec 绑定单例，发送必须复用 INSTANCE（new 会在编码期抛异常断线）。 */
    private ClientHandshakePayload() {
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** 1.21.1 协议版本的完整能力声明（载荷固定，无字段）。 */
    public static byte[] capabilitiesPayload() {
        return ClientCapabilities.modded().encode();
    }

    public static void handle(ClientHandshakePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof net.minecraft.server.level.ServerPlayer player) {
                CapabilityTracker.set(player.getUUID(), ClientCapabilities.modded());
            }
        });
    }
}
