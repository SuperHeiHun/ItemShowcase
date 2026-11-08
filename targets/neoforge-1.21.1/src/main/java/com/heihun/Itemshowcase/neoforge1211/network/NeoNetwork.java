package com.heihun.Itemshowcase.neoforge1211.network;

import com.heihun.Itemshowcase.core.protocol.ClientCapabilities;
import com.heihun.Itemshowcase.neoforge1211.server.CapabilityTracker;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Showcase 独立网络协议（NeoForge 1.21.1 PayloadRegistrar）。
 *
 * <p>包类型：</p>
 * <ul>
 *   <li>C2S ClientHandshakePayload —— 客户端能力握手</li>
 *   <li>C2S SelectedItemRequestPayload —— Ctrl+Enter 槽位请求（服务端校验后重读真实物品）</li>
 *   <li>S2C ShowcaseMessagePayload —— 结构化展示消息</li>
 * </ul>
 */
public final class NeoNetwork {

    private static final String PROTOCOL_VERSION = "1";

    private NeoNetwork() {
    }

    /** mod 构造期注册（mod bus）。 */
    public static void register(IEventBus modBus) {
        modBus.addListener(NeoNetwork::onRegisterPayloads);
    }

    private static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
        registrar.playToServer(ClientHandshakePayload.TYPE, ClientHandshakePayload.STREAM_CODEC,
                ClientHandshakePayload::handle);
        registrar.playToServer(SelectedItemRequestPayload.TYPE, SelectedItemRequestPayload.STREAM_CODEC,
                SelectedItemRequestPayload::handle);
        registrar.playToClient(ShowcaseMessagePayload.TYPE, ShowcaseMessagePayload.STREAM_CODEC,
                ShowcaseMessagePayload::handle);
    }

    /** 服务端：向单个接收者发送结构化展示包（payload 已是 ShowcaseMessage 线格式裸字节）。 */
    public static void sendShowcaseMessage(ServerPlayer receiver, byte[] showcasePayload) {
        PacketDistributor.sendToPlayer(receiver, new ShowcaseMessagePayload(showcasePayload));
    }

    /** 客户端：发送能力握手（unit codec 要求复用单例实例，不可 new）。 */
    public static void sendHandshake() {
        PacketDistributor.sendToServer(ClientHandshakePayload.INSTANCE);
    }

    /** 客户端：发送 Ctrl+Enter 槽位请求。 */
    public static void sendToServer(SelectedItemRequestPayload payload) {
        PacketDistributor.sendToServer(payload);
    }

    /** 客户端能力校验辅助（服务端用）。 */
    public static boolean isModdedClient(ServerPlayer player) {
        ClientCapabilities capabilities = CapabilityTracker.get(player.getUUID());
        return capabilities != null && capabilities.structuredChatSupported();
    }
}
