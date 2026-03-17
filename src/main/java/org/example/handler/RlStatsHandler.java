package org.example.handler;

import org.example.model.Request;
import org.example.model.Response;
import org.example.ratelimit.RateLimiter;

/**
 * RLSTATS <identifier>
 * Returns remaining request capacity for the identifier in the current window.
 * Also evicts expired entries before counting, so the value is always fresh.
 * Response: REMAINING:<count>
 */
public class RlStatsHandler implements CommandHandler {

    private final RateLimiter rateLimiter;

    public RlStatsHandler(RateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    @Override
    public Response handle(Request request) {
        String identifier = request.args().strip();
        if (identifier.isBlank()) {
            return new Response("ERROR: usage: RLSTATS <identifier>");
        }
        return new Response("REMAINING:" + rateLimiter.remaining(identifier));
    }
}