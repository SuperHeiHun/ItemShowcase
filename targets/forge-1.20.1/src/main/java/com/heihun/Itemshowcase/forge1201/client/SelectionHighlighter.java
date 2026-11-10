package com.heihun.Itemshowcase.forge1201.client;

import com.heihun.Itemshowcase.core.config.ItemShowcaseConfig;
import com.heihun.Itemshowcase.forge1201.ShowcaseServices;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraftforge.client.event.ScreenEvent;

/**
 * 容器 GUI 选中高亮（Forge 1.20.1）：为 Enter 键选中的槽位绘制可配置颜色边框（默认亮绿）。
 * 支持多选；选中的框持续显示（粘性），直到取消选中、清空条件触发或发送完成。
 *
 * <p>清空条件（与发送守卫一致）：</p>
 * <ul>
 *   <li>光标上拿着物品（menu.carried 非空）→ 清空全部选中；</li>
 *   <li>关闭背包 / 打开其他容器 → 选中集合随界面重置；</li>
 *   <li>再次 Enter → 切换该槽位选中状态。</li>
 * </ul>
 *
 * <p>颜色来自客户端配置 {@code highlightColor}（十六进制 RGB），非法值由配置层回退亮绿。</p>
 */
public final class SelectionHighlighter {

    /** 颜色解析缓存：仅在配置字符串变化时重新解析（每帧调用，避免反复 parse）。 */
    private static volatile String cachedRaw = null;
    private static volatile int cachedArgb = 0xFF39FF14;

    private SelectionHighlighter() {
    }

    /** ScreenEvent.Render.Post：为全部选中槽位描框。 */
    public static void render(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> screen)) {
            return;
        }
        // 拿起物品 / 界面切换 → 清空（取消条件）
        ContainerSelection.clearIfCarrying(screen);
        ContainerSelection.clearIfScreenChanged(screen);

        ItemShowcaseConfig.Client config = ShowcaseServices.clientConfig();
        int color = resolveColor(config.highlightColor);
        for (int index : ContainerSelection.selectedIndices()) {
            if (index < 0 || index >= screen.getMenu().slots.size()) {
                continue;
            }
            Slot slot = screen.getMenu().slots.get(index);
            if (!slot.hasItem()) {
                continue;
            }
            int x = screen.getGuiLeft() + slot.x;
            int y = screen.getGuiTop() + slot.y;
            // 双层描边形成 2px 亮色框（GUI 缩放下清晰可见）
            event.getGuiGraphics().renderOutline(x - 1, y - 1, 18, 18, color);
            event.getGuiGraphics().renderOutline(x - 2, y - 2, 20, 20, color);
        }
    }

    /** 解析并缓存高亮颜色。 */
    private static int resolveColor(String raw) {
        String key = raw == null ? "" : raw.trim();
        if (!key.equals(cachedRaw)) {
            ItemShowcaseConfig.Client probe = new ItemShowcaseConfig.Client();
            probe.highlightColor = key;
            probe.sanitize();
            cachedArgb = probe.highlightColorArgb();
            cachedRaw = key;
        }
        return cachedArgb;
    }
}
