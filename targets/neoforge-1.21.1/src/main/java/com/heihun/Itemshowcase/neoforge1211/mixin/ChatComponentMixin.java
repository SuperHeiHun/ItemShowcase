package com.heihun.Itemshowcase.neoforge1211.mixin;

import com.heihun.Itemshowcase.core.logging.ItemShowcaseLogger;
import com.heihun.Itemshowcase.neoforge1211.ShowcaseServices;
import com.heihun.Itemshowcase.neoforge1211.client.ChatIconOverlay;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ChatComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 聊天组件渲染注入（NeoForge 1.21.1；运行时为 Mojang 官方映射，无需 refmap）。
 *
 * <p>本类只保留标准 @Inject 注入（render 头部帧重置、getLineHeight 行距附加）。
 * 行绘制包装（@WrapOperation，MixinExtras 延迟注入）拆分到 {@link ChatLineIconMixin}——
 * 实机验证：两类混居同一 mixin 时延迟注入会被静默跳过（详见该类注释）。</p>
 */
@Mixin(ChatComponent.class)
public abstract class ChatComponentMixin {

    @Inject(method = "render(Lnet/minecraft/client/gui/GuiGraphics;IIIZ)V", at = @At("HEAD"))
    private void itemshowcase$beginFrame(GuiGraphics guiGraphics, int tickCount, int mouseX, int mouseY,
                                         boolean isOverlay, CallbackInfo ci) {
        ChatIconOverlay.beginFrame(mouseX, mouseY);
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
