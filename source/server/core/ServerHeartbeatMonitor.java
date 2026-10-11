package server.core;

import common.util.LoggerUtil;

import java.util.List;
import java.util.concurrent.*;

/**
 * Luong giam sat va quet timeout ket noi Socket (Dead connection cleaner)
 * Tu dong thu hoi tai nguyen va dong cac socket client ngung tuong tac qua han.
 */
public class ServerHeartbeatMonitor {
    private static final long DEFAULT_CHECK_INTERVAL_SECONDS = 15L;
    private static final long DEFAULT_TIMEOUT_MILLIS = 60000L; // 60 giay

    private ScheduledExecutorService scheduler;
    private long timeoutMillis;
    private volatile boolean running = false;

    public ServerHeartbeatMonitor() {
        this(DEFAULT_TIMEOUT_MILLIS);
    }

    public ServerHeartbeatMonitor(long timeoutMillis) {
        this.timeoutMillis = timeoutMillis;
    }

    public synchronized void start() {
        if (running) return;

        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "HeartbeatMonitor-Thread");
            t.setDaemon(true);
            return t;
        });

        scheduler.scheduleAtFixedRate(this::checkAndCleanSessions,
                DEFAULT_CHECK_INTERVAL_SECONDS,
                DEFAULT_CHECK_INTERVAL_SECONDS,
                TimeUnit.SECONDS);

        running = true;
        LoggerUtil.info("ServerHeartbeatMonitor", String.format(
                "Khoi dong Heartbeat Monitor (Quet moi %ds, Timeout nguong %ds)",
                DEFAULT_CHECK_INTERVAL_SECONDS, timeoutMillis / 1000
        ));
    }

    private void checkAndCleanSessions() {
        try {
            List<String> cleaned = ClientSessionManager.cleanInactiveSessions(timeoutMillis);
            if (!cleaned.isEmpty()) {
                LoggerUtil.warn("ServerHeartbeatMonitor", String.format(
                        "Da tu dong ngat ket noi %d client khong phan hoi: %s",
                        cleaned.size(), cleaned
                ));
            }
        } catch (Exception e) {
            LoggerUtil.error("ServerHeartbeatMonitor", "Loi trong qua trinh quet session heartbeat", e);
        }
    }

    public synchronized void stop() {
        if (!running) return;
        running = false;
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
            scheduler = null;
        }
        LoggerUtil.info("ServerHeartbeatMonitor", "Da dung ServerHeartbeatMonitor.");
    }

    public boolean isRunning() {
        return running;
    }

    public void setTimeoutMillis(long timeoutMillis) {
        this.timeoutMillis = timeoutMillis;
    }
}
