package org.example.worker;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WorkerPool {

    private final ExecutorService executor;

    public WorkerPool(int threadCount) {
        this.executor = Executors.newFixedThreadPool(threadCount);
    }

    public void submit(Runnable task) {
        executor.submit(task);
    }

    public void shutdown() {
        executor.shutdown();
    }
}