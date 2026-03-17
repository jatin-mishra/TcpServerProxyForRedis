package org.example.poller;

import org.example.handler.HandlerRegistry;
import org.example.worker.RequestTask;
import org.example.worker.WorkerPool;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.logging.Logger;

/**
 * Single poller thread — monitors registered SocketChannels via NIO Selector.
 * On OP_READ: cancels interest (one-shot) and submits a RequestTask to the WorkerPool.
 */
public class Poller implements Runnable {

    private static final Logger log = Logger.getLogger(Poller.class.getName());

    private final Selector selector;
    private final WorkerPool workerPool;
    private final HandlerRegistry registry;

    public Poller(Selector selector, WorkerPool workerPool, HandlerRegistry registry) {
        this.selector = selector;
        this.workerPool = workerPool;
        this.registry = registry;
    }

    /** Called by Acceptor to register a new connection. Thread-safe via wakeup. */
    public void register(SocketChannel channel) throws IOException {
        channel.configureBlocking(false);
        channel.register(selector, SelectionKey.OP_READ);
        selector.wakeup();
    }

    @Override
    public void run() {
        log.info("Poller started");
        while (!Thread.currentThread().isInterrupted()) {
            try {
                selector.select(); // blocks until at least one channel is ready

                Iterator<SelectionKey> keys = selector.selectedKeys().iterator();
                while (keys.hasNext()) {
                    SelectionKey key = keys.next();
                    keys.remove();

                    if (!key.isValid() || !key.isReadable()) continue;

                    SocketChannel channel = (SocketChannel) key.channel();

                    // Cancel interest before handing off — prevents duplicate triggers
                    key.cancel();

                    workerPool.submit(new RequestTask(channel, selector, registry));
                }
            } catch (IOException e) {
                log.warning("Poller error: " + e.getMessage());
            }
        }
    }
}