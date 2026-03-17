package org.example.store;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.ScanArgs;
import io.lettuce.core.ScanCursor;
import io.lettuce.core.ScriptOutputType;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.support.ConnectionPoolSupport;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import java.util.Map;
import java.util.function.Function;

/**
 * Redis-backed KeyStore using Lettuce with Commons Pool 2 connection pooling.
 *
 * Uses SCAN instead of KEYS to avoid blocking the Redis server.
 *
 * Configure via env vars:
 *   REDIS_HOST          (default: localhost)
 *   REDIS_PORT          (default: 6379)
 *   REDIS_POOL_MAX      (default: 8)
 *   REDIS_POOL_MAX_IDLE (default: 4)
 *   REDIS_POOL_MIN_IDLE (default: 1)
 */
public class RedisStore implements KeyStore {

    public record PoolConfig(int maxTotal, int maxIdle, int minIdle) {
        public static PoolConfig defaults() {
            return new PoolConfig(8, 4, 1);
        }

        GenericObjectPoolConfig<StatefulRedisConnection<String, String>> toCommonsConfig() {
            GenericObjectPoolConfig<StatefulRedisConnection<String, String>> cfg = new GenericObjectPoolConfig<>();
            cfg.setMaxTotal(maxTotal);
            cfg.setMaxIdle(maxIdle);
            cfg.setMinIdle(minIdle);
            return cfg;
        }
    }

    // Counts keys matching a glob pattern entirely on the Redis server — no key data sent to client.
    private static final String COUNT_BY_PREFIX_SCRIPT =
            "local cursor = '0'\n" +
            "local count = 0\n" +
            "repeat\n" +
            "  local result = redis.call('SCAN', cursor, 'MATCH', ARGV[1], 'COUNT', 100)\n" +
            "  cursor = result[1]\n" +
            "  count = count + #result[2]\n" +
            "until cursor == '0'\n" +
            "return count";

    private final GenericObjectPool<StatefulRedisConnection<String, String>> pool;

    public RedisStore(String host, int port) {
        this(host, port, PoolConfig.defaults());
    }

    public RedisStore(String host, int port, PoolConfig poolConfig) {
        RedisClient client = RedisClient.create(RedisURI.create(host, port));
        this.pool = ConnectionPoolSupport.createGenericObjectPool(client::connect, poolConfig.toCommonsConfig());
    }

    @Override
    public boolean ping() {
        try {
            return "PONG".equals(exec(commands -> commands.ping()));
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public int totalKeys() {
        return exec(commands -> commands.dbsize()).intValue();
    }

    @Override
    public int countByPrefix(String prefix) {
        return exec(commands -> {
            Long count = commands.eval(COUNT_BY_PREFIX_SCRIPT, ScriptOutputType.INTEGER,
                    new String[0], prefix + "*");
            return count.intValue();
        });
    }

    @Override
    public int countByPrefixInHash(String prefix) {
        return exec(commands -> {
            ScanCursor cursor = ScanCursor.INITIAL;
            int count = 0;
            do {
                var keyScan = commands.scan(cursor, ScanArgs.Builder.limit(100));
                for (String key : keyScan.getKeys()) {
                    if (matchesPrefix(key, prefix)) {
                        count++;
                        continue;
                    }
                    if ("hash".equals(commands.type(key))) {
                        Map<String, String> hash = commands.hgetall(key);
                        boolean matched = hash.entrySet().stream().anyMatch(e ->
                                matchesPrefix(e.getKey(), prefix) || matchesPrefix(e.getValue(), prefix));
                        if (matched) count++;
                    }
                }
                cursor = keyScan;
            } while (!cursor.isFinished());
            return count;
        });
    }

    // Borrows a connection from the pool, runs the action, and returns it automatically.
    public <T> T exec(Function<RedisCommands<String, String>, T> action) {
        try (StatefulRedisConnection<String, String> conn = pool.borrowObject()) {
            return action.apply(conn.sync());
        } catch (Exception e) {
            throw new RuntimeException("Redis pool error", e);
        }
    }

    private static boolean matchesPrefix(String s, String prefix) {
        return s != null && s.startsWith(prefix);
    }
}