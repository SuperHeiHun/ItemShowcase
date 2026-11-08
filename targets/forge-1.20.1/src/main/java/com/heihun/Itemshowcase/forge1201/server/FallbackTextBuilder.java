package com.heihun.Itemshowcase.forge1201.server;

import com.heihun.Itemshowcase.core.model.ItemGroupSegment;
import com.heihun.Itemshowcase.core.model.ItemSegment;
import com.heihun.Itemshowcase.core.model.SectionSegment;
import com.heihun.Itemshowcase.core.model.ShowcaseMessage;
import com.heihun.Itemshowcase.core.model.ShowcaseSegment;
import com.heihun.Itemshowcase.core.model.TextSegment;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;

/**
 * 原版客户端回退消息构建器。
 *
 * <p>把结构化 ShowcaseMessage 转为可读聊天组件：</p>
 * <ul>
 *   <li>文字段原样输出。</li>
 *   <li>物品以“[物品名 x数量]”呈现，附加原版 show_item HoverEvent
 *       ——原版客户端悬停即可看到物品 Tooltip。</li>
 *   <li>分组标题（饰品槽位类型等）以金色小标题输出。</li>
 * </ul>
 */
public final class FallbackTextBuilder {

    private FallbackTextBuilder() {
    }

    public static MutableComponent build(ShowcaseMessage message) {
        MutableComponent root = Component.empty();
        boolean first = true;
        for (ShowcaseSegment segment : message.segments()) {
            if (segment instanceof TextSegment) {
                root.append(resolve(((TextSegment) segment).payload()));
            } else if (segment instanceof ItemSegment) {
                root.append(spaceIfNeeded(first)).append(itemComponent((ItemSegment) segment));
                first = false;
            } else if (segment instanceof ItemGroupSegment) {
                ItemGroupSegment group = (ItemGroupSegment) segment;
                if (group.title() != null) {
                    root.append(spaceIfNeeded(first)).append(Component.literal(group.title().value())
                            .withStyle(ChatFormatting.GOLD));
                    first = false;
                }
                for (ItemSegment item : group.items()) {
                    root.append(spaceIfNeeded(first)).append(itemComponent(item));
                    first = false;
                }
            } else if (segment instanceof SectionSegment) {
                root.append(spaceIfNeeded(first)).append(Component.literal(
                                ((SectionSegment) segment).title().value())
                        .withStyle(ChatFormatting.GOLD));
                first = false;
            }
        }
        return root;
    }

    private static MutableComponent spaceIfNeeded(boolean first) {
        return first ? Component.empty() : Component.literal(" ");
    }

    private static MutableComponent itemComponent(ItemSegment item) {
        String name = item.plainName();
        String text = "[" + name + (item.count() > 1 ? " x" + item.count() : "") + "]";
        MutableComponent component = Component.literal(text).withStyle(ChatFormatting.AQUA);
        try {
            ItemStack stack = ItemStackCodec.decode(item.item());
            if (!stack.isEmpty()) {
                component.withStyle(style -> style.withHoverEvent(
                        new HoverEvent(HoverEvent.Action.SHOW_ITEM, new HoverEvent.ItemInfo(stack))));
            }
        } catch (RuntimeException decodeFailure) {
            // 解码失败时保留纯文本名称，消息不至于不可读
        }
        return component;
    }

    /** TextPayload → 服务端组件（用于回退文本中的翻译键解析）。 */
    private static Component resolve(com.heihun.Itemshowcase.core.text.TextPayload payload) {
        switch (payload.kind()) {
            case LITERAL:
                return Component.literal(payload.value());
            case TRANSLATABLE:
                return Component.translatable(payload.value());
            case JSON:
            default:
                // 回退文本不解析组件 JSON（由结构化通道处理），按纯文本降级
                return Component.literal(payload.value());
        }
    }
}
