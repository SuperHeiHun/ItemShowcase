package com.heihun.Itemshowcase.forge1201.client;

import com.heihun.Itemshowcase.forge1201.ItemShowcaseMod;
import com.heihun.Itemshowcase.forge1201.network.ShowcaseNetwork;
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
 * Alt+Enter 快捷展示（客户端）。
 *
 * <p>规则：</p>
 * <ul>
 *   <li>主键默认 Enter（可重新绑定）：Enter = 选中/取消悬停槽位（可多选，粘性高亮）；
 *       Alt+Enter = 发送全部选中槽位（无选中时回退为悬停单槽）。</li>
 *   <li>仅在容器界面（物品栏等）触发；拿着物品（光标有物品）时清空选中且不触发。</li>
 *   <li>悬停/选中槽位由 {@link SelectionHighlighter} 绘制可配置颜色的选中框。</li>
 *   <li>客户端只发送“菜单 ID + 槽位 ID 列表”，物品数据由服务端重新读取。</li>
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
                    Minecraft minecraft = Minecraft.getInstance();
                    if (hovered == null || !hovered.hasItem()
                            || minecraft.player == null
                            || hovered.container != minecraft.player.getInventory()) {
                        return;
                    }
                    toSend = java.util.List.of(hovered.index);
                    fallbackHover = true;
                }
                Minecraft.getInstance().getSoundManager()
                        .play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                                net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0F));
                ShowcaseNetwork.CHANNEL.sendToServer(
                        new com.heihun.Itemshowcase.forge1201.network.packet.SelectedItemRequestPacket(
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
