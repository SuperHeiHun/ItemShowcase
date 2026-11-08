package com.heihun.Itemshowcase.core.validate;

import com.heihun.Itemshowcase.core.config.ItemShowcaseConfig;

/**
 * 由服务端配置派生出的硬限制集合。
 * 校验器与网络层共用，保证“配置 → 限制”只有一处来源。
 */
public final class ShowcaseLimits {

    private final int maxItemsPerMessage;
    private final int maxMessageBytes;
    private final int maxItemBytes;
    private final int maxNestingDepth;

    public ShowcaseLimits(int maxItemsPerMessage, int maxMessageBytes, int maxItemBytes, int maxNestingDepth) {
        this.maxItemsPerMessage = Math.max(1, maxItemsPerMessage);
        this.maxMessageBytes = Math.max(64, maxMessageBytes);
        this.maxItemBytes = Math.max(64, maxItemBytes);
        this.maxNestingDepth = Math.max(1, maxNestingDepth);
    }

    public static ShowcaseLimits fromConfig(ItemShowcaseConfig.Server config) {
        return new ShowcaseLimits(
                config.maxItemsPerMessage,
                config.maxMessageBytes,
                config.maxItemBytes,
                config.maxNestingDepth);
    }

    public int maxItemsPerMessage() {
        return maxItemsPerMessage;
    }

    public int maxMessageBytes() {
        return maxMessageBytes;
    }

    public int maxItemBytes() {
        return maxItemBytes;
    }

    public int maxNestingDepth() {
        return maxNestingDepth;
    }
}
