package com.heihun.Itemshowcase.forge1201.client;

import com.heihun.Itemshowcase.core.logging.ItemShowcaseLogger;

/**
 * 客户端初始化入口（仅 Dist.CLIENT 时被引用）。
 * 按键注册与网络握手通过各自的 EventBusSubscriber 自动完成；
 * 此处进行显式的客户端状态初始化。
 */
public final class ClientInit {

    private ClientInit() {
    }

    public static void init() {
        ChatIconOverlay.class.getName();
    }
}
