package com.heihun.Itemshowcase.forge1201.client;

import com.heihun.Itemshowcase.core.config.ItemShowcaseConfig;
import com.heihun.Itemshowcase.core.logging.ItemShowcaseLogger;
import com.heihun.Itemshowcase.forge1201.ShowcaseServices;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 聊天图标渲染层：扫描聊天行中的占位 run，用原版 ItemRenderer 绘制真实物品图标。
 *
 * <p>职责：</p>
 * <ul>
 *   <li>每帧开始时清理悬停命中区域。</li>
 *   <li>每行 drawString 后扫描占位 run（连续空格 + 编码颜色），绘制图标
 *       （原版 renderItem：真实模型、附魔光效、自定义模型、BEWLR 全部生效），
 *       并叠加数量与耐久度装饰（renderItemDecorations）。</li>
 *   <li>登记悬停区域，供 ChatScreenMixin 绘制原版物品 Tooltip。</li>
 *   <li>任何单个物品渲染失败只影响该物品（记录日志并跳过），不破坏聊天。</li>
 * </ul>
 */
public final class ChatIconOverlay {

    /** 当前帧的悬停命中区域。 */
    private static final List<IconRect> HOVERABLES = new ArrayList<>();

    private static ItemStack hoveredStack;

    /** 渲染失败去重集合：同一物品只记录一次警告。 */
    private static final List<String> REPORTED_FAILURES = new ArrayList<>();

    private ChatIconOverlay() {
    }

    /** 命中区域。 */
    static final class IconRect {
        final int x;
        final int y;
        final int size;
        final ShowcaseMessageHandler.ChatEntry entry;

        IconRect(int x, int y, int size, ShowcaseMessageHandler.ChatEntry entry) {
            this.x = x;
            this.y = y;
            this.size = size;
            this.entry = entry;
        }
    }

    /** ChatComponentMixin 在 render 头部调用：重置本帧状态。 */
    public static void beginFrame(int mouseX, int mouseY) {
        HOVERABLES.clear();
        hoveredStack = null;
        // 鼠标位置在帧内持续有效（ChatScreenMixin 用它判断悬停）
        mousePosX = mouseX;
        mousePosY = mouseY;
    }

    private static int mousePosX;
    private static int mousePosY;

    /** ChatComponentMixin 在每行 drawString 后调用。 */
    public static void renderLine(GuiGraphics guiGraphics, Font font, FormattedCharSequence line, int x, int y) {
        ItemShowcaseConfig.Client config = ShowcaseServices.clientConfig();
        if (!config.chatIconRendering) {
            return;
        }
        LineScan scan = scanLine(font, line);
        if (scan.icons.isEmpty()) {
            return;
        }
        for (IconRun run : scan.icons) {
            ShowcaseMessageHandler.ChatEntry entry =
                    ShowcaseMessageHandler.lookup(run.messageId, run.itemIndex);
            if (entry == null) {
                continue; // 缓存淘汰或解码失败：占位保留为空白
            }
            int iconSize = config.iconSize;
            // 图标在占位区内水平居中；垂直相对文字行居中（floorDiv 保证上下间隙对称）
            int iconX = x + run.startPx + Math.max(0, (run.runWidth - iconSize) / 2);
            int iconY = y + net.minecraft.util.Mth.floorDiv(
                    fontLineHeight() - iconSize, 2);
            if (entry instanceof ShowcaseMessageHandler.ContainerEntry) {
                ShowcaseMessageHandler.ContainerEntry container = (ShowcaseMessageHandler.ContainerEntry) entry;
                drawIcon(guiGraphics, font, container.iconStack(), iconX, iconY, config);
            } else if (entry instanceof ShowcaseMessageHandler.ItemEntry) {
                ShowcaseMessageHandler.ItemEntry item = (ShowcaseMessageHandler.ItemEntry) entry;
                drawIcon(guiGraphics, font, item.stack(), iconX, iconY, config);
            }
            HOVERABLES.add(new IconRect(iconX, iconY, iconSize, entry));
        }
    }

