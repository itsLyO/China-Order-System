package server.core;

import common.util.LoggerUtil;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Bo quan ly Thread Pool tap trung cho Server, phuc vu xu ly ket noi va tac vu da luong
 */
public class ServerThreadPool {
    private static final int CORE_POOL_SIZE = 10;
    private static final int MAX_POOL_SIZE = 200;
    private static final long KEEP_ALIVE_TIME = 60L;
    private static final int QUEUE_CAPACITY = 500;

    private static ThreadPoolExecutor executorService;

    public static synchronized ThreadPoolExecutor getInstance() {
        if (executorService == null || executorService.isShutdown()) {
            final AtomicInteger threadCounter = new AtomicInteger(0);

            executorService = new ThreadPoolExecutor(
                    CORE_POOL_SIZE,
                    MAX_POOL_SIZE,
                    KEEP_ALIVE_TIME,
                    TimeUnit.SECONDS,
                    new LinkedBlockingQueue<>(QUEUE_CAPACITY),
                    new ThreadFactory() {
                        @Override
                        public Thread newThread(Runnable r) {
                            Thread t = new Thread(r, "ServerWorker-" + threadCounter.incrementAndGet());
                            t.setDaemon(true); // Khong chan JVM thoat neu can
                            return t;
                        }
                    },
                    new ThreadPoolExecutor.CallerRunsPolicy()
            );

            LoggerUtil.info("ServerThreadPool", String.format(
                    "Khoi tao ThreadPool: Core=%d, Max=%d, KeepAlive=%ds, Queue=%d",
                    CORE_POOL_SIZE, MAX_POOL_SIZE, KEEP_ALIVE_TIME, QUEUE_CAPACITY
            ));
        }
        return executorService;
    }

    public static void submit(Runnable task) {
        getInstance().submit(task);
    }

    public static void execute(Runnable task) {
        getInstance().execute(task);
    }

    public static int getActiveCount() {
        return executorService != null ? executorService.getActiveCount() : 0;
    }

    public static int getPoolSize() {
        return executorService != null ? executorService.getPoolSize() : 0;
    }

    public static int getQueueSize() {
        return executorService != null ? executorService.getQueue().size() : 0;
    }

    public static long getCompletedTaskCount() {
        return executorService != null ? executorService.getCompletedTaskCount() : 0;
    }

    public static synchronized void shutdown() {
        if (executorService != null && !executorService.isShutdown()) {
            LoggerUtil.info("ServerThreadPool", "Dang tat ServerThreadPool...");
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
            executorService = null;
            LoggerUtil.info("ServerThreadPool", "ServerThreadPool da dung hoan toan.");
        }
    }
}
