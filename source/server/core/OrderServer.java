package server.core;

import common.util.LoggerUtil;
import server.dao.DatabaseManager;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

/**
 * Lop khoi dong va quan ly may chu trung tam Server Node (Order & Logistics Server)
 */
public class OrderServer {
    public static final int DEFAULT_PORT = 8888;

    private ServerSocket serverSocket;
    private ServerHeartbeatMonitor heartbeatMonitor;
    private volatile boolean running = false;
    private int boundPort;

    public OrderServer() {
        this.heartbeatMonitor = new ServerHeartbeatMonitor();
    }

    /**
     * Khoi dong Server dong bo (chan luong hien tai de lang nghe accept)
     */
    public void start(int port) {
        if (running) return;

        LoggerUtil.info("OrderServer", "=== KHOI DONG HE THONG SERVER NODE (PORT " + port + ") ===");

        // 1. Khoi tao CSDL SQLite & Schema
        DatabaseManager.initialize();

        // 2. Khoi tao ThreadPool
        ServerThreadPool.getInstance();

        // 3. Khoi dong Heartbeat Monitor
        heartbeatMonitor.start();

        // 4. Dang ky Shutdown Hook de tat an toan khi nhan Ctrl+C
        Runtime.getRuntime().addShutdownHook(new Thread(this::stop, "ServerShutdownHook"));

        // 5. Mo ServerSocket va lang nghe ket noi
        try {
            serverSocket = new ServerSocket(port);
            boundPort = serverSocket.getLocalPort();
            running = true;
            LoggerUtil.info("OrderServer", "Server da san sang lang nghe ket noi tai port: " + boundPort);

            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    ClientHandler handler = new ClientHandler(clientSocket);
                    ServerThreadPool.submit(handler);
                } catch (SocketException se) {
                    if (!running) {
                        LoggerUtil.info("OrderServer", "ServerSocket da dong theo yeu cau.");
                        break;
                    }
                    LoggerUtil.error("OrderServer", "Loi Socket tren luong accept", se);
                }
            }
        } catch (IOException e) {
            LoggerUtil.error("OrderServer", "Khong the khoi dong ServerSocket tai port " + port, e);
        } finally {
            stop();
        }
    }

    /**
     * Khoi dong Server bat dong bo tren 1 luong rieng (phuc vu kiem thu hoac tich hop UI)
     */
    public Thread startAsync(int port) {
        Thread serverThread = new Thread(() -> start(port), "OrderServer-Listener");
        serverThread.setDaemon(true);
        serverThread.start();

        // Doi cho toi khi ServerSocket thuc su bind thanh cong
        long startWait = System.currentTimeMillis();
        while (!running && System.currentTimeMillis() - startWait < 5000) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException ignored) {}
        }
        return serverThread;
    }

    /**
     * Dung toan bo he thong Server va giai phong tat ca tai nguyen
     */
    public synchronized void stop() {
        if (!running && serverSocket == null) return;
        running = false;

        LoggerUtil.info("OrderServer", "Dang tien hanh dung OrderServer...");

        // 1. Dong ServerSocket
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException ignored) {}
            serverSocket = null;
        }

        // 2. Dong toan bo Client Session
        ClientSessionManager.closeAllSessions();

        // 3. Dung Heartbeat Monitor
        if (heartbeatMonitor != null) {
            heartbeatMonitor.stop();
        }

        // 4. Tat ServerThreadPool
        ServerThreadPool.shutdown();

        // 5. Dong CSDL SQLite
        DatabaseManager.closeConnection();

        LoggerUtil.info("OrderServer", "OrderServer da dung hoan toan va giai phong 100% tai nguyen.");
    }

    public boolean isRunning() {
        return running;
    }

    public int getBoundPort() {
        return boundPort;
    }

    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException ignored) {}
        }
        new OrderServer().start(port);
    }
}
