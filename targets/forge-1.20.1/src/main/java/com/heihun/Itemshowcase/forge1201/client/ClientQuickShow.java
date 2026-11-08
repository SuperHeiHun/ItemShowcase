package com.heihun.Itemshowcase.forge1201.client;

import com.heihun.Itemshowcase.forge1201.ItemShowcaseMod;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/**
 * Ctrl+Enter 快捷展示（客户端）。
 *
 * <p>规则：</p>
 * <ul>
 *   <li>快捷键默认为 Enter，触发时必须按住 Ctrl（即默认组合 Ctrl+Enter），
 *       支持 Minecraft 控制界面重新绑定。</li>
 *   <li>仅在容器界面（物品栏等）且鼠标悬停有效槽位时触发。</li>
 *   <li>客户端只发送“菜单 ID + 槽位 ID”，物品数据由服务端重新读取。</li>
 *   <li>不拦截原版按键行为（事件返回默认结果）。</li>
 * </ul>
 */
public final class ClientQuickShow {

    /** 快捷键：类别与名称进入 Controls 界面，可重新绑定。 */
    public static final KeyMapping QUICK_SHOW = new KeyMapping(
            "key.itemshowcase.quick_show",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_ENTER,
            "key.categories.itemshowcase");

    private ClientQuickShow() {
    }

    /** MOD 事件总线：注册按键。 */
    @Mod.EventBusSubscriber(modid = ItemShowcaseMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class KeyRegistration {

        @SubscribeEvent
        public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
            event.register(QUICK_SHOW);
        }
    }

    /** 游戏事件总线：容器界面内按键检测。 */
    @Mod.EventBusSubscriber(modid = ItemShowcaseMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static final class KeyHandler {

        @SubscribeEvent
        public static void onKeyPressed(ScreenEvent.KeyboardKeyPressedEvent.Pre event) {
            if (!(event.getScreen() instanceof AbstractContainerScreen<?> containerScreen)) {
                return;
            }
            // 默认 Ctrl+Enter；支持在 Controls 中重新绑定
            if (!com.mojang.blaze3d.platform.Screen.hasControlDown()) {
                return;
            }
            if (!QUICK_SHOW.matches(event.getKeyCode(), event.getScanCode())) {
                return;
            }
            Slot hovered = containerScreen.getSlotUnderMouse();
            if (hovered == null || !hovered.hasItem()) {
                return;
            }
            // 客户端只提交菜单 ID + 槽位 ID；物品由服务端校验后重新读取
            int containerId = containerScreen.getMenu().containerId;
            int slotIndex = hovered.index;
            ShowcaseNetwork.CHANNEL.sendToServer(
                    new com.heihun.Itemshowcase.forge1201.network.packet.SelectedItemRequestPacket(
                            containerId, slotIndex));
            Minecraft.getInstance().getSoundManager()
                    .play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                            net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0F));
        }
    }
}
