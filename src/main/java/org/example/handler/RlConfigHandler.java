package org.example.handler;

import org.example.model.Request;
import org.example.model.Response;
import org.example.ratelimit.RateLimiter;

/**
 * RLCONFIG <limit> <window_in_seconds>
 * Sets global rate limit config. Applies to all identifiers.
 * Example: RLCONFIG 10 60  →  allow 10 requests per 60 seconds
 */
public class RlConfigHandler implements CommandHandler {

    private final RateLimiter rateLimiter;

    public RlConfigHandler(RateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    @Override
    public Response handle(Request request) {
        String[] parts = request.args().strip().split("\\s+", 2);
        if (parts.length != 2) {
            return new Response("ERROR: usage: RLCONFIG <limit> <window_in_seconds>");
        }
        try {
            int limit  = Integer.parseInt(parts[0]);
            int window = Integer.parseInt(parts[1]);
            rateLimiter.configure(limit, window);
            return new Response("OK");
        } catch (NumberFormatException e) {
            return new Response("ERROR: limit and window must be integers");
        }
    }
}