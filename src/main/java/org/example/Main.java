package org.example;

import org.example.ratelimit.RateLimiter;
import org.example.ratelimit.SlidingWindowRateLimiter;
import org.example.store.RedisStore;

public class Main {

    static final int PORT = 9736;
    static final int WORKER_THREADS = 4;

    public static void main(String[] args) throws Exception {
        String redisHost = System.getenv().getOrDefault("REDIS_HOST", "localhost");
        int redisPort   = Integer.parseInt(System.getenv().getOrDefault("REDIS_PORT",          "6379"));
        int maxTotal    = Integer.parseInt(System.getenv().getOrDefault("REDIS_POOL_MAX",       "8"));
        int maxIdle     = Integer.parseInt(System.getenv().getOrDefault("REDIS_POOL_MAX_IDLE",  "4"));
        int minIdle     = Integer.parseInt(System.getenv().getOrDefault("REDIS_POOL_MIN_IDLE",  "1"));

        RedisStore.PoolConfig poolConfig = new RedisStore.PoolConfig(maxTotal, maxIdle, minIdle);
        RedisStore store = new RedisStore(redisHost, redisPort, poolConfig);

        RateLimiter rateLimiter = switch (System.getenv().getOrDefault("RL_ALGORITHM", "sliding_window")) {
            case "sliding_window" -> new SlidingWindowRateLimiter(store);
            default -> throw new IllegalArgumentException("Unknown RL_ALGORITHM. Supported: sliding_window");
        };

        new SocketServer(PORT, WORKER_THREADS, store, rateLimiter).start();
    }
}