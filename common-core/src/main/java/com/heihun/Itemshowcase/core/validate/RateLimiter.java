package com.heihun.Itemshowcase.core.validate;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 展示消息频率限制：每玩家令牌桶。
 *
 * <p>容量 = windowSeconds 内允许的最大消息数；令牌按恒定速率补充。
 * 线程安全；内部状态仅存 UUID，不泄漏玩家对象。</p>
 */
public final class RateLimiter {

    private static final class Bucket {
        double tokens;
        long lastRefillMillis;
    }

    private final int capacity;
    private final long windowMillis;
    private final Map<UUID, Bucket> buckets = new ConcurrentHashMap<>();

    /**
     * @param maxPerWindow 窗口内允许的最大消息数（桶容量）
     * @param windowSeconds 窗口大小（秒）
     */
    public RateLimiter(int maxPerWindow, int windowSeconds) {
        this.capacity = Math.max(1, maxPerWindow);
        this.windowMillis = Math.max(1, windowSeconds) * 1000L;
    }

    /** 尝试为该玩家消耗一个令牌；false = 触发频率限制。 */
    public boolean tryAcquire(UUID playerId) {
        return tryAcquire(playerId, System.currentTimeMillis());
    }

    /** 可注入时间戳的版本（测试用）。 */
    public boolean tryAcquire(UUID playerId, long nowMillis) {
        if (playerId == null) {
            return false;
        }
        Bucket bucket = buckets.computeIfAbsent(playerId, key -> {
            Bucket created = new Bucket();
            created.tokens = capacity;
            created.lastRefillMillis = nowMillis;
            return created;
        });
        synchronized (bucket) {
            refill(bucket, nowMillis);
            if (bucket.tokens >= 1.0) {
                bucket.tokens -= 1.0;
                return true;
            }
            return false;
        }
    }

    /** 该玩家当前是否还有可用令牌（不消耗）。 */
    public boolean hasToken(UUID playerId, long nowMillis) {
        Bucket bucket = buckets.get(playerId);
        if (bucket == null) {
            return true;
        }
        synchronized (bucket) {
            refill(bucket, nowMillis);
            return bucket.tokens >= 1.0;
        }
    }

    /** 清理长时间未活动的桶，防止长期运行内存膨胀。 */
    public void evictIdle(long nowMillis, long idleMillis) {
        for (Map.Entry<UUID, Bucket> entry : buckets.entrySet()) {
            Bucket bucket = entry.getValue();
            synchronized (bucket) {
                if (nowMillis - bucket.lastRefillMillis > idleMillis) {
                    buckets.remove(entry.getKey());
                }
            }
        }
    }

    private void refill(Bucket bucket, long nowMillis) {
        long elapsed = nowMillis - bucket.lastRefillMillis;
        if (elapsed <= 0) {
            return;
        }
        double refillAmount = (elapsed / (double) windowMillis) * capacity;
        if (refillAmount > 0) {
            bucket.tokens = Math.min(capacity, bucket.tokens + refillAmount);
            bucket.lastRefillMillis = nowMillis;
        }
    }
}
