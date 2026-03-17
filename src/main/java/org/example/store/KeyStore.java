package org.example.store;

/**
 * Abstraction over the key store.
 * Swap InMemoryStore for RedisStore (or any other backend) without touching handlers.
 */
public interface KeyStore {

    /** Health-check — returns true if the store is reachable. */
    boolean ping();

    int totalKeys();

    int countByPrefix(String prefix);

    /**
     * Counts keys where prefix matches in any of: key name, hash field, or hash value.
     * Defaults to 0 for stores that don't support hash types.
     */
    default int countByPrefixInHash(String prefix) {
        return 0;
    }
}