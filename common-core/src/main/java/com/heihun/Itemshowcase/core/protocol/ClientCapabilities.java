package com.heihun.Itemshowcase.core.protocol;

import com.heihun.Itemshowcase.core.macros.MacroType;
import com.heihun.Itemshowcase.core.util.ByteReader;
import com.heihun.Itemshowcase.core.util.ByteWriter;

/**
 * 客户端能力握手数据。
 *
 * <p>服务端用它决定向该客户端发送结构化展示消息还是原版回退文本，
 * 避免“两种消息都发一遍”。未发握手或能力不完整的客户端一律按原版客户端处理。</p>
 */
public final class ClientCapabilities {

    /** 客户端已安装 ItemShowcase 且支持结构化聊天渲染。 */
    private final boolean structuredChatSupported;

    /** 客户端具备物品图标渲染能力（现阶段恒等于 structuredChatSupported，留作扩展）。 */
    private final boolean iconRenderingSupported;

    /** 客户端支持的宏集合位掩码（bit i = MacroType.ordinal()）。 */
    private final int supportedMacroMask;

    /** 客户端协议版本。 */
    private final int protocolVersion;

    public ClientCapabilities(boolean structuredChatSupported, boolean iconRenderingSupported,
                              int supportedMacroMask, int protocolVersion) {
        this.structuredChatSupported = structuredChatSupported;
        this.iconRenderingSupported = iconRenderingSupported;
        this.supportedMacroMask = supportedMacroMask;
        this.protocolVersion = protocolVersion;
    }

    public static ClientCapabilities modded() {
        int mask = 0;
        for (MacroType type : MacroType.values()) {
            mask |= type.mask();
        }
        return new ClientCapabilities(true, true, mask, com.heihun.Itemshowcase.core.ItemShowcaseConstants.PROTOCOL_VERSION);
    }

    /** 原版客户端（或未完成握手的客户端）的能力：只收普通聊天。 */
    public static ClientCapabilities vanilla() {
        return new ClientCapabilities(false, false, 0, com.heihun.Itemshowcase.core.ItemShowcaseConstants.PROTOCOL_VERSION);
    }

    public boolean structuredChatSupported() {
        return structuredChatSupported;
    }

    public boolean iconRenderingSupported() {
        return iconRenderingSupported;
    }

    public int supportedMacroMask() {
        return supportedMacroMask;
    }

    public int protocolVersion() {
        return protocolVersion;
    }

    public boolean supportsMacro(MacroType type) {
        return type != null && (supportedMacroMask & type.mask()) != 0;
    }

    public byte[] encode() {
        ByteWriter writer = new ByteWriter(16);
        writer.writeBoolean(structuredChatSupported);
        writer.writeBoolean(iconRenderingSupported);
        writer.writeVarInt(supportedMacroMask);
        writer.writeVarInt(protocolVersion);
        return writer.toByteArray();
    }

    public static ClientCapabilities decode(byte[] payload) {
        ByteReader reader = new ByteReader(payload);
        boolean structured = reader.readBoolean();
        boolean icons = reader.readBoolean();
        int mask = reader.readVarInt();
        int version = reader.readVarInt();
        reader.expectEnd();
        // 客户端声明了旧协议版本时不按 MOD 客户端处理，走回退文本。
        if (version != com.heihun.Itemshowcase.core.ItemShowcaseConstants.PROTOCOL_VERSION) {
            return vanilla();
        }
        return new ClientCapabilities(structured, icons, mask, version);
    }
}
