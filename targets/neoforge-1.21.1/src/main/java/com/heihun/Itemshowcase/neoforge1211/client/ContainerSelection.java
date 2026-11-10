package com.heihun.Itemshowcase.neoforge1211.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 容器 GUI 的选中状态（仅客户端）：
 * Enter 键在悬停槽位上切换选中（可多选，粘性高亮），Alt+Enter 发送全部选中槽位。
 *
 * <p>清空时机：光标拿起物品、切换到其他界面（含关闭背包）、发送完成后。</p>
 */
public final class ContainerSelection {

    /** 选中集合所属的界面（切换界面即重置，跨容器不保留）。 */
    private static AbstractContainerScreen<?> boundScreen = null;
    /** 已选中的菜单槽位索引（保持选择顺序）。 */
    private static final Set<Integer> SELECTED = new LinkedHashSet<>();

    private ContainerSelection() {
    }

    /** 绑定界面变化则重置（关闭背包后重开、打开其他容器等）。 */
    public static void clearIfScreenChanged(AbstractContainerScreen<?> screen) {
        if (boundScreen != screen) {
            boundScreen = screen;
            SELECTED.clear();
        }
    }

    /** 光标拿起物品 → 清空全部选中（用户要求：拿起物品后取消高亮）。 */
    public static void clearIfCarrying(AbstractContainerScreen<?> screen) {
        if (screen != null && !screen.getMenu().getCarried().isEmpty()) {
            clear();
        }
    }

    /** 切换悬停槽位的选中状态（有物品且属于自己背包才允许）。 */
    public static boolean toggle(AbstractContainerScreen<?> screen, Slot hovered) {
        clearIfScreenChanged(screen);
        if (hovered == null || !hovered.hasItem()) {
            return false;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || hovered.container != minecraft.player.getInventory()) {
            return false;
        }
        if (!SELECTED.add(hovered.index)) {
            SELECTED.remove(hovered.index);
            return false;
        }
        return true;
    }

    /** 全部清空。 */
    public static void clear() {
        SELECTED.clear();
    }

    /** 发送后清空。 */
    public static void clearAfterSend() {
        SELECTED.clear();
    }

    /** 已选中槽位索引（按选择顺序）。 */
    public static List<Integer> selectedIndices() {
        return new ArrayList<>(SELECTED);
    }
}
