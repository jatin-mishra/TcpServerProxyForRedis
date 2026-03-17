package org.example.ratelimit;

/**
 * Contract for rate limiting algorithms.
 * Each implementation represents a distinct algorithm (sliding window, token bucket, etc.)
 * Handlers and SocketServer depend only on this interface.
 */
public interface RateLimiter {

    /** Set global limit and window. Applies to all identifiers. */
    void configure(int limit, int windowSeconds);

    /** Returns true if the request for the given identifier is within the allowed rate. */
    boolean isAllowed(String identifier);

    /** Returns remaining capacity for the identifier in the current window. */
    int remaining(String identifier);
}
