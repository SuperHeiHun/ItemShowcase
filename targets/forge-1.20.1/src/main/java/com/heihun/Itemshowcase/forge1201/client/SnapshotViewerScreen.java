package com.heihun.Itemshowcase.forge1201.client;

import com.heihun.Itemshowcase.forge1201.ShowcaseServices;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * 快照查看 GUI：以物品网格展示一次宏的完整内容（真实图标 + 原版 Tooltip）。
 *
 * <p>设计要点：</p>
 * <ul>
 *   <li>纯展示界面：没有菜单（Menu）与任何槽位交互，物品不可拿取/放入——
 *       展示功能不涉及物品移动、复制或库存修改。</li>
 *   <li>图标用原版 {@code renderItem} 管线（数量/耐久/附魔光效/自定义模型全部生效），
 *       Tooltip 用原版 {@code renderTooltip}。</li>
 *   <li>支持鼠标滚轮滚动查看超出一屏的物品；E / ESC 关闭。</li>
 * </ul>
 */
public final class SnapshotViewerScreen extends Screen {

    private static final int COLUMNS = 9;
    private static final int SLOT_SIZE = 18;

    private final List<ShowcaseMessageHandler.DisplayItem> items;
    private final String entryTitle;

    private int gridLeft;
    private int gridTop;
    private int visibleRows;
    private int scrollY;
    private int maxScroll;

    public SnapshotViewerScreen(String entryTitle, List<ShowcaseMessageHandler.DisplayItem> items) {
        super(Component.translatable("itemshowcase.gui.snapshot_title",
                entryTitle, items.size()));
        this.entryTitle = entryTitle;
        this.items = items;
    }

    @Override
    protected void init() {
        int gridWidth = COLUMNS * SLOT_SIZE;
        this.gridLeft = (this.width - gridWidth) / 2;
        this.gridTop = 40;
        int availableHeight = this.height - this.gridTop - 40;
        this.visibleRows = Math.max(1, availableHeight / SLOT_SIZE);
        int totalRows = (this.items.size() + COLUMNS - 1) / COLUMNS;
        this.maxScroll = Math.max(0, totalRows - this.visibleRows) * SLOT_SIZE;
        this.scrollY = 0;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics);

        int gridWidth = COLUMNS * SLOT_SIZE;
        int gridHeight = this.visibleRows * SLOT_SIZE;
        int panelLeft = this.gridLeft - 7;
        int panelTop = this.gridTop - 7;
        int panelRight = this.gridLeft + gridWidth + 7;
        int panelBottom = this.gridTop + gridHeight + 7;

        // 原版容器面板：灰色主体 + 左上亮 / 右下暗 的立体边框
        guiGraphics.fill(panelLeft, panelTop, panelRight, panelBottom, 0xFFC6C6C6);
        guiGraphics.fill(panelLeft, panelTop, panelRight, panelTop + 1, 0xFFFFFFFF);
        guiGraphics.fill(panelLeft, panelTop, panelLeft + 1, panelBottom, 0xFFFFFFFF);
        guiGraphics.fill(panelLeft, panelBottom - 1, panelRight, panelBottom, 0xFF555555);
        guiGraphics.fill(panelRight - 1, panelTop, panelRight, panelBottom, 0xFF555555);

        // 原版风格标题（深灰黑文字）
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, panelTop - 16, 0xFF404040);

        // 网格（裁剪 + 滚动）
        guiGraphics.enableScissor(this.gridLeft, this.gridTop,
                this.gridLeft + gridWidth, this.gridTop + gridHeight);

        int hoveredIndex = -1;
        int hoveredX = -1;
        int hoveredY = -1;
        for (int i = 0; i < this.items.size(); i++) {
            int col = i % COLUMNS;
            int row = i / COLUMNS;
            int x = this.gridLeft + col * SLOT_SIZE;
            int y = this.gridTop + row * SLOT_SIZE - this.scrollY;
            if (y + SLOT_SIZE < this.gridTop || y > this.gridTop + gridHeight) {
                continue;
            }
            drawVanillaSlot(guiGraphics, x, y);
            ItemStack stack = this.items.get(i).stack();
            if (!stack.isEmpty()) {
                guiGraphics.renderItem(stack, x + 1, y + 1);
                guiGraphics.renderItemDecorations(this.font, stack, x + 1, y + 1, null);
            }
            if (mouseX >= x && mouseX < x + SLOT_SIZE && mouseY >= y && mouseY < y + SLOT_SIZE) {
                hoveredIndex = i;
                hoveredX = x;
                hoveredY = y;
            }
        }
        guiGraphics.disableScissor();

        if (hoveredIndex >= 0) {
            // 原版悬停高亮（半透明白覆盖整个槽位）
            guiGraphics.fill(hoveredX, hoveredY, hoveredX + SLOT_SIZE, hoveredY + SLOT_SIZE, 0x80FFFFFF);
            ItemStack stack = this.items.get(hoveredIndex).stack();
            if (!stack.isEmpty() && ShowcaseServices.clientConfig().showTooltip) {
                guiGraphics.renderTooltip(this.font, stack, mouseX, mouseY);
            }
        }

        // 滚动条（原版配色）
        if (this.maxScroll > 0) {
            int trackX = panelRight - 4;
            int trackHeight = gridHeight + 14;
            guiGraphics.fill(trackX, panelTop, trackX + 3, panelTop + trackHeight, 0xFF8B8B8B);
            int thumbHeight = Math.max(8, trackHeight * (trackHeight / (trackHeight + this.maxScroll)));
            int thumbY = panelTop + (trackHeight - thumbHeight) * this.scrollY / this.maxScroll;
            guiGraphics.fill(trackX, thumbY, trackX + 3, thumbY + thumbHeight, 0xFFFFFFFF);
        }
    }

    /** 原版风格槽位：内凹格子（上左深色、下右亮色）。 */
    private void drawVanillaSlot(GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.fill(x + 1, y + 1, x + SLOT_SIZE - 1, y + SLOT_SIZE - 1, 0xFF8B8B8B);
        guiGraphics.fill(x + 1, y + 1, x + SLOT_SIZE - 1, y + 2, 0xFF373737);
        guiGraphics.fill(x + 1, y + 1, x + 2, y + SLOT_SIZE - 1, 0xFF373737);
        guiGraphics.fill(x + 1, y + SLOT_SIZE - 2, x + SLOT_SIZE - 1, y + SLOT_SIZE - 1, 0xFFFFFFFF);
        guiGraphics.fill(x + SLOT_SIZE - 2, y + 1, x + SLOT_SIZE - 1, y + SLOT_SIZE - 1, 0xFFFFFFFF);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaY) {
        if (this.maxScroll > 0) {
            this.scrollY = Math.max(0, Math.min(this.maxScroll,
                    this.scrollY - (int) Math.signum(deltaY) * SLOT_SIZE));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, deltaY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // E 或 ESC 关闭
        if (keyCode == 69 && !this.items.isEmpty()) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
