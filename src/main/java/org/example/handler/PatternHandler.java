package org.example.handler;

import org.example.model.Request;
import org.example.model.Response;
import org.example.store.KeyStore;

public class PatternHandler implements CommandHandler {

    private final KeyStore store;

    public PatternHandler(KeyStore store) {
        this.store = store;
    }

    @Override
    public Response handle(Request request) {
        String prefix = request.args().strip();
        return new Response("COUNT:" + store.countByPrefix(prefix));
    }
}