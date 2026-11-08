package com.heihun.Itemshowcase.neoforge1211.mixin;

import com.heihun.Itemshowcase.core.logging.ItemShowcaseLogger;
import com.heihun.Itemshowcase.neoforge1211.ShowcaseServices;
import com.heihun.Itemshowcase.neoforge1211.client.ChatIconOverlay;
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
 * 聊天组件渲染注入（NeoForge 1.21.1；运行时为 Mojang 官方映射，无需 refmap）。
 * 注入范围刻意收窄：render 头部（帧重置）、行 drawString（图标绘制 + 文字上移）、
 * getLineHeight 返回值（行距附加）。
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
                    target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/client/gui/FormattedCharSequence;III)I"))
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
     */
    @Inject(method = "getLineHeight()I", at = @At("RETURN"), cancellable = true)
    private void itemshowcase$extraLineSpacing(CallbackInfoReturnable<Integer> cir) {
        int extra = ShowcaseServices.clientConfig().extraLineSpacing;
        if (extra > 0) {
            cir.setReturnValue(cir.getReturnValue() + extra);
        }
    }
}
