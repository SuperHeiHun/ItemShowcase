package com.heihun.Itemshowcase.neoforge1211.network;

import com.heihun.Itemshowcase.neoforge1211.ItemShowcaseNeoForge;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * C2S：Ctrl+Enter 快捷展示请求。
 *
 * <p>客户端只提交“打开的菜单 ID + 槽位 ID”，不提交任何 ItemStack 数据。
 * 服务端必须校验：菜单一致性、槽位属于玩家自己的库存，
 * 然后从服务端重新获取该槽位 ItemStack（不信任客户端提交的数据）。</p>
 */
public final class SelectedItemRequestPayload implements CustomPacketPayload {

    public static final Type<SelectedItemRequestPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ItemShowcaseNeoForge.MOD_ID, "selected_item"));

    public static final StreamCodec<FriendlyByteBuf, SelectedItemRequestPayload> STREAM_CODEC =
            StreamCodec.ofMember(SelectedItemRequestPayload::write, SelectedItemRequestPayload::read);

    public final int containerId;
    public final int slotIndex;

    public SelectedItemRequestPayload(int containerId, int slotIndex) {
        this.containerId = containerId;
        this.slotIndex = slotIndex;
    }

    private static void write(SelectedItemRequestPayload payload, FriendlyByteBuf buf) {
        buf.writeVarInt(payload.containerId);
        buf.writeVarInt(payload.slotIndex);
    }

    private static SelectedItemRequestPayload read(FriendlyByteBuf buf) {
        return new SelectedItemRequestPayload(buf.readVarInt(), buf.readVarInt());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SelectedItemRequestPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof net.minecraft.server.level.ServerPlayer player) {
                com.heihun.Itemshowcase.neoforge1211.server.QuickShowHandler.handleRequest(
                        player, payload.containerId, payload.slotIndex);
            }
        });
    }
}
