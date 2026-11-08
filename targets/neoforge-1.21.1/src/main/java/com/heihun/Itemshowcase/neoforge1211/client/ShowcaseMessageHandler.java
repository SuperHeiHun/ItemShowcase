package com.heihun.Itemshowcase.neoforge1211.client;

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
import com.heihun.Itemshowcase.neoforge1211.ShowcaseServices;
import com.heihun.Itemshowcase.neoforge1211.server.ItemStackCodec;
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
import java.util.UUID;

/**
 * S2C 展示消息处理（NeoForge 1.21.1）：结构化数据 → 聊天占位组件 + 物品数据缓存。
 * 渲染模型与 Forge 1.20.1 相同（占位 run 颜色编码 + LRU 缓存 + 入口点击打开 GUI）。
 */
public final class ShowcaseMessageHandler {

    /** 占位 run 的空格数：约 24px 图标区。 */
    public static final int PLACEHOLDER_SPACES = 6;

    private static final int MAX_CACHED_MESSAGES = 128;
    private static final int MAX_MESSAGE_ID = 256;

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
        private final ItemStack iconStack;
        private final String senderName;
        private final UUID senderId;

        ContainerEntry(String title, List<DisplayItem> items, ItemStack iconStack, SourceKind source,
                       String senderName, UUID senderId) {
            super(source);
            this.title = title;
            this.items = items;
            this.iconStack = iconStack;
            this.senderName = senderName;
            this.senderId = senderId;
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

        public String senderName() {
            return senderName;
        }

        public UUID senderId() {
            return senderId;
        }
    }

    /** 物品展示数据（GUI 与入口共用）。 */
    public static final class DisplayItem {
        private final ItemStack stack;
        private final SourceKind source;
        private final String slotInfo;
        private final int slotIndex;

        DisplayItem(ItemStack stack, SourceKind source, String slotInfo, int slotIndex) {
            this.stack = stack;
            this.source = source;
            this.slotInfo = slotInfo;
            this.slotIndex = slotIndex;
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

        public int slotIndex() {
            return slotIndex;
        }
    }

    /** 客户端注册表访问（1.21.1 物品解码需要；收包时必然在游戏内）。 */
    static net.minecraft.core.HolderLookup.Provider clientRegistries() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null) {
            return minecraft.level.registryAccess();
        }
        return net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer().registryAccess();
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
                boolean isEntry = config.clickViewGui
                        && (group.source() != SourceKind.HAND || group.title() != null);
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
                    // 标题解析 TRANSLATABLE（如 [c] 的“饰品”）
                    String title = group.title() != null
                            ? resolveText(group.title()).getString()
                            : defaultEntryTitle(group.source());
                    ContainerEntry entry = new ContainerEntry(title, displayItems,
                            entryIcon(group.source(), firstStack), group.source(),
                            message.senderName(), message.sender());
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

    private static String defaultEntryTitle(SourceKind source) {
        return Component.translatable(source.titleKey()).getString();
    }

    /** 入口图标：背包类宏用原版容器物品做 UI 按钮，其余用组内第一个真实物品。 */
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
            stack = ItemStackCodec.decode(item.item(), clientRegistries());
        } catch (RuntimeException decodeFailure) {
            ItemShowcaseLogger.warn("物品数据解码失败: " + item.item().itemIdHint(), decodeFailure);
        }
        return new DisplayItem(stack, item.source(), item.slotInfo(), item.slotIndex());
    }

    /** 物品显示名：优先客户端解码的 ItemStack（本地语言），失败回退服务端 plainName。 */
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

    /** 颜色编码：0x010000 | id<<8 | index。 */
    public static int encodeColor(int messageId, int itemIndex) {
        return 0x010000 | ((messageId & 0xFF) << 8) | (itemIndex & 0xFF);
    }

    public static boolean isEncodedColor(int rgb) {
        return (rgb & 0xFF0000) == 0x010000;
    }

    public static int decodeId(int rgb) {
        return (rgb >> 8) & 0xFF;
    }

    public static int decodeIndex(int rgb) {
        return rgb & 0xFF;
    }

    public static ChatEntry lookup(int messageId, int itemIndex) {
        List<ChatEntry> list = DATA.get(messageId);
        if (list == null || itemIndex < 0 || itemIndex >= list.size()) {
            return null;
        }
        return list.get(itemIndex);
    }
}
