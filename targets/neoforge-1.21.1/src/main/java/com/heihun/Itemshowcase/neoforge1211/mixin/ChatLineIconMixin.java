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

/**
 * 聊天行绘制包装（NeoForge 1.21.1）。
 *
 * <p>刻意独立于 {@link ChatComponentMixin}：实机验证（.mixin.out 导出的混合字节码）表明，
 * 同一 mixin 类内 @Inject 与 @WrapOperation 混用时，@WrapOperation 的调用点改写在
 * MixinExtras 0.5.5 延迟注入阶段被静默跳过（处理器被复制但调用点未改写）；
 * 而同类中仅含 wrap 类处理器时（如 justzoom）织入正常。拆分为独立类后与参考环境一致。</p>
 *
 * <p>兼容性关键点：行绘制使用 <b>@WrapOperation</b> 而非 @Redirect——
 * 其他 mod（malilib/tweakeroo 等）也可能包装同一调用点，@Redirect 相互冲突会
 * 导致 "0/1 targets" 硬崩溃；@WrapOperation 可与其他包装链式共存。</p>
 */
@Mixin(ChatComponent.class)
public abstract class ChatLineIconMixin {

    /** 一次性诊断日志：确认包装真实生效（排查静默失效用）。 */
    private static boolean itemshowcase$loggedHookActive = false;

    @WrapOperation(
            method = "render(Lnet/minecraft/client/gui/GuiGraphics;IIIZ)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/client/gui/FormattedCharSequence;III)I"))
    private int itemshowcase$drawLineWithIcons(GuiGraphics instance, Font font, FormattedCharSequence line,
                                               int x, int y, int color, Operation<Integer> original) {
        if (!itemshowcase$loggedHookActive) {
            itemshowcase$loggedHookActive = true;
            ItemShowcaseLogger.info("聊天行渲染钩子已生效 (drawString 包装)");
        }
        // 行槽加高后，文字在槽内垂直居中：
        // 原版把文字画在槽底再上移 8px（槽 9px 时恰好贴合），槽加高 extra 后若不多移会贴底；
        // 居中需上移 floorDiv(extra,2)+1（+1 修正原版文字相对槽位自带的 1px 底部偏移）。
        int extra = ShowcaseServices.clientConfig().extraLineSpacing;
        int shiftUp = extra > 0 ? Math.floorDiv(extra, 2) + 1 : 0;
        int drawY = y - shiftUp;
        int result = original.call(instance, font, line, x, drawY, color);
        try {
            ChatIconOverlay.renderLine(instance, font, line, x, drawY);
        } catch (Throwable renderFailure) {
            // 图标渲染故障不允许破坏聊天绘制
            ItemShowcaseLogger.warn("聊天行图标渲染异常", renderFailure);
        }
        return result;
    }
}
