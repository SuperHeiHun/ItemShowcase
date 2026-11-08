package com.heihun.Itemshowcase.forge1201.server;

import com.heihun.Itemshowcase.core.protocol.ClientCapabilities;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 客户端能力追踪：UUID → 能力。
 * 决定服务端向该玩家发送结构化展示包还是原版回退文本，避免两种消息重复发送。
 * 玩家退出时由事件清理。
 */
public final class CapabilityTracker {

    private static final Map<UUID, ClientCapabilities> CAPABILITIES = new ConcurrentHashMap<>();

    private CapabilityTracker() {
    }

    public static void set(UUID playerId, ClientCapabilities capabilities) {
        if (capabilities != null && capabilities.structuredChatSupported()) {
            CAPABILITIES.put(playerId, capabilities);
        } else {
            CAPABILITIES.remove(playerId);
        }
    }

    /** 未握手的玩家返回 null（按原版客户端处理）。 */
    public static ClientCapabilities get(UUID playerId) {
        return CAPABILITIES.get(playerId);
    }

    public static void remove(UUID playerId) {
        CAPABILITIES.remove(playerId);
    }
}
