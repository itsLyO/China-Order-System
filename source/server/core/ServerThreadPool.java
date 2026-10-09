package server.core;

import java.util.concurrent.*;

public class ServerThreadPool {
    private static final int CORE_POOL_SIZE = 10;
    private static final int MAX_POOL_SIZE = 200;
    private static final long KEEP_ALIVE_TIME = 60L;

    private static ExecutorService executorService;

    public static synchronized ExecutorService getInstance() {
        if (executorService == null) {
            executorService = new ThreadPoolExecutor(
                    CORE_POOL_SIZE,
                    MAX_POOL_SIZE,
                    KEEP_ALIVE_TIME,
                    TimeUnit.SECONDS,
                    new LinkedBlockingQueue<>(500),
                    new ThreadFactory() {
                        private int counter = 0;
                        @Override
                        public Thread newThread(Runnable r) {
                            return new Thread(r, "ClientWorker-" + (++counter));
                        }
                    }
            );
        }
        return executorService;
    }
}
