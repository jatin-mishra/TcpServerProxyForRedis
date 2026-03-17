package org.example.acceptor;

import org.example.poller.Poller;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.logging.Logger;

/**
 * Main-thread acceptor — blocks on ServerSocketChannel.accept() and
 * hands each new connection to the Poller for monitoring.
 */
public class Acceptor implements Runnable {

    private static final Logger log = Logger.getLogger(Acceptor.class.getName());

    private final int port;
    private final Poller poller;

    public Acceptor(int port, Poller poller) {
        this.port = port;
        this.poller = poller;
    }

    @Override
    public void run() {
        try (ServerSocketChannel serverChannel = ServerSocketChannel.open()) {
            serverChannel.bind(new InetSocketAddress(port));
            serverChannel.configureBlocking(true); // main thread blocks here — intentional
            log.info("Server listening on port " + port);

            while (!Thread.currentThread().isInterrupted()) {
                SocketChannel client = serverChannel.accept();
                log.fine("Accepted connection from " + client.getRemoteAddress());
                poller.register(client);
            }
        } catch (IOException e) {
            log.severe("Acceptor failed: " + e.getMessage());
        }
    }
}