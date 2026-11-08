package com.heihun.Itemshowcase.neoforge1211.server;

import com.heihun.Itemshowcase.core.config.ItemShowcaseConfig;
import com.heihun.Itemshowcase.core.logging.ItemShowcaseLogger;
import com.heihun.Itemshowcase.core.macros.MacroParser;
import com.heihun.Itemshowcase.core.macros.MacroParseResult;
import com.heihun.Itemshowcase.core.model.ShowcaseMessage;
import com.heihun.Itemshowcase.core.model.ShowcaseMessageFactory;
import com.heihun.Itemshowcase.core.validate.MessageValidator;
import com.heihun.Itemshowcase.core.validate.ShowcaseLimits;
import com.heihun.Itemshowcase.neoforge1211.ShowcaseServices;
import com.heihun.Itemshowcase.neoforge1211.network.NeoNetwork;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * 展示消息广播服务（NeoForge 1.21.1）：校验 → 按接收者能力分发。
 *
 * <p>ItemShowcase 客户端（已握手）→ SHOWCASE_MESSAGE 结构化包；
 * 原版客户端 → 可读回退文本（物品名 + 原版 show_item 悬停）。两者互斥。</p>
 */
public final class BroadcastService {

    private BroadcastService() {
    }

    /** 编码物品（1.21.1 需要 registries；服务器线程上从 server 拿）。 */
    public static HolderLookup.Provider registries(MinecraftServer server) {
        return server.registryAccess();
    }

    public static com.heihun.Itemshowcase.core.protocol.ItemStackPayload encodeStack(
            MinecraftServer server, net.minecraft.world.item.ItemStack stack) {
        return ItemStackCodec.encode(stack, registries(server));
    }

    public static net.minecraft.world.item.ItemStack decodeStack(
            com.heihun.Itemshowcase.core.protocol.ItemStackPayload payload) {
        return ItemStackCodec.decode(payload,
                net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer().registryAccess());
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
            if (NeoNetwork.isModdedClient(receiver)) {
                NeoNetwork.sendShowcaseMessage(receiver, payload);
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
