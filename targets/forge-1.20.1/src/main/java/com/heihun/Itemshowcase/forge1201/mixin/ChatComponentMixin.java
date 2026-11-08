package com.heihun.Itemshowcase.forge1201.mixin;

import com.heihun.Itemshowcase.core.logging.ItemShowcaseLogger;
import com.heihun.Itemshowcase.forge1201.ShowcaseServices;
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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

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

    @Inject(method = "render(Lnet/minecraft/client/gui/GuiGraphics;III)V", at = @At("HEAD"))
    private void itemshowcase$beginFrame(GuiGraphics guiGraphics, int tickCount, int mouseX, int mouseY,
                                         CallbackInfo ci) {
        ChatIconOverlay.beginFrame(mouseX, mouseY);
    }

    @Redirect(
            method = "render(Lnet/minecraft/client/gui/GuiGraphics;III)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;III)I"))
    private int itemshowcase$drawLineWithIcons(GuiGraphics guiGraphics, Font font, FormattedCharSequence line,
                                               int x, int y, int color) {
        // 行步进加大后，文字整体向上偏移（用户要求：上方留白、与输入框拉开距离）
        int shift = -Math.floorDiv(ShowcaseServices.clientConfig().extraLineSpacing, 2);
        int drawY = y + shift;
        int result = guiGraphics.drawString(font, line, x, drawY, color);
        try {
            ChatIconOverlay.renderLine(guiGraphics, font, line, x, drawY);
        } catch (Throwable renderFailure) {
            // 图标渲染故障不允许破坏聊天绘制
            ItemShowcaseLogger.warn("聊天行图标渲染异常", renderFailure);
        }
        return result;
    }

    /**
     * 聊天行距附加量：原版行步进 = 9px + getLineHeight()（读取原版“聊天间距”设置）。
     * 16px 图标需要更大的行距才不拥挤，此处按客户端配置附加若干像素。
     * 影响全部聊天行（含无物品消息），保证整体视觉一致。
     */
    @Inject(method = "getLineHeight()I", at = @At("RETURN"), cancellable = true)
    private void itemshowcase$extraLineSpacing(CallbackInfoReturnable<Integer> cir) {
        int extra = ShowcaseServices.clientConfig().extraLineSpacing;
        if (extra > 0) {
            cir.setReturnValue(cir.getReturnValue() + extra);
        }
    }
}
