package com.heihun.Itemshowcase.core;

/**
 * ItemShowcase 全局常量。
 * 所有目标版本共用，禁止在此引用任何 Minecraft 类型。
 */
public final class ItemShowcaseConstants {

    /** MOD ID，同时也是网络通道命名空间。 */
    public static final String MOD_ID = "itemshowcase";

    /** 网络通道资源路径（itemshowcase:main）。 */
    public static final String CHANNEL_PATH = "main";

    /** 展示协议当前版本。载入格式或渲染行为出现不兼容变更时递增。 */
    public static final int PROTOCOL_VERSION = 1;

    /**
     * ItemStack 载荷编码“方言”标记。
     * 不同 Minecraft 版本使用不同序列化机制，客户端必须按方言解码，
     * 禁止沿用旧版本 NBT 格式假设处理 1.21.1 数据。
     */
    public static final String DIALECT_NBT_1_16 = "nbt116";
    public static final String DIALECT_NBT_1_20 = "nbt1201";
    public static final String DIALECT_COMPONENTS_1_21 = "dc1211";

    /** Envelope 魔数："ISCF"。 */
    public static final int ENVELOPE_MAGIC = ('I' << 24) | ('S' << 16) | ('C' << 8) | 'F';

    /** 单个字符串在协议层允许的最大 UTF-8 字节数（防止恶意超大 NBT / 文本）。 */
    public static final int MAX_STRING_BYTES = 262144;

    /** 物品载荷最大字节数默认值（服务端配置可覆盖，见 ServerConfig）。 */
    public static final int DEFAULT_MAX_ITEM_BYTES = 4096;

    private ItemShowcaseConstants() {
    }
}
