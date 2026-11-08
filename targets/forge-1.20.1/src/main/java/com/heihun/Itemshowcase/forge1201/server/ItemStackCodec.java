package com.heihun.Itemshowcase.forge1201.server;

import com.heihun.Itemshowcase.core.ItemShowcaseConstants;
import com.heihun.Itemshowcase.core.protocol.ItemStackPayload;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.world.item.ItemStack;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * 1.20.1 ItemStack 序列化（NBT 方言 nbt1201）。
 *
 * <p>使用该版本原生的 save / ItemStack.of 机制，保证附魔、耐久、
 * 自定义名称、Lore、MOD 自定义 NBT 全部无损；不使用任何自造格式。</p>
 */
public final class ItemStackCodec {

    private ItemStackCodec() {
    }

    /** 真实 ItemStack → 网络载荷。空物品不允许编码。 */
    public static ItemStackPayload encode(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            throw new IllegalArgumentException("不允许编码空物品");
        }
        try {
            CompoundTag tag = stack.save(new CompoundTag());
            ByteArrayOutputStream output = new ByteArrayOutputStream(256);
            NbtIo.writeCompressed(tag, output);
            byte[] data = output.toByteArray();
            return new ItemStackPayload(
                    1,
                    ItemShowcaseConstants.DIALECT_NBT_1_20,
                    data,
                    stack.getCount(),
                    stack.getItem().builtInRegistryHolder().key().location().toString());
        } catch (IOException failure) {
            throw new IllegalStateException("1.20.1 ItemStack NBT 编码失败", failure);
        }
    }

    /** 网络载荷 → 真实 ItemStack（客户端渲染 / 服务端回退）。 */
    public static ItemStack decode(ItemStackPayload payload) {
        if (!ItemShowcaseConstants.DIALECT_NBT_1_20.equals(payload.dialect())) {
            throw new IllegalArgumentException("方言不匹配: " + payload.dialect() + " (期望 nbt1201)");
        }
        try {
            CompoundTag tag = NbtIo.readCompressed(
                    new ByteArrayInputStream(payload.data()), new NbtAccounter(2097152L));
            ItemStack stack = ItemStack.of(tag);
            if (stack.isEmpty()) {
                return ItemStack.EMPTY;
            }
            return stack.copyWithCount(payload.count());
        } catch (IOException failure) {
            throw new IllegalStateException("1.20.1 ItemStack NBT 解码失败", failure);
        }
    }
}
