package com.heihun.Itemshowcase.forge1201.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * C2S：Alt+Enter 快捷展示请求（选中槽位批量，协议 v2）。
 *
 * <p>线格式：containerId(V) + count(V) + slotIndex(V)×count。
 * 客户端只提交“打开的菜单 ID + 若干槽位 ID”，不提交任何 ItemStack 数据。
 * 服务端必须校验：菜单一致性、每个槽位属于玩家自己的库存，
 * 然后从服务端重新获取真实 ItemStack（不信任客户端提交的数据）。</p>
 */
public final class SelectedItemRequestPacket {

    /** 批量上限（防恶意大包；展示预算另有 maxItemsPerMessage 把关）。 */
    public static final int MAX_SLOTS = 512;

    private final int containerId;
    private final List<Integer> slotIndices;

    public SelectedItemRequestPacket(int containerId, List<Integer> slotIndices) {
        this.containerId = containerId;
        this.slotIndices = List.copyOf(slotIndices);
    }

    public static void encode(SelectedItemRequestPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.containerId);
        buf.writeVarInt(packet.slotIndices.size());
        for (int index : packet.slotIndices) {
            buf.writeVarInt(index);
        }
    }

    public static SelectedItemRequestPacket decode(FriendlyByteBuf buf) {
        int containerId = buf.readVarInt();
        int count = Math.min(buf.readVarInt(), MAX_SLOTS);
        List<Integer> indices = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            indices.add(buf.readVarInt());
        }
        return new SelectedItemRequestPacket(containerId, indices);
    }

    public int containerId() {
        return containerId;
    }

    public List<Integer> slotIndices() {
        return slotIndices;
    }

    public static void handle(SelectedItemRequestPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer sender = ctx.get().getSender();
        if (sender == null) {
            return;
        }
        // 交给服务端快捷展示逻辑做完整校验
        com.heihun.Itemshowcase.forge1201.server.QuickShowHandler.handleRequest(sender, packet.containerId, packet.slotIndices());
    }
}
