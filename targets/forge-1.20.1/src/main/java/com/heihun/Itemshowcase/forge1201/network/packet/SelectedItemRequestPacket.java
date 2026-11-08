package com.heihun.Itemshowcase.forge1201.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * C2S：Ctrl+Enter 快捷展示请求。
 *
 * <p>客户端只提交“打开的菜单 ID + 槽位 ID”，不提交任何 ItemStack 数据。
 * 服务端必须校验：</p>
 * <ol>
 *   <li>玩家当前确实打开了该 ID 的菜单（防伪造）。</li>
 *   <li>槽位属于玩家自己的库存（背包/快捷栏/盔甲/副手），不允许读取容器其他物品。</li>
 *   <li>从服务端重新获取该槽位 ItemStack（不允许相信客户端提交的物品数据）。</li>
 * </ol>
 */
public final class SelectedItemRequestPacket {

    private final int containerId;
    private final int slotIndex;

    public SelectedItemRequestPacket(int containerId, int slotIndex) {
        this.containerId = containerId;
        this.slotIndex = slotIndex;
    }

    public static void encode(SelectedItemRequestPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.containerId);
        buf.writeVarInt(packet.slotIndex);
    }

    public static SelectedItemRequestPacket decode(FriendlyByteBuf buf) {
        return new SelectedItemRequestPacket(buf.readVarInt(), buf.readVarInt());
    }

    public int containerId() {
        return containerId;
    }

    public int slotIndex() {
        return slotIndex;
    }

    public static void handle(SelectedItemRequestPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer sender = ctx.get().getSender();
        if (sender == null) {
            return;
        }
        // 交给服务端快捷展示逻辑做完整校验
        com.heihun.Itemshowcase.forge1201.server.QuickShowHandler.handleRequest(sender, packet.containerId, packet.slotIndex);
    }
}
