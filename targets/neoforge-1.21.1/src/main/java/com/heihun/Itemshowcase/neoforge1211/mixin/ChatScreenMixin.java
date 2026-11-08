package com.heihun.Itemshowcase.neoforge1211.mixin;

import com.heihun.Itemshowcase.neoforge1211.client.ChatIconOverlay;
import com.heihun.Itemshowcase.neoforge1211.client.ShowcaseMessageHandler;
import com.heihun.Itemshowcase.neoforge1211.client.SnapshotViewerScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 聊天界面注入（NeoForge 1.21.1）：悬停 Tooltip + 入口点击。不拦截原版其他事件。
 */
@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin {

    @Inject(method = "render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", at = @At("TAIL"))
    private void itemshowcase$renderItemTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY,
                                                float partialTick, CallbackInfo ci) {
        ChatIconOverlay.renderHoveredTooltip(guiGraphics,
                net.minecraft.client.Minecraft.getInstance().font, mouseX, mouseY);
    }

    /**
     * 点击处理：命中“容器入口”时打开快照查看 GUI 并吞掉事件（不影响原版
     * 聊天点击行为——未命中时返回默认结果）。
     */
    @Inject(method = "mouseClicked(DDI)Z", at = @At("HEAD"), cancellable = true)
    private void itemshowcase$onMouseClicked(double mouseX, double mouseY, int button,
                                             CallbackInfoReturnable<Boolean> cir) {
        if (button != 0) {
            return;
        }
        ShowcaseMessageHandler.ChatEntry entry =
                ChatIconOverlay.findClicked((int) mouseX, (int) mouseY);
        if (entry instanceof ShowcaseMessageHandler.ContainerEntry container) {
            SnapshotViewerScreen.open(container);
            cir.setReturnValue(true);
        }
    }
}
