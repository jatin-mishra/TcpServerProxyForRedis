package org.example.handler;

import org.example.model.Request;
import org.example.model.Response;
import org.example.store.KeyStore;

public class PingHandler implements CommandHandler {

    private final KeyStore store;

    public PingHandler(KeyStore store) {
        this.store = store;
    }

    @Override
    public Response handle(Request request) {
        return new Response(store.ping() ? "PONG" : "ERROR: store unavailable");
    }
}