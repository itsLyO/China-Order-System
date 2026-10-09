package server.core;

import common.util.LoggerUtil;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class OrderServer {
    public static final int DEFAULT_PORT = 8888;
    private volatile boolean running = true;

    public void start(int port) {
        LoggerUtil.info("OrderServer", "Khoi dong Order Server tren port " + port + "...");
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            LoggerUtil.info("OrderServer", "Server san sang lang nghe ket noi!");
            while (running) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(clientSocket);
                ServerThreadPool.getInstance().submit(handler);
            }
        } catch (IOException e) {
            LoggerUtil.error("OrderServer", "Loi khoi dong ServerSocket", e);
        }
    }

    public static void main(String[] args) {
        new OrderServer().start(DEFAULT_PORT);
    }
}
