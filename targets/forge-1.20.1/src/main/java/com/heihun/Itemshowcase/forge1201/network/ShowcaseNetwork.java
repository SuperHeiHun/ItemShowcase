package com.heihun.Itemshowcase.forge1201.network;

import com.heihun.Itemshowcase.forge1201.ItemShowcaseMod;
import com.heihun.Itemshowcase.forge1201.network.packet.ClientHandshakePacket;
import com.heihun.Itemshowcase.forge1201.network.packet.SelectedItemRequestPacket;
import com.heihun.Itemshowcase.forge1201.network.packet.ShowcaseMessagePacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * Showcase 独立网络协议（itemshowcase:main）。
 *
 * <p>包类型：</p>
 * <ul>
 *   <li>C2S ClientHandshakePacket —— 客户端能力握手（决定服务端发结构化包还是回退文本）</li>
 *   <li>C2S SelectedItemRequestPacket —— Ctrl+Enter 槽位请求（服务端校验后重读真实物品）</li>
 *   <li>S2C ShowcaseMessagePacket —— 结构化展示消息</li>
 * </ul>
 */
public final class ShowcaseNetwork {

    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(ItemShowcaseMod.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals);

    private static int packetId = 0;

    private ShowcaseNetwork() {
    }

    public static void register() {
        CHANNEL.messageBuilder(ClientHandshakePacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ClientHandshakePacket::encode)
                .decoder(ClientHandshakePacket::decode)
                .consumerMainThread(ClientHandshakePacket::handle)
                .add();

        CHANNEL.messageBuilder(SelectedItemRequestPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SelectedItemRequestPacket::encode)
                .decoder(SelectedItemRequestPacket::decode)
                .consumerMainThread(SelectedItemRequestPacket::handle)
                .add();

        CHANNEL.messageBuilder(ShowcaseMessagePacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ShowcaseMessagePacket::encode)
                .decoder(ShowcaseMessagePacket::decode)
                .consumerMainThread(ShowcaseMessagePacket::handle)
                .add();
    }

    /** 服务端：向单个接收者发送包。 */
    public static void sendTo(ServerPlayer player, Object packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }
}
