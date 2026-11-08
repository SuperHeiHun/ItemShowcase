package com.heihun.Itemshowcase.neoforge1211;

import com.heihun.Itemshowcase.core.config.ItemShowcaseConfig;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * NeoForge 原生配置（TOML，ModConfigSpec）：
 * <ul>
 *   <li>config/itemshowcase-common.toml —— 服务端逻辑（宏开关 / 限额 / 限流）</li>
 *   <li>config/itemshowcase-client.toml —— 客户端渲染（图标 / 行距 / 入口模式）</li>
 * </ul>
 * NeoForge 自带配置文件监听：外部修改后自动触发 ModConfigEvent.Reloading，
 * 值同步回 common-core 配置对象（服务端管线与客户端渲染无感知）——真热重载。
 */
public final class NeoConfigs {

    private static final ItemShowcaseConfig.Server SERVER_DEFAULTS = freshDefaults(new ItemShowcaseConfig.Server());
    private static final ItemShowcaseConfig.Client CLIENT_DEFAULTS = freshDefaults(new ItemShowcaseConfig.Client());

    // ---------------- 服务端（COMMON） ----------------

    private static final ModConfigSpec.BooleanValue ALLOW_MACRO_ITEM;
    private static final ModConfigSpec.BooleanValue ALLOW_MACRO_INVENTORY;
    private static final ModConfigSpec.BooleanValue ALLOW_MACRO_ENDER_CHEST;
    private static final ModConfigSpec.BooleanValue ALLOW_MACRO_CURIOS;
    private static final ModConfigSpec.BooleanValue ALLOW_MACRO_PACK;
    private static final ModConfigSpec.BooleanValue ALLOW_CTRL_ENTER;
    private static final ModConfigSpec.IntValue MAX_ITEMS_PER_MESSAGE;
    private static final ModConfigSpec.IntValue MAX_CONTAINER_CONTENTS;
    private static final ModConfigSpec.IntValue MAX_MESSAGE_BYTES;
    private static final ModConfigSpec.IntValue MAX_ITEM_BYTES;
    private static final ModConfigSpec.IntValue MAX_NESTING_DEPTH;
    private static final ModConfigSpec.IntValue RATE_LIMIT_WINDOW;
    private static final ModConfigSpec.IntValue RATE_LIMIT_MAX;
    private static final ModConfigSpec.BooleanValue SHOW_SOURCE;
    private static final ModConfigSpec.BooleanValue SEND_VANILLA_FALLBACK;
    private static final ModConfigSpec.BooleanValue EXPAND_CONTAINER_ITEMS;

    public static final ModConfigSpec SERVER_SPEC;

    // ---------------- 客户端（CLIENT） ----------------

    private static final ModConfigSpec.BooleanValue CHAT_ICON_RENDERING;
    private static final ModConfigSpec.IntValue ICON_SIZE;
    private static final ModConfigSpec.BooleanValue SHOW_COUNT;
    private static final ModConfigSpec.BooleanValue SHOW_TOOLTIP;
    private static final ModConfigSpec.BooleanValue SHOW_SOURCE_GROUPS;
    private static final ModConfigSpec.IntValue ICON_SPACING;
    private static final ModConfigSpec.IntValue MAX_RENDERED_ITEMS;
    private static final ModConfigSpec.IntValue EXTRA_LINE_SPACING;
    private static final ModConfigSpec.BooleanValue CLICK_VIEW_GUI;
    private static final ModConfigSpec.EnumValue<QuickShowMode> QUICK_SHOW_MODE;

    public static final ModConfigSpec CLIENT_SPEC;

    /** Ctrl+Enter 模式枚举。 */
    public enum QuickShowMode {
        DIRECT_SEND, INSERT_MACRO
    }

