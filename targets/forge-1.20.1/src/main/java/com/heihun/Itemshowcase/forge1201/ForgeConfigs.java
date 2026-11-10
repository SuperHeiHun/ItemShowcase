package com.heihun.Itemshowcase.forge1201;

import com.heihun.Itemshowcase.core.config.ItemShowcaseConfig;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

/**
 * Forge 原生配置（TOML）：
 * <ul>
 *   <li>config/itemshowcase-common.toml —— 服务端逻辑（宏开关 / 限额 / 限流）</li>
 *   <li>config/itemshowcase-client.toml —— 客户端渲染（图标 / 行距 / 入口模式）</li>
 * </ul>
 * Forge 自带配置文件监听：外部修改文件后自动触发 {@link ModConfigEvent.Reloading}，
 * 值同步回 common-core 的配置对象（服务端管线与客户端渲染无感知地拿到新值）——
 * 即真正的热重载，无需 tick 轮询。
 */
public final class ForgeConfigs {

    // ---------------- 服务端（COMMON） ----------------

    private static final ItemShowcaseConfig.Server SERVER_DEFAULTS = freshDefaults(new ItemShowcaseConfig.Server());

    private static final ForgeConfigSpec.BooleanValue ALLOW_MACRO_ITEM;
    private static final ForgeConfigSpec.BooleanValue ALLOW_MACRO_INVENTORY;
    private static final ForgeConfigSpec.BooleanValue ALLOW_MACRO_ENDER_CHEST;
    private static final ForgeConfigSpec.BooleanValue ALLOW_MACRO_CURIOS;
    private static final ForgeConfigSpec.BooleanValue ALLOW_MACRO_PACK;
    private static final ForgeConfigSpec.BooleanValue ALLOW_CTRL_ENTER;
    private static final ForgeConfigSpec.IntValue MAX_ITEMS_PER_MESSAGE;
    private static final ForgeConfigSpec.IntValue MAX_CONTAINER_CONTENTS;
    private static final ForgeConfigSpec.IntValue MAX_MESSAGE_BYTES;
    private static final ForgeConfigSpec.IntValue MAX_ITEM_BYTES;
    private static final ForgeConfigSpec.IntValue MAX_NESTING_DEPTH;
    private static final ForgeConfigSpec.IntValue RATE_LIMIT_WINDOW;
    private static final ForgeConfigSpec.IntValue RATE_LIMIT_MAX;
    private static final ForgeConfigSpec.BooleanValue SHOW_SOURCE;
    private static final ForgeConfigSpec.BooleanValue SEND_VANILLA_FALLBACK;
    private static final ForgeConfigSpec.BooleanValue EXPAND_CONTAINER_ITEMS;

    public static final ForgeConfigSpec SERVER_SPEC;

    // ---------------- 客户端（CLIENT） ----------------

    private static final ItemShowcaseConfig.Client CLIENT_DEFAULTS = freshDefaults(new ItemShowcaseConfig.Client());

    private static final ForgeConfigSpec.BooleanValue CHAT_ICON_RENDERING;
    private static final ForgeConfigSpec.IntValue ICON_SIZE;
    private static final ForgeConfigSpec.BooleanValue SHOW_COUNT;
    private static final ForgeConfigSpec.BooleanValue SHOW_TOOLTIP;
    private static final ForgeConfigSpec.BooleanValue SHOW_SOURCE_GROUPS;
    private static final ForgeConfigSpec.IntValue ICON_SPACING;
    private static final ForgeConfigSpec.IntValue MAX_RENDERED_ITEMS;
    private static final ForgeConfigSpec.IntValue EXTRA_LINE_SPACING;
    private static final ForgeConfigSpec.BooleanValue CLICK_VIEW_GUI;
    private static final ForgeConfigSpec.EnumValue<QuickShowMode> QUICK_SHOW_MODE;
    private static final ForgeConfigSpec.ConfigValue<String> HIGHLIGHT_COLOR;

    public static final ForgeConfigSpec CLIENT_SPEC;

    static {
        ItemShowcaseConfig.Server d = SERVER_DEFAULTS;
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
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
        ForgeConfigSpec.Builder cb = new ForgeConfigSpec.Builder();
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
        QUICK_SHOW_MODE = cb.comment("快捷发送模式：DIRECT_SEND=Alt+Enter 直接发送, INSERT_MACRO=插入占位文本（未实现，保留）")
                .defineEnum("quickShowMode", QuickShowMode.DIRECT_SEND);
        HIGHLIGHT_COLOR = cb.comment("容器 GUI 选中高亮颜色（十六进制 RGB，默认亮绿 39FF14）")
                .define("highlightColor", "39FF14");
        cb.pop();
        CLIENT_SPEC = cb.build();
    }

    /** Ctrl+Enter 模式枚举（spec defineEnum 用）。 */
    public enum QuickShowMode {
        DIRECT_SEND, INSERT_MACRO
    }

    private ForgeConfigs() {
    }

    /** MOD 构造期注册配置文件。 */
    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SERVER_SPEC, "itemshowcase-common.toml");
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, CLIENT_SPEC, "itemshowcase-client.toml");
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
        c.highlightColor = HIGHLIGHT_COLOR.get();
        c.sanitize();

        ShowcaseServices.onConfigSynced();
    }

    private static <T extends ItemShowcaseConfig> T freshDefaults(T config) {
        config.applyDefaults();
        config.sanitize();
        return config;
    }
}
