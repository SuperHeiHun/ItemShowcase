package com.heihun.Itemshowcase.neoforge1211.network;

import com.heihun.Itemshowcase.neoforge1211.ItemShowcaseNeoForge;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * S2C：结构化展示消息。
 * payload 为 common-core ShowcaseMessage 的线格式字节
 * （物品数据按 1.21.1 Data Components 方言 dc1211 序列化，客户端按同一方言解码）。
 */
public final class ShowcaseMessagePayload implements CustomPacketPayload {

    public static final Type<ShowcaseMessagePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ItemShowcaseNeoForge.MOD_ID, "showcase_message"));

    public static final StreamCodec<FriendlyByteBuf, ShowcaseMessagePayload> STREAM_CODEC =
            StreamCodec.ofMember(ShowcaseMessagePayload::write, ShowcaseMessagePayload::read);

    public final byte[] showcasePayload;

    public ShowcaseMessagePayload(byte[] showcasePayload) {
        this.showcasePayload = showcasePayload;
    }

    private static void write(ShowcaseMessagePayload payload, FriendlyByteBuf buf) {
        buf.writeByteArray(payload.showcasePayload);
    }

    private static ShowcaseMessagePayload read(FriendlyByteBuf buf) {
        return new ShowcaseMessagePayload(buf.readByteArray());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ShowcaseMessagePayload payload, IPayloadContext context) {
        // 仅客户端处理；enqueueWork 保证主线程
        context.enqueueWork(() -> com.heihun.Itemshowcase.neoforge1211.client.ShowcaseMessageHandler
                .handleShowcaseMessage(payload.showcasePayload));
    }
}
