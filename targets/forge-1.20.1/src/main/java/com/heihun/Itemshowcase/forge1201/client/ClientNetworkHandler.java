package com.heihun.Itemshowcase.forge1201.client;

import com.heihun.Itemshowcase.core.protocol.ClientCapabilities;
import com.heihun.Itemshowcase.forge1201.ItemShowcaseMod;
import com.heihun.Itemshowcase.forge1201.network.ShowcaseNetwork;
import com.heihun.Itemshowcase.forge1201.network.packet.ClientHandshakePacket;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 客户端网络事件：进入世界后发送能力握手。
 * 服务端据此把该玩家标记为“结构化展示客户端”。
 */
@Mod.EventBusSubscriber(modid = ItemShowcaseMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ClientNetworkHandler {

    private ClientNetworkHandler() {
    }

    @SubscribeEvent
    public static void onLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
        // 握手声明：支持结构化聊天 + 图标渲染 + 全部宏
        ShowcaseNetwork.CHANNEL.sendToServer(new ClientHandshakePacket(ClientCapabilities.modded().encode()));
    }
}
