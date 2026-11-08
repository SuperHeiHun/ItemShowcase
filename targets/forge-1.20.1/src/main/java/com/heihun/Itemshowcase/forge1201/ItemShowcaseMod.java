package com.heihun.Itemshowcase.forge1201;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/**
 * ItemShowcase 主类（Forge 1.20.1）。
 * 生命周期与网络通道注册入口；具体子系统见各包。
 */
@Mod(ItemShowcaseMod.MOD_ID)
public class ItemShowcaseMod {

    public static final String MOD_ID = "itemshowcase";

    public ItemShowcaseMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        modBus.addListener(this::onCommonSetup);
        // Forge 原生配置（TOML，自带文件监听热重载）
        ForgeConfigs.register();
        modBus.addListener(this::onModConfigEvent);
        ItemShowcaseBootstrap.init();
    }

    private void onModConfigEvent(ModConfigEvent event) {
        // Loading（首次加载）与 Reloading（外部修改热重载）都同步到运行时配置对象
        if (event.getConfig().getSpec() == ForgeConfigs.SERVER_SPEC
                || event.getConfig().getSpec() == ForgeConfigs.CLIENT_SPEC) {
            ForgeConfigs.syncAll();
        }
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        // 网络通道注册（enqueueWork 保证线程正确）
        event.enqueueWork(ShowcaseNetworkHelper::register);
    }

    /** 间接引用，避免构造期类加载顺序问题。 */
    private static final class ShowcaseNetworkHelper {
        static void register() {
            com.heihun.Itemshowcase.forge1201.network.ShowcaseNetwork.register();
        }
    }
}
