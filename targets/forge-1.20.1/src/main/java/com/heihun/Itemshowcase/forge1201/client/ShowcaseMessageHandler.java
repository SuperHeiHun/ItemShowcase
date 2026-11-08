package com.heihun.Itemshowcase.forge1201.client;

import com.heihun.Itemshowcase.core.config.ItemShowcaseConfig;
import com.heihun.Itemshowcase.core.logging.ItemShowcaseLogger;
import com.heihun.Itemshowcase.core.model.ItemGroupSegment;
import com.heihun.Itemshowcase.core.model.ItemSegment;
import com.heihun.Itemshowcase.core.model.SectionSegment;
import com.heihun.Itemshowcase.core.model.ShowcaseMessage;
import com.heihun.Itemshowcase.core.model.ShowcaseSegment;
import com.heihun.Itemshowcase.core.model.SourceKind;
import com.heihun.Itemshowcase.core.model.TextSegment;
import com.heihun.Itemshowcase.core.protocol.ItemStackPayload;
import com.heihun.Itemshowcase.core.text.TextPayload;
import com.heihun.Itemshowcase.forge1201.ShowcaseServices;
import com.heihun.Itemshowcase.forge1201.server.ItemStackCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * S2C 展示消息处理：结构化数据 → 聊天占位组件 + 物品数据缓存。
 *
 * <p>渲染模型（与 ChatIconOverlay 配合）：</p>
 * <ul>
 *   <li>每个物品在聊天文本中占一个“占位 run”：4 个空格（约 16px），
 *       其文本颜色编码了 (消息ID, 物品索引)——空格无字形，视觉上不可见。</li>
 *   <li>物品数据解码后的真实 ItemStack 存入 {@link #DATA}（LRU，防内存膨胀）。</li>
 *   <li>聊天渲染 Mixin 扫描每行文本，命中占位 run 后从缓存取出真实
 *       ItemStack 并用原版 ItemRenderer 绘制图标。</li>
 *   <li>数据被 LRU 淘汰时占位退化为普通空白，不影响聊天可读性。</li>
 * </ul>
 */
public final class ShowcaseMessageHandler {

    /** 占位 run 的空格数：4 空格 × 4px = 16px 图标区。 */
    public static final int PLACEHOLDER_SPACES = 4;

    /** 缓存容量：聊天历史中可见的展示消息数量上限。 */
    private static final int MAX_CACHED_MESSAGES = 128;

    /** 消息 ID 编码空间：与占位颜色编码一致（低 8 位循环）。 */
    private static final int MAX_MESSAGE_ID = 256;

    /** 消息数据缓存：id → 物品列表（访问顺序淘汰）。 */
    private static final Map<Integer, List<DisplayItem>> DATA =
            Collections.synchronizedMap(new LinkedHashMap<>(32, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<Integer, List<DisplayItem>> eldest) {
                    return size() > MAX_CACHED_MESSAGES;
                }
            });

    private static int nextMessageId = 0;

    private ShowcaseMessageHandler() {
    }

    /** 单个物品的客户端展示数据。 */
    public static final class DisplayItem {
        private final ItemStack stack;
        private final SourceKind source;
        private final String slotInfo;

        DisplayItem(ItemStack stack, SourceKind source, String slotInfo) {
            this.stack = stack;
            this.source = source;
            this.slotInfo = slotInfo;
        }

        public ItemStack stack() {
            return stack;
        }

        public SourceKind source() {
            return source;
        }

        public String slotInfo() {
            return slotInfo;
        }
    }

    /** S2C 主线程入口。 */
    public static void handleShowcaseMessage(byte[] payload) {
        ShowcaseMessage message;
        try {
            message = ShowcaseMessage.decodeFromBytes(payload);
        } catch (RuntimeException malformed) {
            ItemShowcaseLogger.warn("无法解析展示消息, 已丢弃", malformed);
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.gui == null || minecraft.gui.getChat() == null) {
            return;
        }
        ItemShowcaseConfig.Client config = ShowcaseServices.clientConfig();
        if (!config.chatIconRendering) {
            // 用户关闭了聊天图标渲染：退化为纯文本（物品名列表）
            minecraft.gui.getChat().addMessage(Component.literal(message.toPlainText(key -> key)));
            return;
        }

        int messageId = nextMessageId++ % MAX_MESSAGE_ID;
        List<DisplayItem> items = new ArrayList<>();

        MutableComponent line = Component.empty();
        line.append(Component.literal("<" + message.senderName() + "> ").withStyle(ChatFormatting.WHITE));

        int itemIndex = 0;
        for (ShowcaseSegment segment : message.segments()) {
            if (itemIndex >= config.maxRenderedItems && !(segment instanceof TextSegment)) {
                break;
            }
            if (segment instanceof TextSegment) {
                line.append(resolveText(((TextSegment) segment).payload()));
            } else if (segment instanceof ItemGroupSegment) {
                ItemGroupSegment group = (ItemGroupSegment) segment;
                if (group.title() != null && config.showSourceGroups) {
                    line.append(Component.literal(group.title().value()).withStyle(ChatFormatting.GOLD));
                }
                for (ItemSegment item : group.items()) {
                    if (itemIndex >= config.maxRenderedItems) {
                        break;
                    }
                    line.append(Component.literal(" "));
                    line.append(placeholder(messageId, itemIndex));
                    line.append(Component.literal("[").withStyle(ChatFormatting.DARK_GRAY));
                    line.append(Component.literal(item.plainName()).withStyle(ChatFormatting.AQUA));
                    if (item.count() > 1) {
                        line.append(Component.literal(" x" + item.count()).withStyle(ChatFormatting.GRAY));
                    }
                    line.append(Component.literal("]").withStyle(ChatFormatting.DARK_GRAY));
                    items.add(toDisplayItem(item));
                    itemIndex++;
                }
            } else if (segment instanceof ItemSegment) {
                ItemSegment item = (ItemSegment) segment;
                if (itemIndex < config.maxRenderedItems) {
                    line.append(Component.literal(" "));
                    line.append(placeholder(messageId, itemIndex));
                    items.add(toDisplayItem(item));
                    itemIndex++;
                }
            } else if (segment instanceof SectionSegment) {
                line.append(Component.literal(((SectionSegment) segment).title().value())
                        .withStyle(ChatFormatting.GOLD));
            }
        }

        if (!items.isEmpty()) {
            DATA.put(messageId, items);
        }
        minecraft.gui.getChat().addMessage(line);
    }

    private static DisplayItem toDisplayItem(ItemSegment item) {
        ItemStack stack = ItemStack.EMPTY;
        try {
            stack = ItemStackCodec.decode(item.item());
        } catch (RuntimeException decodeFailure) {
            ItemShowcaseLogger.warn("物品数据解码失败: " + item.item().itemIdHint(), decodeFailure);
        }
        return new DisplayItem(stack, item.source(), item.slotInfo());
    }

    /** 文本片段 → 客户端组件（翻译键由客户端本地语言解析）。 */
    private static MutableComponent resolveText(TextPayload payload) {
        switch (payload.kind()) {
            case LITERAL:
                return Component.literal(payload.value());
            case TRANSLATABLE:
                return Component.translatable(payload.value());
            case JSON:
            default:
                return Component.literal(payload.value());
        }
    }

    // ---------------- 占位 run 编码协议 ----------------

    private static MutableComponent placeholder(int messageId, int itemIndex) {
        Style style = Style.EMPTY.withColor(TextColor.fromRgb(encodeColor(messageId, itemIndex)));
        return Component.literal(" ".repeat(PLACEHOLDER_SPACES)).withStyle(style);
    }

    /** 颜色编码：0x010000 | id<<8 | index。空格无字形，颜色不影响视觉。 */
    public static int encodeColor(int messageId, int itemIndex) {
        return 0x010000 | ((messageId & 0xFF) << 8) | (itemIndex & 0xFF);
    }

    /** 判断一个 RGB 颜色是否为占位编码。 */
    public static boolean isEncodedColor(int rgb) {
        return (rgb & 0xFF0000) == 0x010000;
    }

    public static int decodeId(int rgb) {
        return (rgb >> 8) & 0xFF;
    }

    public static int decodeIndex(int rgb) {
        return rgb & 0xFF;
    }

    /** 渲染层查询物品数据；缓存已淘汰时返回 null（占位退化为空白）。 */
    public static DisplayItem lookup(int messageId, int itemIndex) {
        List<DisplayItem> list = DATA.get(messageId);
        if (list == null || itemIndex < 0 || itemIndex >= list.size()) {
            return null;
        }
        return list.get(itemIndex);
    }
}
