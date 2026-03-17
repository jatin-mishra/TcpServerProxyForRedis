package org.example.handler;

import org.example.model.Request;
import org.example.model.Response;

import java.util.HashMap;
import java.util.Map;

public class HandlerRegistry {

    private static final String UNKNOWN = "ERROR: unknown command";

    private final Map<String, CommandHandler> handlers = new HashMap<>();

    public void register(String command, CommandHandler handler) {
        handlers.put(command.toUpperCase(), handler);
    }

    public Response dispatch(Request request) {
        CommandHandler handler = handlers.get(request.command());
        if (handler == null) {
            return new Response(UNKNOWN);
        }
        return handler.handle(request);
    }
}