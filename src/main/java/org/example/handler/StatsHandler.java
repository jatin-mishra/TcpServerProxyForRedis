package org.example.handler;

import org.example.model.Request;
import org.example.model.Response;
import org.example.store.KeyStore;

public class StatsHandler implements CommandHandler {

    private final KeyStore store;

    public StatsHandler(KeyStore store) {
        this.store = store;
    }

    @Override
    public Response handle(Request request) {
        return new Response("TOTAL_KEYS:" + store.totalKeys());
    }
}