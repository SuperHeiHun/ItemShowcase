package com.heihun.Itemshowcase.core.config;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigManagerTest {

    @TempDir
    Path tempDir;

    @Test
    void writesDefaultsOnFirstLoad() throws Exception {
        Path file = tempDir.resolve("itemshowcase/server.json");
        ConfigManager<ItemShowcaseConfig.Server> manager =
                new ConfigManager<>(file, ItemShowcaseConfig.Server.class, ItemShowcaseConfig.Server::new, "server");
        manager.load();

        assertTrue(Files.exists(file), "首次加载应写出默认配置文件");
        ItemShowcaseConfig.Server config = manager.get();
        assertEquals(54, config.maxItemsPerMessage);
        assertTrue(config.allowMacroItem);
        assertTrue(config.sendVanillaFallback);
        assertEquals(1, config.maxNestingDepth);
    }

    @Test
    void loadsValuesFromFile() {
        Path file = tempDir.resolve("server.json");
        ConfigManager<ItemShowcaseConfig.Server> manager =
                new ConfigManager<>(file, ItemShowcaseConfig.Server.class, ItemShowcaseConfig.Server::new, "server");
        manager.load();
        manager.get().allowMacroCurios = false;
        manager.get().maxItemsPerMessage = 100;
        manager.save();

        ConfigManager<ItemShowcaseConfig.Server> reloaded =
                new ConfigManager<>(file, ItemShowcaseConfig.Server.class, ItemShowcaseConfig.Server::new, "server");
        reloaded.load();
        assertTrue(!reloaded.get().allowMacroCurios);
        assertEquals(100, reloaded.get().maxItemsPerMessage);
        // 未修改的字段保持默认
        assertEquals(32768, reloaded.get().maxMessageBytes);
    }

    @Test
    void corruptJsonFallsBackToDefaults() {
        Path file = tempDir.resolve("server.json");
        ConfigManager<ItemShowcaseConfig.Server> manager =
                new ConfigManager<>(file, ItemShowcaseConfig.Server.class, ItemShowcaseConfig.Server::new, "server");
        manager.load();

        ConfigManager<ItemShowcaseConfig.Server> corrupt =
                new ConfigManager<>(file, ItemShowcaseConfig.Server.class, ItemShowcaseConfig.Server::new, "server");
        try {
            Files.write(file, "{ 这不是合法 JSON !!!".getBytes("UTF-8"));
        } catch (Exception ignored) {
        }
        corrupt.load();
        // 错误配置不得导致崩溃，回退默认值
        assertEquals(54, corrupt.get().maxItemsPerMessage);
        assertTrue(corrupt.get().allowMacroItem);
    }

    @Test
    void wrongFieldTypeFallsBackToDefaults() throws Exception {
        Path file = tempDir.resolve("server.json");
        Files.write(file, "{\"maxItemsPerMessage\": \"不是数字\"}".getBytes("UTF-8"));
        ConfigManager<ItemShowcaseConfig.Server> manager =
                new ConfigManager<>(file, ItemShowcaseConfig.Server.class, ItemShowcaseConfig.Server::new, "server");
        manager.load();
        assertEquals(54, manager.get().maxItemsPerMessage);
    }

    @Test
    void sanitizeClampsIllegalValues() {
        ItemShowcaseConfig.Server config = new ItemShowcaseConfig.Server();
        config.maxItemsPerMessage = -5;
        config.maxItemBytes = 1;
        config.maxNestingDepth = 99;
        config.rateLimitWindowSeconds = 0;
        config.sanitize();
        assertEquals(1, config.maxItemsPerMessage);
        assertEquals(256, config.maxItemBytes);
        assertEquals(4, config.maxNestingDepth);
        assertEquals(1, config.rateLimitWindowSeconds);

        ItemShowcaseConfig.Client client = new ItemShowcaseConfig.Client();
        client.iconSize = 500;
        client.quickShowMode = "无意义模式";
        client.sanitize();
        assertEquals(32, client.iconSize);
        assertEquals("DIRECT_SEND", client.quickShowMode);
        assertTrue(client.isDirectSend());
    }

    @Test
    void hotReloadDetectsFileChange() throws Exception {
        Path file = tempDir.resolve("server.json");
        ConfigManager<ItemShowcaseConfig.Server> manager =
                new ConfigManager<>(file, ItemShowcaseConfig.Server.class, ItemShowcaseConfig.Server::new, "server");
        manager.load();
        assertTrue(manager.get().allowMacroInventory);

        // 外部修改配置文件
        Thread.sleep(20); // 保证修改时间戳变化
        Files.write(file, "{\"allowMacroInventory\": false}".getBytes("UTF-8"));
        manager.reloadIfChanged();
        assertTrue(!manager.get().allowMacroInventory, "热加载应生效");
    }
}
