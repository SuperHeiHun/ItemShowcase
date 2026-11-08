package com.heihun.Itemshowcase.forge1201.mixin;

import com.heihun.Itemshowcase.core.logging.ItemShowcaseLogger;
import com.heihun.Itemshowcase.forge1201.client.ChatIconOverlay;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 聊天组件渲染注入。
 *
 * <p>注入范围刻意收窄到两处：</p>
 * <ul>
 *   <li>{@code render} 头部：重置每帧悬停状态。</li>
 *   <li>{@code render} 中 {@link GuiGraphics#drawString(Font, FormattedCharSequence, int, int, int, boolean)}
 *       调用点（每行一次）：绘制占位 run 对应的物品图标。</li>
 * </ul>
 * 不修改原版任何其他聊天逻辑（滚动、历史、输入、签名指示器等）。
 */
@Mixin(ChatComponent.class)
public abstract class ChatComponentMixin {

    @Inject(method = "render(Lnet/minecraft/client/gui/GuiGraphics;IIIZ)V", at = @At("HEAD"))
    private void itemshowcase$beginFrame(GuiGraphics guiGraphics, int tickCount, int mouseX, int mouseY,
                                         boolean isOverlay, CallbackInfo ci) {
        ChatIconOverlay.beginFrame(mouseX, mouseY);
    }

    @Redirect(
            method = "render(Lnet/minecraft/client/gui/GuiGraphics;IIIZ)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/client/gui/FormattedCharSequence;IIIZ)I"))
    private int itemshowcase$drawLineWithIcons(GuiGraphics guiGraphics, Font font, FormattedCharSequence line,
                                               int x, int y, int color, boolean dropShadow) {
        int result = guiGraphics.drawString(font, line, x, y, color, dropShadow);
        try {
            ChatIconOverlay.renderLine(guiGraphics, font, line, x, y);
        } catch (Throwable renderFailure) {
            // 图标渲染故障不允许破坏聊天绘制
            ItemShowcaseLogger.warn("聊天行图标渲染异常", renderFailure);
        }
        return result;
    }
}