    static {
        ItemShowcaseConfig.Server d = SERVER_DEFAULTS;
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.comment("ItemShowcase 服务端配置（修改后自动热重载）").push("server");

        b.comment("宏开关").push("macros");
        ALLOW_MACRO_ITEM = b.comment("允许 [i] 手持物品宏").define("allowMacroItem", d.allowMacroItem);
        ALLOW_MACRO_INVENTORY = b.comment("允许 [inv] 背包宏").define("allowMacroInventory", d.allowMacroInventory);
        ALLOW_MACRO_ENDER_CHEST = b.comment("允许 [ec] 末影箱宏").define("allowMacroEnderChest", d.allowMacroEnderChest);
        ALLOW_MACRO_CURIOS = b.comment("允许 [c] 饰品宏（需要饰品 API）").define("allowMacroCurios", d.allowMacroCurios);
        ALLOW_MACRO_PACK = b.comment("允许 [pack] / [p] 背包宏").define("allowMacroPack", d.allowMacroPack);
        b.pop();

        ALLOW_CTRL_ENTER = b.comment("允许 Ctrl+Enter 快捷展示").define("allowCtrlEnterShowcase", d.allowCtrlEnterShowcase);

        b.comment("限制").push("limits");
        MAX_ITEMS_PER_MESSAGE = b.comment("单条消息最大物品数量（平铺展示）")
                .defineInRange("maxItemsPerMessage", d.maxItemsPerMessage, 1, 512);
        MAX_CONTAINER_CONTENTS = b.comment("单个容器展开的内容物上限（大背包可超过 54 格）")
                .defineInRange("maxContainerContents", d.maxContainerContents, 1, 512);
        MAX_MESSAGE_BYTES = b.comment("单条消息最大序列化字节")
                .defineInRange("maxMessageBytes", d.maxMessageBytes, 1024, 1024 * 1024);
        MAX_ITEM_BYTES = b.comment("单个物品最大序列化字节")
                .defineInRange("maxItemBytes", d.maxItemBytes, 256, 256 * 1024);
        MAX_NESTING_DEPTH = b.comment("背包内部库存展示的最大嵌套深度")
                .defineInRange("maxNestingDepth", d.maxNestingDepth, 1, 4);
        b.pop();

        b.comment("频率限制").push("rateLimit");
        RATE_LIMIT_WINDOW = b.comment("限流窗口（秒）")
                .defineInRange("windowSeconds", d.rateLimitWindowSeconds, 1, 3600);
        RATE_LIMIT_MAX = b.comment("窗口内最大展示次数")
                .defineInRange("maxPerWindow", d.rateLimitMaxPerWindow, 1, 1000);
        b.pop();

        SHOW_SOURCE = b.comment("是否展示发送来源").define("showSource", d.showSource);
        SEND_VANILLA_FALLBACK = b.comment("是否向原版客户端发送可读回退消息")
                .define("sendVanillaFallback", d.sendVanillaFallback);
        EXPAND_CONTAINER_ITEMS = b.comment("[i] 宏是否展开容器物品（如精妙背包）的内部库存")
                .define("expandContainerItems", d.expandContainerItems);
        b.pop();
        SERVER_SPEC = b.build();

        ItemShowcaseConfig.Client c = CLIENT_DEFAULTS;
        ModConfigSpec.Builder cb = new ModConfigSpec.Builder();
        cb.comment("ItemShowcase 客户端配置（修改后自动热重载）").push("client");
        CHAT_ICON_RENDERING = cb.comment("聊天物品图标渲染总开关").define("chatIconRendering", c.chatIconRendering);
        ICON_SIZE = cb.comment("物品图标尺寸（像素）").defineInRange("iconSize", c.iconSize, 8, 32);
        SHOW_COUNT = cb.comment("显示物品数量覆盖层").define("showCount", c.showCount);
        SHOW_TOOLTIP = cb.comment("显示物品 Tooltip").define("showTooltip", c.showTooltip);
        SHOW_SOURCE_GROUPS = cb.comment("显示来源分组标题").define("showSourceGroups", c.showSourceGroups);
        ICON_SPACING = cb.comment("图标间距（像素）").defineInRange("iconSpacing", c.iconSpacing, 0, 16);
        MAX_RENDERED_ITEMS = cb.comment("单条聊天消息最多渲染的图标数")
                .defineInRange("maxRenderedItems", c.maxRenderedItems, 1, 512);
        EXTRA_LINE_SPACING = cb.comment("聊天行距附加量（行步进 = 9px + 本值）")
                .defineInRange("extraLineSpacing", c.extraLineSpacing, 0, 32);
        CLICK_VIEW_GUI = cb.comment("容器宏显示为可点击入口（false = 图标平铺）")
                .define("clickViewGui", c.clickViewGui);
        QUICK_SHOW_MODE = cb.comment("Ctrl+Enter 模式：DIRECT_SEND=直接发送, INSERT_MACRO=插入占位文本")
                .defineEnum("quickShowMode", QuickShowMode.DIRECT_SEND);
        cb.pop();
        CLIENT_SPEC = cb.build();
    }

