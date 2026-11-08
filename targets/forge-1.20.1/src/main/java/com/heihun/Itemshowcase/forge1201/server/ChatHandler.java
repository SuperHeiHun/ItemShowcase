package com.heihun.Itemshowcase.forge1201.server;

import com.heihun.Itemshowcase.core.macros.MacroParser;
import com.heihun.Itemshowcase.core.macros.MacroParseResult;
import com.heihun.Itemshowcase.core.model.ShowcaseMessage;
import com.heihun.Itemshowcase.core.model.ShowcaseMessageFactory;
import com.heihun.Itemshowcase.forge1201.ShowcaseServices;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Set;

/**
 * 服务端聊天事件接入。
 *
 * <p>流程：{@code ServerChatEvent} → 宏预检 → 解析 → 服务端真实库存读取 →
 * 结构化消息 → 校验 → 按接收者能力分发（结构化包 / 原版回退文本）。</p>
 *
 * <p>兼容性说明：宏消息会取消原版聊天广播并自行按接收者能力分发
 * （这是 Forge 1.20.1 聊天事件取消后的标准做法）。未包含宏的普通消息
 * 完全不进入本处理器，原版聊天签名与聊天插件行为不受影响；
 * 含宏的消息取消后以系统消息形式广播，已有聊天插件的频道过滤
 * 对展示消息不生效（在兼容性文档中注明）。</p>
 */
@Mod.EventBusSubscriber(modid = com.heihun.Itemshowcase.forge1201.ItemShowcaseMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ChatHandler {

    /** 配置热加载节流：每 100 tick 检查一次。 */
    private static int tickCounter = 0;

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
        ShowcaseMessageFactory factory =
                new ShowcaseMessageFactory(new ForgeDataViews(player, ShowcaseServices.integrations()),
                        ShowcaseServices.serverConfig());
        ShowcaseMessage message = factory.build(parsed, player.getUUID(), player.getGameProfile().getName());
        BroadcastService.broadcast(player.server, message);

        // 消息已按能力分发，取消原版广播避免重复
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            tickCounter++;
            if (tickCounter >= 100) {
                tickCounter = 0;
                ShowcaseServices.tickConfigReload();
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer) {
            CapabilityTracker.remove(event.getEntity().getUUID());
        }
    }
}
