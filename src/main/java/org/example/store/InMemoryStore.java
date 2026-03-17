package org.example.store;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory KeyStore backed by a ConcurrentHashMap.
 * Pre-seeded with sample data so STATS/PATTERN return meaningful results.
 *
 * Replace with RedisStore by implementing KeyStore and injecting it into SocketServer.
 */
public class InMemoryStore implements KeyStore {

    private final ConcurrentHashMap<String, String> data;

    public InMemoryStore() {
        data = new ConcurrentHashMap<>(Map.of(
            "foo:1", "alpha",
            "foo:2", "beta",
            "foo:3", "gamma",
            "bar:1", "delta",
            "bar:2", "epsilon",
            "baz:1", "zeta",
            "baz:2", "eta",
            "qux:1", "theta",
            "qux:2", "iota",
            "qux:3", "kappa"
        ));
    }

    @Override
    public boolean ping() {
        return true;
    }

    @Override
    public int totalKeys() {
        return data.size();
    }

    @Override
    public int countByPrefix(String prefix) {
        return (int) data.keySet().stream()
                .filter(k -> k.startsWith(prefix))
                .count();
    }
}