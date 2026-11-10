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
 * 客户端初始化（NeoForge 1.21.1）：按键注册、进入世界握手、Alt+Enter 快捷展示、容器选中高亮。
 * mod bus 事件用构造器 addListener 注册；游戏事件经 NeoForge.EVENT_BUS。
 */
public final class ClientInit {

    /** 快捷键：默认 Enter。Enter = 选中/取消悬停槽位（可多选）；Alt+Enter = 发送全部选中（可重新绑定主键）。 */
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

    /**
     * 游戏事件总线处理器（仅客户端 dist 注册）。
     */
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
            if (!QUICK_SHOW.matches(event.getKeyCode(), event.getScanCode())) {
                return;
            }
            // 拿着物品（光标有物品）时：清空选中且不触发（取消条件）
            if (!containerScreen.getMenu().getCarried().isEmpty()) {
                ContainerSelection.clear();
                return;
            }
            boolean altDown = net.minecraft.client.gui.screens.Screen.hasAltDown();
            Slot hovered = containerScreen.getSlotUnderMouse();

            if (altDown) {
                // Alt+Enter：发送全部选中槽位；无选中时回退为发送悬停单槽
                java.util.List<Integer> toSend = ContainerSelection.selectedIndices();
                boolean fallbackHover = false;
                if (toSend.isEmpty()) {
                    if (hovered == null || !hovered.hasItem()
                            || hovered.container != Minecraft.getInstance().player.getInventory()) {
                        return;
                    }
                    toSend = java.util.List.of(hovered.index);
                    fallbackHover = true;
                }
                Minecraft.getInstance().getSoundManager()
                        .play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                                net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0F));
                NeoNetwork.sendToServer(new SelectedItemRequestPayload(
                        containerScreen.getMenu().containerId, toSend));
                if (!fallbackHover) {
                    ContainerSelection.clearAfterSend();
                }
                return;
            }

            // Enter（无修饰键）：切换悬停槽位的选中状态（高亮）
            ContainerSelection.toggle(containerScreen, hovered);
        }

        @SubscribeEvent
        public static void onRender(ScreenEvent.Render.Post event) {
            SelectionHighlighter.render(event);
        }
    }
}
