package com.heihun.Itemshowcase.core.validate;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RateLimiterTest {

    @Test
    void allowsBurstThenBlocks() {
        RateLimiter limiter = new RateLimiter(3, 10);
        UUID player = UUID.randomUUID();
        assertTrue(limiter.tryAcquire(player));
        assertTrue(limiter.tryAcquire(player));
        assertTrue(limiter.tryAcquire(player));
        assertFalse(limiter.tryAcquire(player), "窗口内第 4 条应被限流");
    }

    @Test
    void refillsOverTime() {
        RateLimiter limiter = new RateLimiter(2, 10);
        UUID player = UUID.randomUUID();
        long now = 1_000_000L;
        assertTrue(limiter.tryAcquire(player, now));
        assertTrue(limiter.tryAcquire(player, now));
        assertFalse(limiter.tryAcquire(player, now + 1000));
        // 一个完整窗口后应恢复全部令牌
        assertTrue(limiter.tryAcquire(player, now + 10_000 + 1));
    }

    @Test
    void playersAreIndependent() {
        RateLimiter limiter = new RateLimiter(1, 10);
        UUID playerA = UUID.randomUUID();
        UUID playerB = UUID.randomUUID();
        assertTrue(limiter.tryAcquire(playerA));
        assertFalse(limiter.tryAcquire(playerA));
        assertTrue(limiter.tryAcquire(playerB), "其他玩家不应被 A 的限流影响");
    }

    @Test
    void nullPlayerIsRejected() {
        RateLimiter limiter = new RateLimiter(5, 10);
        assertFalse(limiter.tryAcquire(null));
    }
}