    private NeoConfigs() {
    }

    /** MOD 构造期注册配置文件（NeoForge ModContainer）。 */
    public static void register(ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, SERVER_SPEC, "itemshowcase-common.toml");
        modContainer.registerConfig(ModConfig.Type.CLIENT, CLIENT_SPEC, "itemshowcase-client.toml");
    }

    /** 配置加载/重载事件：把 TOML 值同步回 common-core 配置对象。 */
    static void syncAll() {
        ItemShowcaseConfig.Server s = ShowcaseServices.serverValues();
        s.allowMacroItem = ALLOW_MACRO_ITEM.get();
        s.allowMacroInventory = ALLOW_MACRO_INVENTORY.get();
        s.allowMacroEnderChest = ALLOW_MACRO_ENDER_CHEST.get();
        s.allowMacroCurios = ALLOW_MACRO_CURIOS.get();
        s.allowMacroPack = ALLOW_MACRO_PACK.get();
        s.allowCtrlEnterShowcase = ALLOW_CTRL_ENTER.get();
        s.maxItemsPerMessage = MAX_ITEMS_PER_MESSAGE.get();
        s.maxContainerContents = MAX_CONTAINER_CONTENTS.get();
        s.maxMessageBytes = MAX_MESSAGE_BYTES.get();
        s.maxItemBytes = MAX_ITEM_BYTES.get();
        s.maxNestingDepth = MAX_NESTING_DEPTH.get();
        s.rateLimitWindowSeconds = RATE_LIMIT_WINDOW.get();
        s.rateLimitMaxPerWindow = RATE_LIMIT_MAX.get();
        s.showSource = SHOW_SOURCE.get();
        s.sendVanillaFallback = SEND_VANILLA_FALLBACK.get();
        s.expandContainerItems = EXPAND_CONTAINER_ITEMS.get();
        s.sanitize();

        ItemShowcaseConfig.Client c = ShowcaseServices.clientValues();
        c.chatIconRendering = CHAT_ICON_RENDERING.get();
        c.iconSize = ICON_SIZE.get();
        c.showCount = SHOW_COUNT.get();
        c.showTooltip = SHOW_TOOLTIP.get();
        c.showSourceGroups = SHOW_SOURCE_GROUPS.get();
        c.iconSpacing = ICON_SPACING.get();
        c.maxRenderedItems = MAX_RENDERED_ITEMS.get();
        c.extraLineSpacing = EXTRA_LINE_SPACING.get();
        c.clickViewGui = CLICK_VIEW_GUI.get();
        c.quickShowMode = QUICK_SHOW_MODE.get().name();
        c.sanitize();

        ShowcaseServices.onConfigSynced();
    }

    private static <T extends ItemShowcaseConfig> T freshDefaults(T config) {
        config.applyDefaults();
        config.sanitize();
        return config;
    }
}
