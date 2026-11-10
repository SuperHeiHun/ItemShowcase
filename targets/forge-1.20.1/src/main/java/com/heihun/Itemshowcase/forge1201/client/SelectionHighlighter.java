package com.heihun.Itemshowcase.forge1201.client;

import com.heihun.Itemshowcase.core.config.ItemShowcaseConfig;
import com.heihun.Itemshowcase.forge1201.ShowcaseServices;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraftforge.client.event.ScreenEvent;

/**
 * 容器 GUI 选中高亮（Forge 1.20.1）：鼠标悬停自己背包里的物品时，
 * 以可配置颜色绘制亮色边框（默认亮绿）。
 *
 * <p>取消条件（与发送守卫一致）：</p>
 * <ul>
 *   <li>光标上拿着物品（menu.carried 非空）→ 不绘制；</li>
 *   <li>关闭背包 → 界面销毁，高亮自然消失；</li>
 *   <li>非自己的库存槽位（容器内其他物品）→ 不绘制。</li>
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

    /** ScreenEvent.Render.Post：在高亮条件满足时描出选中框。 */
    public static void render(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> screen)) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        // 拿起物品后取消高亮
        if (!screen.getMenu().getCarried().isEmpty()) {
            return;
        }
        Slot hovered = screen.getSlotUnderMouse();
        if (hovered == null || !hovered.hasItem()) {
            return;
        }
        // 只高亮自己的库存槽位
        if (hovered.container != minecraft.player.getInventory()) {
            return;
        }
        ItemShowcaseConfig.Client config = ShowcaseServices.clientConfig();
        int color = resolveColor(config.highlightColor);
        int x = screen.getGuiLeft() + hovered.x;
        int y = screen.getGuiTop() + hovered.y;
        // 双层描边形成 2px 亮色框（GUI 缩放下清晰可见）
        event.getGuiGraphics().renderOutline(x - 1, y - 1, 18, 18, color);
        event.getGuiGraphics().renderOutline(x - 2, y - 2, 20, 20, color);
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
