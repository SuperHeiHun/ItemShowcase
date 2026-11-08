package com.heihun.Itemshowcase.forge1201.server;

import com.heihun.Itemshowcase.core.config.ItemShowcaseConfig;
import com.heihun.Itemshowcase.core.logging.ItemShowcaseLogger;
import com.heihun.Itemshowcase.core.model.ShowcaseMessage;
import com.heihun.Itemshowcase.core.validate.MessageValidator;
import com.heihun.Itemshowcase.core.validate.ShowcaseLimits;
import com.heihun.Itemshowcase.forge1201.ShowcaseServices;
import com.heihun.Itemshowcase.forge1201.network.ShowcaseNetwork;
import com.heihun.Itemshowcase.forge1201.network.packet.ShowcaseMessagePacket;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * 展示消息广播服务：校验 → 按接收者能力分发。
 *
 * <p>分发规则：</p>
 * <ul>
 *   <li>ItemShowcase 客户端（已握手）→ SHOWCASE_MESSAGE 结构化包。</li>
 *   <li>原版客户端 → 可读回退文本（物品名 + 原版 show_item 悬停）。</li>
 *   <li>两者互斥，不会重复收到消息。</li>
 * </ul>
 */
public final class BroadcastService {

    private BroadcastService() {
    }

    public static void broadcast(MinecraftServer server, ShowcaseMessage message) {
        ItemShowcaseConfig.Server config = ShowcaseServices.serverConfig();

        MessageValidator.ValidationResult result =
                MessageValidator.validate(message, ShowcaseLimits.fromConfig(config));
        if (!result.ok()) {
            ItemShowcaseLogger.warn("展示消息未通过校验, 已拒绝: " + result.violations());
            return;
        }

        byte[] payload = message.encodeToBytes();
        MutableComponent fallback = config.sendVanillaFallback ? FallbackTextBuilder.build(message) : null;

        for (ServerPlayer receiver : server.getPlayerList().getPlayers()) {
            if (CapabilityTracker.get(receiver.getUUID()) != null) {
                ShowcaseNetwork.sendTo(receiver, new ShowcaseMessagePacket(payload));
            } else if (fallback != null) {
                receiver.sendSystemMessage(fallback);
            }
        }
    }

    /** 向发送者本人发送系统提示（限流 / 校验失败时）。 */
    public static void notice(ServerPlayer player, String translationKey) {
        player.sendSystemMessage(Component.translatable(translationKey));
    }
}
