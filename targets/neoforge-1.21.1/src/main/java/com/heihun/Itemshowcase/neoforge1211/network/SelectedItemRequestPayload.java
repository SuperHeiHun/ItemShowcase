package com.heihun.Itemshowcase.neoforge1211.network;

import com.heihun.Itemshowcase.neoforge1211.ItemShowcaseNeoForge;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

/**
 * C2S：Alt+Enter 快捷展示请求（选中槽位批量，协议 v2）。
 *
 * <p>线格式：containerId(V) + count(V) + slotIndex(V)×count。
 * 客户端只提交“打开的菜单 ID + 若干槽位 ID”，不提交任何 ItemStack 数据。
 * 服务端必须校验：菜单一致性、每个槽位属于玩家自己的库存，
 * 然后从服务端重新获取真实 ItemStack（不信任客户端提交的数据）。</p>
 */
public final class SelectedItemRequestPayload implements CustomPacketPayload {

    public static final Type<SelectedItemRequestPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ItemShowcaseNeoForge.MOD_ID, "selected_item"));

    public static final StreamCodec<FriendlyByteBuf, SelectedItemRequestPayload> STREAM_CODEC =
            StreamCodec.ofMember(SelectedItemRequestPayload::write, SelectedItemRequestPayload::read);

    /** 批量上限（防恶意大包；展示预算另有 maxItemsPerMessage 把关）。 */
    public static final int MAX_SLOTS = 512;

    public final int containerId;
    public final List<Integer> slotIndices;

    public SelectedItemRequestPayload(int containerId, List<Integer> slotIndices) {
        this.containerId = containerId;
        this.slotIndices = List.copyOf(slotIndices);
    }

    private static void write(SelectedItemRequestPayload payload, FriendlyByteBuf buf) {
        buf.writeVarInt(payload.containerId);
        buf.writeVarInt(payload.slotIndices.size());
        for (int index : payload.slotIndices) {
            buf.writeVarInt(index);
        }
    }

    private static SelectedItemRequestPayload read(FriendlyByteBuf buf) {
        int containerId = buf.readVarInt();
        int count = Math.min(buf.readVarInt(), MAX_SLOTS);
        List<Integer> indices = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            indices.add(buf.readVarInt());
        }
        return new SelectedItemRequestPayload(containerId, indices);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SelectedItemRequestPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof net.minecraft.server.level.ServerPlayer player) {
                com.heihun.Itemshowcase.neoforge1211.server.QuickShowHandler.handleRequest(
                        player, payload.containerId, payload.slotIndices);
            }
        });
    }
}
