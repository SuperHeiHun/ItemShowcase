package com.heihun.Itemshowcase.forge1201.client;

import com.heihun.Itemshowcase.core.config.ItemShowcaseConfig;
import com.heihun.Itemshowcase.core.logging.ItemShowcaseLogger;
import com.heihun.Itemshowcase.forge1201.ShowcaseServices;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
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
        final ItemStack stack;

        IconRect(int x, int y, int size, ItemStack stack) {
            this.x = x;
            this.y = y;
            this.size = size;
            this.stack = stack;
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
        for (LineScan.IconRun run : scan.icons) {
            ShowcaseMessageHandler.DisplayItem item =
                    ShowcaseMessageHandler.lookup(run.messageId, run.itemIndex);
            if (item == null || item.stack().isEmpty()) {
                continue; // 缓存淘汰或解码失败：占位保留为空白
            }
            drawIcon(guiGraphics, font, item.stack(), x + run.startPx, y, config);
            int iconSize = config.iconSize;
            HOVERABLES.add(new IconRect(x + run.startPx, y - 3, iconSize, item.stack()));
        }
    }

    /** ChatScreenMixin 在 render 尾部调用：绘制原版物品 Tooltip。 */
    public static void renderHoveredTooltip(GuiGraphics guiGraphics, Font font, int mouseX, int mouseY) {
        ItemShowcaseConfig.Client config = ShowcaseServices.clientConfig();
        if (!config.chatIconRendering || !config.showTooltip) {
            return;
        }
        ItemStack hovered = findHovered(mouseX, mouseY);
        if (hovered != null && !hovered.isEmpty()) {
            // 原版物品 Tooltip（名称/耐久/附魔/Lore/组件提示全部由原版生成）
            guiGraphics.renderTooltip(font, hovered, mouseX, mouseY);
        }
    }

    private static ItemStack findHovered(int mouseX, int mouseY) {
        for (IconRect rect : HOVERABLES) {
            if (mouseX >= rect.x && mouseX < rect.x + rect.size
                    && mouseY >= rect.y && mouseY < rect.y + rect.size) {
                return rect.stack;
            }
        }
        return null;
    }

    /** 绘制单个物品图标（原版渲染管线）。 */
    private static void drawIcon(GuiGraphics guiGraphics, Font font, ItemStack stack, int x, int y,
                                 ItemShowcaseConfig.Client config) {
        String failureKey = stack.getItem().builtInRegistryHolder().key().location().toString();
        try {
            PoseStack pose = guiGraphics.pose();
            pose.pushPose();
            float scale = config.iconSize / 16.0f;
            pose.translate(x, y - 3, 0);
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

    /** 一个占位 run：屏幕 x 起点 + 消息/物品索引。 */
    static final class IconRun {
        final int startPx;
        final int messageId;
        final int itemIndex;

        IconRun(int startPx, int messageId, int itemIndex) {
            this.startPx = startPx;
            this.messageId = messageId;
            this.itemIndex = itemIndex;
        }
    }

    /**
     * 扫描 FormattedCharSequence：按“相同样式的连续字符段”重建文本，
     * 检测占位 run（全是空格、长度 >= 4、颜色为编码颜色）。
     * 段宽度用原版 font.width(String, Style) 度量，保证与文本布局一致。
     */
    private static LineScan scanLine(Font font, FormattedCharSequence line) {
        LineScan scan = new LineScan();
        List<Run> runs = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        Style currentStyle = null;

        int[] startPx = {0};
        line.accept((index, style, codePoint) -> {
            if (!style.equals(currentStyle) && current.length() > 0) {
                runs.add(new Run(current.toString(), currentStyle, startPx[0]));
                startPx[0] += font.width(current.toString(), currentStyle);
                current.setLength(0);
            }
            currentStyle = style;
            current.append(codePoint == 0 ? ' ' : Character.toChars(codePoint));
            return true;
        });
        if (current.length() > 0) {
            runs.add(new Run(current.toString(), currentStyle, startPx[0]));
        }

        for (Run run : runs) {
            if (run.text == null || run.text.isEmpty() || run.style == null) {
                continue;
            }
            int colorRgb = run.style.getColor() != null ? run.style.getColor().getValue() : -1;
            if (!ShowcaseMessageHandler.isEncodedColor(colorRgb)) {
                continue;
            }
            // 占位 run：至少 4 个空格
            if (run.text.length() < ShowcaseMessageHandler.PLACEHOLDER_SPACES) {
                continue;
            }
            boolean allSpaces = true;
            for (int i = 0; i < run.text.length(); i++) {
                if (run.text.charAt(i) != ' ') {
                    allSpaces = false;
                    break;
                }
            }
            if (!allSpaces) {
                continue;
            }
            int id = ShowcaseMessageHandler.decodeId(colorRgb);
            int index = ShowcaseMessageHandler.decodeIndex(colorRgb);
            if (ShowcaseMessageHandler.lookup(id, index) != null) {
                scan.icons.add(new IconRun(run.startPx, id, index));
            }
        }
        return scan;
    }

    private static final class Run {
        final String text;
        final Style style;
        final int startPx;

        Run(String text, Style style, int startPx) {
            this.text = text;
            this.style = style;
            this.startPx = startPx;
        }
    }
}
