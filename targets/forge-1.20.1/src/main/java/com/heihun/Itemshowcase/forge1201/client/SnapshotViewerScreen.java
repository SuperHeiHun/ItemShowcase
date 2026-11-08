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
        // 面板背景与边框
        guiGraphics.fill(this.gridLeft - 4, this.gridTop - 4,
                this.gridLeft + gridWidth + 4, this.gridTop + gridHeight + 4, 0xC0101010);
        guiGraphics.renderOutline(this.gridLeft - 4, this.gridTop - 4,
                gridWidth + 8, gridHeight + 8, 0xFF50505A);

        // 标题
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, this.gridTop - 20, 0xFFFFFF);

        // 网格（裁剪 + 滚动）
        guiGraphics.enableScissor(this.gridLeft, this.gridTop,
                this.gridLeft + gridWidth, this.gridTop + gridHeight);

        int hoveredIndex = -1;
        int hoveredX = -1;
        int hoveredY = -1;
        for (int i = 0; i < this.items.size(); i++) {
            int col = i % COLUMNS;
            int row = i / COLUMNS;
            int x = this.gridLeft + col * SLOT_SIZE + 1;
            int y = this.gridTop + row * SLOT_SIZE + 1 - this.scrollY;
            if (y + 16 < this.gridTop || y > this.gridTop + gridHeight) {
                continue;
            }
            guiGraphics.fill(x - 1, y - 1, x + 17, y + 17, 0x80202028);
            ItemStack stack = this.items.get(i).stack();
            if (!stack.isEmpty()) {
                guiGraphics.renderItem(stack, x, y);
                guiGraphics.renderItemDecorations(this.font, stack, x, y, null);
            }
            if (mouseX >= x - 1 && mouseX < x + 17 && mouseY >= y - 1 && mouseY < y + 17) {
                hoveredIndex = i;
                hoveredX = x;
                hoveredY = y;
            }
        }
        if (hoveredIndex >= 0) {
            // 悬停高亮
            guiGraphics.fill(hoveredX - 1, hoveredY - 1, hoveredX + 17, hoveredY + 17, 0x60FFFFFF);
        }
        guiGraphics.disableScissor();

        if (hoveredIndex >= 0) {
            ItemStack stack = this.items.get(hoveredIndex).stack();
            if (!stack.isEmpty() && ShowcaseServices.clientConfig().showTooltip) {
                guiGraphics.renderTooltip(this.font, stack, mouseX, mouseY);
            }
        }

        // 滚动指示
        if (this.maxScroll > 0) {
            int trackX = this.gridLeft + gridWidth + 2;
            int trackHeight = gridHeight;
            guiGraphics.fill(trackX, this.gridTop, trackX + 2, this.gridTop + trackHeight, 0xFF2A2A32);
            int thumbHeight = Math.max(8, trackHeight * (trackHeight / (trackHeight + this.maxScroll)));
            int thumbY = this.gridTop + (trackHeight - thumbHeight) * this.scrollY / this.maxScroll;
            guiGraphics.fill(trackX, thumbY, trackX + 2, thumbY + thumbHeight, 0xFF9090A0);
        }
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
