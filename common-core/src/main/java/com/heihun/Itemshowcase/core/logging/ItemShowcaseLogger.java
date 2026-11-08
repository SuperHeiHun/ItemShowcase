package com.heihun.Itemshowcase.core.logging;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 统一日志门面。
 *
 * <p>common-core 不允许依赖特定日志实现（1.16.5 使用 log4j2，1.20.1+ 使用 slf4j），
 * 各平台适配器在初始化时通过 {@link #use(Sink)} 注入自己的日志实现；
 * 未注入时回退到 java.util.logging，保证单元测试与独立运行时也有日志。</p>
 *
 * <p>禁止在各平台适配器中直接 System.out 打印。</p>
 */
public final class ItemShowcaseLogger {

    public enum Severity {
        DEBUG, INFO, WARN, ERROR
    }

    /** 平台日志适配接口。 */
    public interface Sink {
        void log(Severity severity, String message, Throwable throwable);
    }

    private static volatile Sink sink = new JulSink();

    private ItemShowcaseLogger() {
    }

    /** 平台初始化时调用一次，把平台 Logger 接入门面。 */
    public static void use(Sink newSink) {
        sink = (newSink == null) ? new JulSink() : newSink;
    }

    public static void debug(String message) {
        dispatch(Severity.DEBUG, message, null);
    }

    public static void debug(String message, Throwable t) {
        dispatch(Severity.DEBUG, message, t);
    }

    public static void info(String message) {
        dispatch(Severity.INFO, message, null);
    }

    public static void info(String message, Throwable t) {
        dispatch(Severity.INFO, message, t);
    }

    public static void warn(String message) {
        dispatch(Severity.WARN, message, null);
    }

    public static void warn(String message, Throwable t) {
        dispatch(Severity.WARN, message, t);
    }

    public static void error(String message) {
        dispatch(Severity.ERROR, message, null);
    }

    public static void error(String message, Throwable t) {
        dispatch(Severity.ERROR, message, t);
    }

    private static void dispatch(Severity severity, String message, Throwable t) {
        Sink current = sink;
        try {
            current.log(severity, message, t);
        } catch (Throwable sinkFailure) {
            // 日志系统自身故障不允许传播到业务逻辑
            new JulSink().log(Severity.ERROR, "ItemShowcase 日志 Sink 抛出异常: " + sinkFailure, sinkFailure);
        }
    }

    /** 默认实现：java.util.logging。 */
    private static final class JulSink implements Sink {
        private final Logger logger = Logger.getLogger("ItemShowcase");

        @Override
        public void log(Severity severity, String message, Throwable throwable) {
            Level level;
            switch (severity) {
                case DEBUG: level = Level.FINE; break;
                case INFO: level = Level.INFO; break;
                case WARN: level = Level.WARNING; break;
                case ERROR: level = Level.SEVERE; break;
                default: level = Level.INFO; break;
            }
            if (throwable != null) {
                logger.log(level, message, throwable);
            } else {
                logger.log(level, message);
            }
        }
    }
}
