package com.heihun.Itemshowcase.neoforge1211.client;

import com.heihun.Itemshowcase.neoforge1211.network.NeoNetwork;
import com.heihun.Itemshowcase.neoforge1211.network.SelectedItemRequestPayload;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;

/**
 * 客户端初始化（NeoForge 1.21.1）：按键注册、进入世界握手、Ctrl+Enter 快捷展示。
 * mod bus 事件用构造器 addListener 注册；游戏事件经 NeoForge.EVENT_BUS。
 */
public final class ClientInit {

    /** 快捷键：默认 Enter + Ctrl（即默认组合 Ctrl+Enter），可在 Controls 中重新绑定。 */
    public static final KeyMapping QUICK_SHOW = new KeyMapping(
            "key.itemshowcase.quick_show",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_ENTER,
            "key.categories.itemshowcase");

    private ClientInit() {
    }

    /** Bootstrap 在客户端 dist 调用：注册 mod bus 与 game bus 监听。 */
    public static void register(IEventBus modBus) {
        // mod bus：按键注册（client-only 事件，dedicated server 不会触发）
        modBus.addListener(ClientInit::onRegisterKeyMappings);
        // game bus：进入世界握手 + 容器界面按键
        NeoForge.EVENT_BUS.register(GameEvents.class);
    }

    private static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(QUICK_SHOW);
    }

    /** 游戏事件总线处理器（仅客户端 dist 注册）。 */
    public static final class GameEvents {

        @SubscribeEvent
        public static void onLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
            // 握手声明：支持结构化聊天 + 图标渲染 + 全部宏
            NeoNetwork.sendHandshake();
        }

        @SubscribeEvent
        public static void onKeyPressed(ScreenEvent.KeyPressed.Pre event) {
            if (!(event.getScreen() instanceof AbstractContainerScreen<?> containerScreen)) {
                return;
            }
            // 默认 Ctrl+Enter；支持在 Controls 中重新绑定
            if (!net.minecraft.client.gui.screens.Screen.hasControlDown()) {
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
            Minecraft.getInstance().getSoundManager()
                    .play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                            net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0F));
            NeoNetwork.sendToServer(new SelectedItemRequestPayload(containerId, slotIndex));
        }
    }
}
