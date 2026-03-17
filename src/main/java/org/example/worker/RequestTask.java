package org.example.worker;

import org.example.handler.HandlerRegistry;
import org.example.model.Request;
import org.example.model.Response;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

/**
 * Reads one request from the channel, dispatches it, writes the response,
 * then re-registers the channel with the selector for the next read.
 */
public class RequestTask implements Runnable {

    private static final Logger log = Logger.getLogger(RequestTask.class.getName());
    private static final int BUFFER_SIZE = 1024;

    private final SocketChannel channel;
    private final Selector selector;
    private final HandlerRegistry registry;

    public RequestTask(SocketChannel channel, Selector selector, HandlerRegistry registry) {
        this.channel = channel;
        this.selector = selector;
        this.registry = registry;
    }

    @Override
    public void run() {
        try {
            String raw = readLine();
            if (raw == null) {
                // Client closed connection
                channel.close();
                return;
            }

            Request request = Request.parse(raw);
            Response response = registry.dispatch(request);

            writeResponse(response);

            // Re-register for next read
            channel.configureBlocking(false);
            channel.register(selector, SelectionKey.OP_READ);
            selector.wakeup();

        } catch (IOException e) {
            log.warning("Error handling request: " + e.getMessage());
            closeQuietly();
        }
    }

    private String readLine() throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
        StringBuilder sb = new StringBuilder();

        while (true) {
            buffer.clear();
            int bytesRead = channel.read(buffer);

            if (bytesRead == -1) return null; // EOF

            buffer.flip();
            while (buffer.hasRemaining()) {
                char c = (char) buffer.get();
                if (c == '\n') return sb.toString();
                sb.append(c);
            }
        }
    }

    private void writeResponse(Response response) throws IOException {
        byte[] bytes = response.wire().getBytes(StandardCharsets.UTF_8);
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        while (buffer.hasRemaining()) {
            channel.write(buffer);
        }
    }

    private void closeQuietly() {
        try { channel.close(); } catch (IOException ignored) {}
    }
}