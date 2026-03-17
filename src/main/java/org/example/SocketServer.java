package org.example;

import org.example.acceptor.Acceptor;
import org.example.handler.*;
import org.example.poller.Poller;
import org.example.ratelimit.RateLimiter;
import org.example.store.KeyStore;
import org.example.worker.WorkerPool;

import java.io.IOException;
import java.nio.channels.Selector;
import java.util.logging.Logger;

/**
 * Wires together all components and starts the server.
 *
 * Thread layout:
 *   - main thread  → Acceptor (blocking accept loop)
 *   - poller thread → Poller (NIO Selector loop)
 *   - worker threads → WorkerPool (fixed thread pool)
 */
public class SocketServer {

    private static final Logger log = Logger.getLogger(SocketServer.class.getName());

    private final int port;
    private final int workerThreads;
    private final KeyStore store;
    private final RateLimiter rateLimiter;

    public SocketServer(int port, int workerThreads, KeyStore store, RateLimiter rateLimiter) {
        this.port = port;
        this.workerThreads = workerThreads;
        this.store = store;
        this.rateLimiter = rateLimiter;
    }

    public void start() throws IOException {
        HandlerRegistry registry = buildRegistry();
        WorkerPool workerPool = new WorkerPool(workerThreads);
        Selector selector = Selector.open();

        Poller poller = new Poller(selector, workerPool, registry);
        Thread pollerThread = new Thread(poller, "poller");
        pollerThread.setDaemon(true);
        pollerThread.start();

        // Acceptor runs on the calling (main) thread — blocks until interrupted
        log.info("Starting SocketServer on port " + port + " with " + workerThreads + " worker threads");
        new Acceptor(port, poller).run();
    }

    private HandlerRegistry buildRegistry() {
        HandlerRegistry registry = new HandlerRegistry();
        registry.register("PING",      new PingHandler(store));
        registry.register("STATS",     new StatsHandler(store));
        registry.register("PATTERN",   new PatternHandler(store));
        registry.register("RLCONFIG",  new RlConfigHandler(rateLimiter));
        registry.register("RATELIMIT", new RateLimitHandler(rateLimiter));
        registry.register("RLSTATS",   new RlStatsHandler(rateLimiter));
        return registry;
    }
}