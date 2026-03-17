package org.example.ratelimit;

import io.lettuce.core.ScriptOutputType;
import org.example.store.RedisStore;

/**
 * Sliding-window rate limiter backed by a Redis sorted set per identifier.
 * Reuses the RedisStore connection pool — no separate pool needed.
 *
 * Data model:
 *   rl:config          → Hash  { limit, window }    (global, set once via RLCONFIG)
 *   rl:{identifier}    → Sorted Set { member → score=epoch_ms }
 *                         member = "{epoch_ms}:{nanoTime}" for sub-ms uniqueness
 *
 * Why sorted set over a linked list?
 *   Even with monotonically arriving requests, ZREMRANGEBYSCORE is O(log N + M).
 *   A Redis List has no score-range removal — we'd need an O(N) Lua loop.
 *
 * Atomicity: both operations are single Lua scripts (no distributed lock needed).
 */
public class SlidingWindowRateLimiter implements RateLimiter {

    private static final String CONFIG_KEY = "rl:config";

    /**
     * Atomic check-and-admit:
     *   1. Read limit + window from config hash
     *   2. ZREMRANGEBYSCORE to evict expired entries
     *   3. If ZCARD < limit → ZADD + PEXPIRE → return 1 (ALLOWED)
     *   4. Else → return 0 (THROTTLED)
     */
    private static final String RATELIMIT_SCRIPT = """
            local limit  = tonumber(redis.call('HGET', KEYS[1], 'limit'))
            local window = tonumber(redis.call('HGET', KEYS[1], 'window'))
            if not limit or not window then return redis.error_reply('RLCONFIG not set') end
            local now          = tonumber(ARGV[1])
            local window_ms    = window * 1000
            local window_start = now - window_ms
            redis.call('ZREMRANGEBYSCORE', KEYS[2], '-inf', window_start - 1)
            local count = tonumber(redis.call('ZCARD', KEYS[2]))
            if count < limit then
              redis.call('ZADD', KEYS[2], now, ARGV[2])
              redis.call('PEXPIRE', KEYS[2], window_ms + 5000)
              return 1
            end
            return 0
            """;

    /**
     * Atomic remaining-capacity check:
     *   1. Read config
     *   2. ZREMRANGEBYSCORE to evict expired entries
     *   3. Return limit - ZCARD
     */
    private static final String RLSTATS_SCRIPT = """
            local limit  = tonumber(redis.call('HGET', KEYS[1], 'limit'))
            local window = tonumber(redis.call('HGET', KEYS[1], 'window'))
            if not limit or not window then return redis.error_reply('RLCONFIG not set') end
            local now          = tonumber(ARGV[1])
            local window_start = now - window * 1000
            redis.call('ZREMRANGEBYSCORE', KEYS[2], '-inf', window_start - 1)
            local count = tonumber(redis.call('ZCARD', KEYS[2]))
            return limit - count
            """;

    private final RedisStore store;

    public SlidingWindowRateLimiter(RedisStore store) {
        this.store = store;
    }

    @Override
    public void configure(int limit, int windowSeconds) {
        store.exec(commands -> {
            commands.hset(CONFIG_KEY, "limit",  String.valueOf(limit));
            commands.hset(CONFIG_KEY, "window", String.valueOf(windowSeconds));
            return null;
        });
    }

    @Override
    public boolean isAllowed(String identifier) {
        String idKey  = "rl:" + identifier;
        String member = System.currentTimeMillis() + ":" + System.nanoTime();
        Long result = store.exec(commands ->
                commands.eval(RATELIMIT_SCRIPT, ScriptOutputType.INTEGER,
                        new String[]{CONFIG_KEY, idKey},
                        String.valueOf(System.currentTimeMillis()), member));
        return result != null && result == 1L;
    }

    @Override
    public int remaining(String identifier) {
        String idKey = "rl:" + identifier;
        Long result = store.exec(commands ->
                commands.eval(RLSTATS_SCRIPT, ScriptOutputType.INTEGER,
                        new String[]{CONFIG_KEY, idKey},
                        String.valueOf(System.currentTimeMillis())));
        return result == null ? 0 : result.intValue();
    }
}