package com.heihun.Itemshowcase.neoforge1211.server;

import com.heihun.Itemshowcase.core.macros.MacroParser;
import com.heihun.Itemshowcase.core.macros.MacroParseResult;
import com.heihun.Itemshowcase.core.model.ShowcaseMessage;
import com.heihun.Itemshowcase.core.model.ShowcaseMessageFactory;
import com.heihun.Itemshowcase.neoforge1211.ShowcaseServices;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.Set;

/**
 * 服务端聊天事件接入（NeoForge 1.21.1）。
 *
 * <p>流程：{@code ServerChatEvent} → 宏预检 → 解析 → 服务端真实库存读取 →
 * 结构化消息 → 校验 → 按接收者能力分发（结构化包 / 原版回退文本）。</p>
 *
 * <p>兼容性说明（同 Forge 1.20.1）：不含宏的普通聊天完全不受影响；
 * 含宏消息取消后以系统消息形式广播，聊天插件的频道过滤对其不生效。</p>
 */
public final class ChatHandler {

    private ChatHandler() {
    }

    @SubscribeEvent
    public static void onServerChat(ServerChatEvent event) {
        String rawText = event.getRawText();
        // 快速预检：绝大多数普通消息在这里直接放行，零开销
        if (!MacroParser.containsMacro(rawText)) {
            return;
        }

        ServerPlayer player = event.getPlayer();
        MacroParseResult parsed = MacroParser.parse(rawText);
        if (parsed.macrosFound().isEmpty()) {
            return;
        }

        // 全部宏都被禁用 → 不拦截，按普通消息发送
        Set<com.heihun.Itemshowcase.core.macros.MacroType> found = Set.copyOf(parsed.macrosFound());
        boolean anyEnabled = found.stream()
                .anyMatch(macro -> ShowcaseServices.serverConfig().isMacroEnabled(macro));
        if (!anyEnabled) {
            return;
        }

        // 频率限制
        if (!ShowcaseServices.rateLimiter().tryAcquire(player.getUUID())) {
            event.setCanceled(true);
            BroadcastService.notice(player, "itemshowcase.chat.rate_limited");
            return;
        }

        // 服务端线程上读取真实库存并构建结构化消息
        ShowcaseMessageFactory factory = new ShowcaseMessageFactory(
                new NeoDataViews(player, ShowcaseServices.integrations(),
                        player.registryAccess()),
                ShowcaseServices.serverConfig());
        ShowcaseMessage message = factory.build(parsed, player.getUUID(), player.getGameProfile().getName());
        BroadcastService.broadcast(player.level().getServer(), message);

        // 消息已按能力分发，取消原版广播避免重复
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        CapabilityTracker.remove(event.getEntity().getUUID());
    }
}
