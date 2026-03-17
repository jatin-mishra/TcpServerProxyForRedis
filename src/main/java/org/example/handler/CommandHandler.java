package org.example.handler;

import org.example.model.Request;
import org.example.model.Response;

public interface CommandHandler {

    Response handle(Request request);
}