package com.heihun.Itemshowcase.forge1201.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * S2C：结构化展示消息。
 * payload 为 common-core ShowcaseMessage 的线格式字节
 * （物品数据按 1.20.1 NBT 方言序列化，客户端按同一方言解码）。
 */
public final class ShowcaseMessagePacket {

    private final byte[] showcasePayload;

    public ShowcaseMessagePacket(byte[] showcasePayload) {
        this.showcasePayload = showcasePayload;
    }

    public static void encode(ShowcaseMessagePacket packet, FriendlyByteBuf buf) {
        buf.writeByteArray(packet.showcasePayload);
    }

    public static ShowcaseMessagePacket decode(FriendlyByteBuf buf) {
        return new ShowcaseMessagePacket(buf.readByteArray());
    }

    public static void handle(ShowcaseMessagePacket packet, Supplier<NetworkEvent.Context> ctx) {
        // 仅客户端处理；安全引用客户端类，dedicated server 不会加载
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> com.heihun.Itemshowcase.forge1201.client.ShowcaseMessageHandler
                        .handleShowcaseMessage(packet.showcasePayload));
    }
}