    /** ChatScreenMixin 在 render 尾部调用：绘制原版物品 Tooltip / 入口提示。 */
    public static void renderHoveredTooltip(GuiGraphics guiGraphics, Font font, int mouseX, int mouseY) {
        ItemShowcaseConfig.Client config = ShowcaseServices.clientConfig();
        if (!config.chatIconRendering || !config.showTooltip) {
            return;
        }
        IconRect hovered = findHovered(mouseX, mouseY);
        if (hovered == null) {
            return;
        }
        if (hovered.entry instanceof ShowcaseMessageHandler.ItemEntry) {
            ItemStack stack = ((ShowcaseMessageHandler.ItemEntry) hovered.entry).stack();
            if (!stack.isEmpty()) {
                // 原版物品 Tooltip（名称/耐久/附魔/Lore/组件提示全部由原版生成）
                guiGraphics.renderTooltip(font, stack, mouseX, mouseY);
            }
        } else if (hovered.entry instanceof ShowcaseMessageHandler.ContainerEntry) {
            ShowcaseMessageHandler.ContainerEntry container = (ShowcaseMessageHandler.ContainerEntry) hovered.entry;
            guiGraphics.renderComponentTooltip(font, java.util.List.of(
                    Component.literal(container.title()).withStyle(ChatFormatting.AQUA),
                    Component.translatable("itemshowcase.chat.click_to_view")
                            .withStyle(ChatFormatting.GRAY)), mouseX, mouseY);
        }
    }

    /** ChatScreenMixin 在 mouseClicked 时调用：命中可点击入口则返回该条目。 */
    public static ShowcaseMessageHandler.ChatEntry findClicked(int mouseX, int mouseY) {
        IconRect hovered = findHovered(mouseX, mouseY);
        if (hovered != null && hovered.entry instanceof ShowcaseMessageHandler.ContainerEntry) {
            return hovered.entry;
        }
        return null;
    }

    private static IconRect findHovered(int mouseX, int mouseY) {
        for (IconRect rect : HOVERABLES) {
            if (mouseX >= rect.x && mouseX < rect.x + rect.size
                    && mouseY >= rect.y && mouseY < rect.y + rect.size) {
                return rect;
            }
        }
        return null;
    }

    /** 文字行高（9px），图标垂直居中的基准。 */
    private static int fontLineHeight() {
        return net.minecraft.client.Minecraft.getInstance().font.lineHeight;
    }

    /** 绘制单个物品图标（原版渲染管线）。 */
    private static void drawIcon(GuiGraphics guiGraphics, Font font, ItemStack stack, int x, int y,
                                 ItemShowcaseConfig.Client config) {
        String failureKey = stack.getItem().builtInRegistryHolder().key().location().toString();
        try {
            PoseStack pose = guiGraphics.pose();
            pose.pushPose();
            float scale = config.iconSize / 16.0f;
            pose.translate(x, y, 0);
            if (scale != 1.0f) {
                // 以左上角为原点缩放
                pose.scale(scale, scale, 1.0f);
            }
            // 原版 ItemRenderer：真实模型 + 附魔光效 + 自定义渲染
            guiGraphics.renderItem(stack, 0, 0);
            // 数量覆盖 / 耐久条（原版装饰层）
            guiGraphics.renderItemDecorations(font, stack, 0, 0, null);
            pose.popPose();
        } catch (Throwable renderFailure) {
            // 渲染失败的物品：跳过绘制并记录（去重），不得破坏整条聊天
            if (!REPORTED_FAILURES.contains(failureKey)) {
                REPORTED_FAILURES.add(failureKey);
                if (REPORTED_FAILURES.size() > 64) {
                    REPORTED_FAILURES.clear();
                }
                ItemShowcaseLogger.warn("物品图标渲染失败, 已跳过: " + failureKey, renderFailure);
            }
        }
    }

    // ---------------- 行扫描 ----------------

    /** 扫描结果。 */
    static final class LineScan {
        final List<IconRun> icons = new ArrayList<>();
    }

    /** 一个占位 run：屏幕 x 起点、占位区宽度、消息/物品索引。 */
    static final class IconRun {
        final int startPx;
        final int runWidth;
        final int messageId;
        final int itemIndex;

        IconRun(int startPx, int runWidth, int messageId, int itemIndex) {
            this.startPx = startPx;
            this.runWidth = runWidth;
            this.messageId = messageId;
            this.itemIndex = itemIndex;
        }
    }

