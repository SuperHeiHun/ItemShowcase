package com.heihun.Itemshowcase.core.protocol;

/**
 * Showcase 网络协议的包类型。
 *
 * <p>数值 ID 是线格式的一部分，必须保持稳定；新类型只能追加到末尾，
 * 不得复用或修改已有 ID。</p>
 */
public enum PacketType {

    /** 1.21.1+ 上客户端请求刷新能力快照（预留，兼容性占位从 1 开始编号）。 */
    SHOWCASE_REQUEST(1),

    /** 服务端下发的结构化展示消息（核心包）。 */
    SHOWCASE_MESSAGE(2),

    /** Ctrl+Enter：客户端提交“菜单 ID + 槽位 ID”，由服务端校验并重读真实物品。 */
    SELECTED_ITEM_REQUEST(3),

    /** 饰品栏快照（服务端 → 客户端，调试/同步用）。 */
    ACCESSORY_SNAPSHOT(4),

    /** 背包快照（服务端 → 客户端，调试/同步用）。 */
    INVENTORY_SNAPSHOT(5),

    /** 末影箱快照（服务端 → 客户端，调试/同步用）。 */
    ENDER_CHEST_SNAPSHOT(6),

    /** 背包内部库存快照（服务端 → 客户端，调试/同步用）。 */
    BACKPACK_SNAPSHOT(7),

    /** 客户端能力握手：声明协议版本、支持的宏、是否具备图标渲染能力。 */
    CLIENT_CAPABILITY_HANDSHAKE(8);

    private final int id;

    PacketType(int id) {
        this.id = id;
    }

    public int id() {
        return id;
    }

    public static PacketType byId(int id) {
        for (PacketType type : values()) {
            if (type.id == id) {
                return type;
            }
        }
        return null;
    }
}
