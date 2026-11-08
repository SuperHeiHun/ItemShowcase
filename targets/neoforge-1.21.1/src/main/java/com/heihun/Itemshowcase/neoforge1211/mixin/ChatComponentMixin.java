package com.heihun.Itemshowcase.neoforge1211.mixin;

import com.heihun.Itemshowcase.core.logging.ItemShowcaseLogger;
import com.heihun.Itemshowcase.neoforge1211.ShowcaseServices;
import com.heihun.Itemshowcase.neoforge1211.client.ChatIconOverlay;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 聊天组件渲染注入（NeoForge 1.21.1；运行时为 Mojang 官方映射，无需 refmap）。
 *
 * <p>注入范围刻意收窄：render 头部（帧重置）、行 drawString（图标绘制 + 文字上移）、
 * getLineHeight 返回值（行距附加）。</p>
 *
 * <p>兼容性关键点：行绘制使用 <b>@WrapOperation</b> 而非 @Redirect——
 * 其他 mod（malilib/tweakeroo 等）也可能包装同一调用点，@Redirect 相互冲突会
 * 导致 "0/1 targets" 硬崩溃；@WrapOperation 可与其他包装链式共存。
 * 次要注入点（行距）用 require = 0 降级：极端冲突下只损失行距/图标功能，不崩游戏。</p>
 */
@Mixin(ChatComponent.class)
public abstract class ChatComponentMixin {

    @Inject(method = "render(Lnet/minecraft/client/gui/GuiGraphics;IIIZ)V", at = @At("HEAD"))
    private void itemshowcase$beginFrame(GuiGraphics guiGraphics, int tickCount, int mouseX, int mouseY,
                                         boolean isOverlay, CallbackInfo ci) {
        ChatIconOverlay.beginFrame(mouseX, mouseY);
    }

    @WrapOperation(
            method = "render(Lnet/minecraft/client/gui/GuiGraphics;IIIZ)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/client/gui/FormattedCharSequence;III)I"),
            require = 0)
    private int itemshowcase$drawLineWithIcons(GuiGraphics instance, Font font, FormattedCharSequence line,
                                               int x, int y, int color, Operation<Integer> original) {
        // 行步进加大后，文字整体向上偏移（用户要求：上方留白、与输入框拉开距离）
        int shift = -Math.floorDiv(ShowcaseServices.clientConfig().extraLineSpacing, 2);
        int drawY = y + shift;
        int result = original.call(instance, font, line, x, drawY, color);
        try {
            ChatIconOverlay.renderLine(instance, font, line, x, drawY);
        } catch (Throwable renderFailure) {
            // 图标渲染故障不允许破坏聊天绘制
            ItemShowcaseLogger.warn("聊天行图标渲染异常", renderFailure);
        }
        return result;
    }

    /**
     * 聊天行距附加量：原版行步进 = 9px + getLineHeight()（读取原版“聊天间距”设置）。
     * require = 0：极端冲突下降级为原版行距，不崩游戏。
     */
    @Inject(method = "getLineHeight()I", at = @At("RETURN"), cancellable = true, require = 0)
    private void itemshowcase$extraLineSpacing(CallbackInfoReturnable<Integer> cir) {
        int extra = ShowcaseServices.clientConfig().extraLineSpacing;
        if (extra > 0) {
            cir.setReturnValue(cir.getReturnValue() + extra);
        }
    }
}