    /**
     * 扫描 FormattedCharSequence：按“相同样式的连续字符段”重建文本，
     * 检测占位 run（全是空格、长度 >= 4、颜色为编码颜色）。
     *
     * <p>重要：不使用 FormattedCharSink 的 index 参数（换行后的行序列中
     * 该索引不保证是行内序号），而是自计行内序号；段屏幕起点用
     * StringSplitter 对“行内前缀子序列”的度量计算，与聊天布局一致。</p>
     */
    private static LineScan scanLine(Font font, FormattedCharSequence line) {
        LineScan scan = new LineScan();
        List<IndexedChar> chars = new ArrayList<>();
        int[] seq = {0};
        line.accept((index, style, codePoint) -> {
            chars.add(new IndexedChar(seq[0]++, style, codePoint == 0 ? ' ' : codePoint));
            return true;
        });
        if (chars.isEmpty()) {
            return scan;
        }
        net.minecraft.client.StringSplitter splitter = font.getSplitter();

        StringBuilder current = new StringBuilder();
        Style currentStyle = null;
        int runStartIndex = 0;

        for (int i = 0; i <= chars.size(); i++) {
            IndexedChar ch = i < chars.size() ? chars.get(i) : null;
            boolean styleChanged = ch == null
                    || !java.util.Objects.equals(ch.style(), currentStyle);
            if (styleChanged && current.length() > 0) {
                flushRun(scan, splitter, line, current, currentStyle, runStartIndex);
                current.setLength(0);
                runStartIndex = ch == null ? i : ch.index();
            }
            if (ch != null) {
                currentStyle = ch.style();
                current.appendCodePoint(ch.codePoint());
            }
        }
        return scan;
    }

    /** 检查一个文本段是否为物品占位 run，是则登记图标。 */
    private static void flushRun(LineScan scan, net.minecraft.client.StringSplitter splitter,
                                 FormattedCharSequence line, StringBuilder text, Style style, int startIndex) {
        if (style == null || text.length() < ShowcaseMessageHandler.PLACEHOLDER_SPACES) {
            return;
        }
        int colorRgb = style.getColor() != null ? style.getColor().getValue() : -1;
        if (!ShowcaseMessageHandler.isEncodedColor(colorRgb)) {
            return;
        }
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) != ' ') {
                return;
            }
        }
        int id = ShowcaseMessageHandler.decodeId(colorRgb);
        int index = ShowcaseMessageHandler.decodeIndex(colorRgb);
        if (ShowcaseMessageHandler.lookup(id, index) == null) {
            return; // 缓存已淘汰
        }
        // 前缀像素宽度：按行内序号度量 [0, startIndex) 的子序列。
        // 二次遍历用独立计数器截取前缀，保证与首次收集时序一致。
        FormattedCharSequence prefix = consumer -> {
            int[] count = {0};
            return line.accept((idx, charStyle, codePoint) -> {
                int position = count[0]++;
                if (position < startIndex) {
                    return consumer.accept(position, charStyle, codePoint);
                }
                return true;
            });
        };
        int startPx = net.minecraft.util.Mth.floor(splitter.stringWidth(prefix));
        if (startPx < 0) {
            startPx = 0;
        }
        // 占位区实际像素宽度（含换行器对空格的实际排版），供图标居中使用
        int prefixPlusRun = startIndex + text.length();
        FormattedCharSequence prefixAndRun = consumer -> {
            int[] count = {0};
            return line.accept((idx, charStyle, codePoint) -> {
                int position = count[0]++;
                if (position < prefixPlusRun) {
                    return consumer.accept(position, charStyle, codePoint);
                }
                return true;
            });
        };
        int runWidth = net.minecraft.util.Mth.floor(splitter.stringWidth(prefixAndRun)) - startPx;
        if (runWidth < 1) {
            runWidth = 1;
        }
        scan.icons.add(new IconRun(startPx, runWidth, id, index));
    }

    /** 行内字符（含样式与行内索引）。 */
    private static final class IndexedChar {
        final int index;
        final Style style;
        final int codePoint;

        IndexedChar(int index, Style style, int codePoint) {
            this.index = index;
            this.style = style;
            this.codePoint = codePoint;
        }

        /** 行内索引。 */
        int index() {
            return index;
        }

        Style style() {
            return style;
        }

        int codePoint() {
            return codePoint;
        }
    }
}
