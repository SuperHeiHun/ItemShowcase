package com.heihun.Itemshowcase.forge1201.mixin;

import com.heihun.Itemshowcase.forge1201.client.ChatIconOverlay;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 聊天界面注入：在原版绘制完成后，若鼠标悬停在物品图标上，
 * 追加原版物品 Tooltip。不拦截任何原版事件（点击行为完全兼容）。
 */
@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin {

    @Inject(method = "render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", at = @At("TAIL"))
    private void itemshowcase$renderItemTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY,
                                                float partialTick, CallbackInfo ci) {
        ChatIconOverlay.renderHoveredTooltip(guiGraphics,
                net.minecraft.client.Minecraft.getInstance().font, mouseX, mouseY);
    }
}
