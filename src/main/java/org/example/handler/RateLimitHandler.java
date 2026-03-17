package org.example.handler;

import org.example.model.Request;
import org.example.model.Response;
import org.example.ratelimit.RateLimiter;

/**
 * RATELIMIT <identifier> <action>
 * Checks whether the request for the given identifier is within the allowed rate.
 * <action> is parsed but ignored for now.
 * Response: ALLOWED or THROTTLED
 */
public class RateLimitHandler implements CommandHandler {

    private final RateLimiter rateLimiter;

    public RateLimitHandler(RateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    @Override
    public Response handle(Request request) {
        String[] parts = request.args().strip().split("\\s+", 2);
        if (parts.length < 1 || parts[0].isBlank()) {
            return new Response("ERROR: usage: RATELIMIT <identifier> <action>");
        }
        String identifier = parts[0];
        return new Response(rateLimiter.isAllowed(identifier) ? "ALLOWED" : "THROTTLED");
    }
}