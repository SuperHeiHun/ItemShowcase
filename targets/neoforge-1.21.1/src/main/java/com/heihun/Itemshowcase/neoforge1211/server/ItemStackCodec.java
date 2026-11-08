package com.heihun.Itemshowcase.neoforge1211.server;

import com.heihun.Itemshowcase.core.ItemShowcaseConstants;
import com.heihun.Itemshowcase.core.protocol.ItemStackPayload;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

/**
 * 1.21.1 ItemStack 序列化（Data Components 兼容方言 dc1211）。
 *
 * <p>使用该版本原生的 {@link ItemStack#STREAM_CODEC} 机制编码（与原版网络栈
 * 完全一致的 Data Components 表示），保证附魔、耐久、自定义名称、Lore、
 * MOD 自定义组件全部无损；不沿用旧版本 NBT 格式假设。</p>
 */
public final class ItemStackCodec {

    private ItemStackCodec() {
    }

    /** 真实 ItemStack → 网络载荷。空物品不允许编码。 */
    public static ItemStackPayload encode(ItemStack stack, HolderLookup.Provider registries) {
        if (stack == null || stack.isEmpty()) {
            throw new IllegalArgumentException("不允许编码空物品");
        }
        ByteBuf nettyBuffer = Unpooled.buffer(256);
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(new FriendlyByteBuf(nettyBuffer), (RegistryAccess) registries);
        ItemStack.STREAM_CODEC.encode(buf, stack);
        byte[] data = io.netty.buffer.ByteBufUtil.getBytes(buf);
        return new ItemStackPayload(
                1,
                ItemShowcaseConstants.DIALECT_COMPONENTS_1_21,
                data,
                stack.getCount(),
                stack.getItem().builtInRegistryHolder().key().location().toString());
    }

    /** 网络载荷 → 真实 ItemStack（客户端渲染 / 服务端回退）。 */
    public static ItemStack decode(ItemStackPayload payload, HolderLookup.Provider registries) {
        if (!ItemShowcaseConstants.DIALECT_COMPONENTS_1_21.equals(payload.dialect())) {
            throw new IllegalArgumentException("方言不匹配: " + payload.dialect() + " (期望 dc1211)");
        }
        ByteBuf nettyBuffer = Unpooled.wrappedBuffer(payload.data());
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(new FriendlyByteBuf(nettyBuffer), (RegistryAccess) registries);
        ItemStack stack = ItemStack.STREAM_CODEC.decode(buf);
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return stack.copyWithCount(payload.count());
    }
}
