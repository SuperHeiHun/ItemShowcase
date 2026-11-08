package com.heihun.Itemshowcase.core.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.heihun.Itemshowcase.core.logging.ItemShowcaseLogger;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.function.Supplier;

/**
 * JSON 配置管理器。
 *
 * <ul>
 *   <li>首次运行写出默认配置文件。</li>
 *   <li>加载失败（文件损坏 / 字段类型错误）时保留默认值并记录警告——
 *       错误配置不允许导致 MOD 无法启动。</li>
 *   <li>{@link #reloadIfChanged()} 支持热加载：按文件修改时间检测外部改动，
 *       由平台 tick 或命令触发。</li>
 *   <li>合并加载：JSON 中缺失的字段使用默认值，未知字段忽略。</li>
 * </ul>
 */
public final class ConfigManager<T extends ItemShowcaseConfig> {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private final Path file;
    private final Class<T> type;
    private final Supplier<T> factory;
    private final String logName;
    private volatile T config;
    private volatile long lastLoadedModified = Long.MIN_VALUE;

    public ConfigManager(Path file, Class<T> type, Supplier<T> factory, String logName) {
        this.file = file;
        this.type = type;
        this.factory = factory;
        this.logName = logName;
        this.config = freshDefaults();
    }

    /** 当前生效配置（线程安全读取）。 */
    public T get() {
        return config;
    }

    /** 新默认值实例（不共享内部状态）。 */
    private T freshDefaults() {
        T instance = factory.get();
        instance.applyDefaults();
        instance.sanitize();
        return instance;
    }

    /** 加载配置；文件不存在时写出默认文件。任何失败都回退默认值。 */
    public synchronized void load() {
        try {
            if (Files.notExists(file)) {
                save();
                lastLoadedModified = safeModified();
                return;
            }
            String json = readFile();
            if (json == null) {
                return;
            }
            T merged = mergeWithDefaults(json);
            if (merged != null) {
                merged.sanitize();
                config = merged;
                lastLoadedModified = safeModified();
            }
        } catch (Exception failure) {
            ItemShowcaseLogger.warn("加载 " + logName + " 配置失败, 使用默认配置: " + failure);
            config = freshDefaults();
        }
    }

    /**
     * 合并加载：先取默认值的 JSON 树，再用文件中的字段逐个覆盖。
     * 任何解析异常返回 null（调用方保留当前配置）。
     */
    private T mergeWithDefaults(String json) {
        JsonElement fileElement;
        try {
            fileElement = new JsonParser().parse(json);
        } catch (RuntimeException parseFailure) {
            ItemShowcaseLogger.warn(logName + " 配置文件不是合法 JSON, 使用默认配置");
            return null;
        }
        if (!(fileElement instanceof JsonObject)) {
            ItemShowcaseLogger.warn(logName + " 配置文件顶层不是 JSON 对象, 使用默认配置");
            return null;
        }
        T defaults = freshDefaults();
        JsonObject merged = GSON.toJsonTree(defaults, type).getAsJsonObject();
        for (Map.Entry<String, JsonElement> entry : fileElement.getAsJsonObject().entrySet()) {
            merged.add(entry.getKey(), entry.getValue());
        }
        return GSON.fromJson(merged, type);
    }

    /** 保存当前配置（先写临时文件再原子替换）。 */
    public synchronized void save() {
        try {
            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
            Path temp = file.resolveSibling(file.getFileName() + ".tmp");
            try (Writer writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
                GSON.toJson(config, type, writer);
            }
            Files.move(temp, file,
                    StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            lastLoadedModified = safeModified();
        } catch (IOException failure) {
            ItemShowcaseLogger.warn("保存 " + logName + " 配置失败: " + failure);
        }
    }

    /** 热加载入口：文件修改时间变化时重新加载。 */
    public synchronized void reloadIfChanged() {
        long modified = safeModified();
        if (modified != lastLoadedModified) {
            load();
        }
    }

    private long safeModified() {
        try {
            return Files.exists(file) ? Files.getLastModifiedTime(file).toMillis() : Long.MIN_VALUE;
        } catch (IOException failure) {
            return Long.MIN_VALUE;
        }
    }

    private String readFile() {
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            StringBuilder builder = new StringBuilder();
            char[] buffer = new char[4096];
            int read;
            while ((read = reader.read(buffer)) >= 0) {
                builder.append(buffer, 0, read);
            }
            return builder.toString();
        } catch (IOException failure) {
            ItemShowcaseLogger.warn("读取 " + logName + " 配置失败: " + failure);
            return null;
        }
    }
}
