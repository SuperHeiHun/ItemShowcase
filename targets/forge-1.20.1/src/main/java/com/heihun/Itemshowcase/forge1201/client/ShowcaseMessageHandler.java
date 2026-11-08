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
 * <p>聊天内的两种占位 run（与 ChatIconOverlay 配合）：</p>
 * <ul>
 *   <li><b>物品占位</b>：[i] 宏等直接展示的物品，渲染真实图标 + 悬停 Tooltip。</li>
 *   <li><b>入口占位</b>：[inv]/[ec]/[c]/[pack] 等容器类宏，渲染入口图标 +
 *       “点击查看”文字，点击后打开 {@link SnapshotViewerScreen} 查看全部物品。</li>
 * </ul>
 * <p>占位 run 由“连续空格 + 编码颜色（0x010000 | id<<8 | idx）”标识，
 * 物品数据存 LRU 缓存防内存膨胀；数据淘汰时占位退化为普通空白。</p>
 */
public final class ShowcaseMessageHandler {

    /** 占位 run 的空格数：约 24px 图标区，图标在区内居中（左右各留间隙）。 */
    public static final int PLACEHOLDER_SPACES = 6;

    /** 缓存容量：聊天历史中可见的展示消息数量上限。 */
    private static final int MAX_CACHED_MESSAGES = 128;

    /** 消息 ID 编码空间：与占位颜色编码一致（低 8 位循环）。 */
    private static final int MAX_MESSAGE_ID = 256;

