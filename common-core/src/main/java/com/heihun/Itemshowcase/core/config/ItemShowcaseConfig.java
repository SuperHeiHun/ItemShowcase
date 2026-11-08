package com.heihun.Itemshowcase.core.config;

import com.heihun.Itemshowcase.core.macros.MacroType;

/**
 * 配置对象约定：加载前填充默认值，加载后自我修正（sanitize）。
 * 错误配置不允许导致 MOD 无法启动。
 */
public interface ItemShowcaseConfig {

    /** 填充所有字段的默认值。 */
    void applyDefaults();

    /** 加载后修正非法值（越界 / 负数 / 冲突），记录需要时由调用方打日志。 */
    void sanitize();

    /** 服务端配置：宏开关与限制。 */
    final class Server implements ItemShowcaseConfig {
        public boolean allowMacroItem = true;
        public boolean allowMacroInventory = true;
        public boolean allowMacroEnderChest = true;
        public boolean allowMacroCurios = true;
        public boolean allowMacroPack = true;
        public boolean allowCtrlEnterShowcase = true;
        /** 单条消息最大物品数量（平铺展示的物品；容器内部展开不受此限，见 maxContainerContents）。 */
        public int maxItemsPerMessage = 54;
        /** 单个容器物品（如精妙背包）展开的内容物上限（大背包可超过 54 格）。 */
        public int maxContainerContents = 108;
        /** 单条消息最大序列化数据大小（字节）。 */
        public int maxMessageBytes = 32768;
        /** 单个物品最大数据大小（字节）。 */
        public int maxItemBytes = com.heihun.Itemshowcase.core.ItemShowcaseConstants.DEFAULT_MAX_ITEM_BYTES;
        /** 背包内部库存展示的最大嵌套深度（1 = 只展开一层）。 */
        public int maxNestingDepth = 1;
        /** 频率限制：窗口大小（秒）。 */
        public int rateLimitWindowSeconds = 10;
        /** 频率限制：窗口内最大展示消息数。 */
        public int rateLimitMaxPerWindow = 6;
        /** 是否展示发送来源（玩家名 / 来源分组标题）。 */
        public boolean showSource = true;
        /** 是否向原版客户端发送可读回退消息。 */
        public boolean sendVanillaFallback = true;
        /** [i] 宏是否展开容器物品（如 Sophisticated Backpacks）的内部库存。 */
        public boolean expandContainerItems = true;

        @Override
        public void applyDefaults() {
            allowMacroItem = true;
            allowMacroInventory = true;
            allowMacroEnderChest = true;
            allowMacroCurios = true;
            allowMacroPack = true;
            allowCtrlEnterShowcase = true;
            maxItemsPerMessage = 54;
            maxContainerContents = 108;
            maxMessageBytes = 32768;
            maxItemBytes = com.heihun.Itemshowcase.core.ItemShowcaseConstants.DEFAULT_MAX_ITEM_BYTES;
            maxNestingDepth = 1;
            rateLimitWindowSeconds = 10;
            rateLimitMaxPerWindow = 6;
            showSource = true;
            sendVanillaFallback = true;
            expandContainerItems = true;
        }

        @Override
        public void sanitize() {
            maxItemsPerMessage = clamp(maxItemsPerMessage, 1, 512);
            maxContainerContents = clamp(maxContainerContents, 1, 512);
            maxMessageBytes = clamp(maxMessageBytes, 1024, 1024 * 1024);
            maxItemBytes = clamp(maxItemBytes, 256, 256 * 1024);
            maxNestingDepth = clamp(maxNestingDepth, 1, 4);
            rateLimitWindowSeconds = clamp(rateLimitWindowSeconds, 1, 3600);
            rateLimitMaxPerWindow = clamp(rateLimitMaxPerWindow, 1, 1000);
        }

        /** 宏开关统一入口。 */
        public boolean isMacroEnabled(MacroType macro) {
            if (macro == null) {
                return false;
            }
            switch (macro) {
                case ITEM: return allowMacroItem;
                case INVENTORY: return allowMacroInventory;
                case ENDER_CHEST: return allowMacroEnderChest;
                case CURIOS: return allowMacroCurios;
                case PACK: return allowMacroPack;
                default: return false;
            }
        }
    }

    /** 客户端渲染配置。 */
    final class Client implements ItemShowcaseConfig {
        /** 聊天图标渲染总开关。 */
        public boolean chatIconRendering = true;
        /** 图标尺寸（像素，聊天 GUI 缩放坐标系）。 */
        public int iconSize = 16;
        /** 是否显示物品数量覆盖层。 */
        public boolean showCount = true;
        /** 是否显示物品 Tooltip。 */
        public boolean showTooltip = true;
        /** 是否显示来源分组标题。 */
        public boolean showSourceGroups = true;
        /** 是否启用渲染动画（淡入等）。 */
        public boolean animations = true;
        /** 图标间距（像素）。 */
        public int iconSpacing = 2;
        /** 单条聊天消息最多渲染的图标数（防御渲染性能）。 */
        public int maxRenderedItems = 54;
        /**
         * 聊天行距附加量（像素）：原版行步进 = 9px + 本值。
         * 默认 7 → 行步进 16px，16px 物品图标垂直占满自己的行块、与上下行对称不重叠。
         * 注意：Minecraft 聊天行距是全局布局，无法只对单条消息生效（原版限制）。
         */
        public int extraLineSpacing = 7;
        /**
         * 容器宏（[inv]/[ec]/[c]/[pack]）的聊天展示模式：
         * true = 显示可点击入口，点击打开物品查看 GUI；
         * false = 图标平铺在聊天中。
         */
        public boolean clickViewGui = true;
        /**
         * Ctrl+Enter 快捷发送模式：DIRECT_SEND = 直接发送展示；
         * INSERT_MACRO = 在聊天输入框插入 [i] 占位。
         */
        public String quickShowMode = "DIRECT_SEND";

        @Override
        public void applyDefaults() {
            chatIconRendering = true;
            iconSize = 16;
            showCount = true;
            showTooltip = true;
            showSourceGroups = true;
            animations = true;
            iconSpacing = 2;
            maxRenderedItems = 54;
            extraLineSpacing = 16;
            clickViewGui = true;
            quickShowMode = "DIRECT_SEND";
        }

        @Override
        public void sanitize() {
            iconSize = clamp(iconSize, 8, 32);
            iconSpacing = clamp(iconSpacing, 0, 16);
            maxRenderedItems = clamp(maxRenderedItems, 1, 512);
            extraLineSpacing = clamp(extraLineSpacing, 0, 32);
            if (!"DIRECT_SEND".equals(quickShowMode) && !"INSERT_MACRO".equals(quickShowMode)) {
                quickShowMode = "DIRECT_SEND";
            }
        }

        public boolean isDirectSend() {
            return !"INSERT_MACRO".equals(quickShowMode);
        }
    }

    static int clamp(int value, int min, int max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }
}