    /** 消息数据缓存：id → 聊天条目列表（访问顺序淘汰）。 */
    private static final Map<Integer, List<ChatEntry>> DATA =
            Collections.synchronizedMap(new LinkedHashMap<>(32, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<Integer, List<ChatEntry>> eldest) {
                    return size() > MAX_CACHED_MESSAGES;
                }
            });

    private static int nextMessageId = 0;

    private ShowcaseMessageHandler() {
    }

    /** 聊天占位对应的可渲染条目：单个物品，或一个可点击的容器入口。 */
    public static abstract class ChatEntry {
        private final SourceKind source;

        protected ChatEntry(SourceKind source) {
            this.source = source;
        }

        public SourceKind source() {
            return source;
        }
    }

    /** 单物品条目。 */
    public static final class ItemEntry extends ChatEntry {
        private final ItemStack stack;
        private final String slotInfo;

        ItemEntry(ItemStack stack, SourceKind source, String slotInfo) {
            super(source);
            this.stack = stack;
            this.slotInfo = slotInfo;
        }

        public ItemStack stack() {
            return stack;
        }

        public String slotInfo() {
            return slotInfo;
        }
    }

    /** 容器入口条目：点击打开快照查看 GUI。 */
    public static final class ContainerEntry extends ChatEntry {
        private final String title;
        private final List<DisplayItem> items;
        /** 入口图标（inv/ec 用 UI 容器图标；pack/curios 用组内第一个真实物品）。 */
        private final ItemStack iconStack;

        ContainerEntry(String title, List<DisplayItem> items, ItemStack iconStack, SourceKind source) {
            super(source);
            this.title = title;
            this.items = items;
            this.iconStack = iconStack;
        }

        public String title() {
            return title;
        }

        public List<DisplayItem> items() {
            return items;
        }

        public ItemStack iconStack() {
            return iconStack;
        }
    }

    /** 物品展示数据（GUI 与入口共用）。 */
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
        List<ChatEntry> entries = new ArrayList<>();

        MutableComponent line = Component.empty();
        line.append(Component.literal("<" + message.senderName() + "> ").withStyle(ChatFormatting.WHITE));

        int placeholderIndex = 0;
        for (ShowcaseSegment segment : message.segments()) {
            if (placeholderIndex >= config.maxRenderedItems && !(segment instanceof TextSegment)) {
                break;
            }
            if (segment instanceof TextSegment) {
                line.append(resolveText(((TextSegment) segment).payload()));
            } else if (segment instanceof ItemGroupSegment) {
                ItemGroupSegment group = (ItemGroupSegment) segment;
                boolean isEntry = config.clickViewGui && group.source() != SourceKind.HAND;
                if (group.title() != null && config.showSourceGroups && !isEntry) {
                    line.append(Component.literal(group.title().value()).withStyle(ChatFormatting.GOLD));
                }
                if (isEntry) {
                    // 容器入口：一个占位 + 可点击文字，点击打开 GUI
                    List<DisplayItem> displayItems = new ArrayList<>();
                    ItemStack firstStack = ItemStack.EMPTY;
                    for (ItemSegment item : group.items()) {
                        DisplayItem display = toDisplayItem(item);
                        displayItems.add(display);
                        if (firstStack.isEmpty()) {
                            firstStack = display.stack();
                        }
                    }
                    if (displayItems.isEmpty()) {
                        continue;
                    }
                    // 标题：解析 TRANSLATABLE（如 [c] 的组标题"饰品"），字面标题直接用
                    String title = group.title() != null
                            ? resolveText(group.title()).getString()
                            : defaultEntryTitle(group.source());
                    ContainerEntry entry = new ContainerEntry(title, displayItems,
                            entryIcon(group.source(), firstStack), group.source());
                    int count = displayItems.size();
                    line.append(Component.literal(" "));
                    line.append(placeholder(messageId, placeholderIndex));
                    line.append(Component.literal("[").withStyle(ChatFormatting.DARK_GRAY));
                    line.append(Component.literal(title).withStyle(ChatFormatting.AQUA));
                    line.append(Component.literal(" x" + count).withStyle(ChatFormatting.YELLOW));
                    line.append(Component.literal("]").withStyle(ChatFormatting.DARK_GRAY));
                    entries.add(entry);
                    placeholderIndex++;
                } else {
                    // 直接展示的物品（[i] 等）：图标 + 名称平铺
                    for (ItemSegment item : group.items()) {
                        if (placeholderIndex >= config.maxRenderedItems) {
                            break;
                        }
                        DisplayItem display = toDisplayItem(item);
                        line.append(Component.literal(" "));
                        line.append(placeholder(messageId, placeholderIndex));
                        line.append(Component.literal("[").withStyle(ChatFormatting.DARK_GRAY));
                        line.append(Component.literal(displayName(display, item)).withStyle(ChatFormatting.AQUA));
                        if (item.count() > 1) {
                            line.append(Component.literal(" x" + item.count()).withStyle(ChatFormatting.GRAY));
                        }
                        line.append(Component.literal("]").withStyle(ChatFormatting.DARK_GRAY));
                        entries.add(new ItemEntry(display.stack(), item.source(), item.slotInfo()));
                        placeholderIndex++;
                    }
                }
            } else if (segment instanceof ItemSegment) {
                ItemSegment item = (ItemSegment) segment;
                if (placeholderIndex < config.maxRenderedItems) {
                    DisplayItem display = toDisplayItem(item);
                    line.append(Component.literal(" "));
                    line.append(placeholder(messageId, placeholderIndex));
                    line.append(Component.literal("[").withStyle(ChatFormatting.DARK_GRAY));
                    line.append(Component.literal(displayName(display, item)).withStyle(ChatFormatting.AQUA));
                    if (item.count() > 1) {
                        line.append(Component.literal(" x" + item.count()).withStyle(ChatFormatting.GRAY));
                    }
                    line.append(Component.literal("]").withStyle(ChatFormatting.DARK_GRAY));
                    entries.add(new ItemEntry(display.stack(), item.source(), item.slotInfo()));
                    placeholderIndex++;
                }
            } else if (segment instanceof SectionSegment) {
                // 入口模式下分组标题并入入口显示，跳过
                if (!config.clickViewGui) {
                    line.append(Component.literal(((SectionSegment) segment).title().value())
                            .withStyle(ChatFormatting.GOLD));
                }
            }
        }

        if (!entries.isEmpty()) {
            DATA.put(messageId, entries);
        }
        minecraft.gui.getChat().addMessage(line);
    }

    /** 入口默认标题（组无标题时）。 */
    private static String defaultEntryTitle(SourceKind source) {
        Component component = Component.translatable(source.titleKey());
        return component.getString();
    }

    /** 入口图标：背包类宏用原版容器物品做 UI 按钮（仅 UI 入口，非物品展示逻辑），其余用组内第一个真实物品。 */
    private static ItemStack entryIcon(SourceKind source, ItemStack firstStack) {
        switch (source) {
            case INVENTORY:
                return new ItemStack(net.minecraft.world.item.Items.CHEST);
            case ENDER_CHEST:
                return new ItemStack(net.minecraft.world.item.Items.ENDER_CHEST);
            default:
                return firstStack;
        }
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

    /**
     * 物品显示名：优先用客户端解码后的真实 ItemStack（客户端本地语言解析，
     * 对 MOD 物品绝对正确）；解码失败时回退服务端生成的纯文本名。
     */
    private static String displayName(DisplayItem display, ItemSegment fallback) {
        ItemStack stack = display.stack();
        if (!stack.isEmpty()) {
            String clientName = stack.getHoverName().getString();
            if (clientName != null && !clientName.isEmpty()) {
                return clientName;
            }
        }
        return fallback.plainName();
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

    /** 渲染层查询条目；缓存已淘汰时返回 null（占位退化为空白）。 */
    public static ChatEntry lookup(int messageId, int itemIndex) {
        List<ChatEntry> list = DATA.get(messageId);
        if (list == null || itemIndex < 0 || itemIndex >= list.size()) {
            return null;
        }
        return list.get(itemIndex);
    }
}
