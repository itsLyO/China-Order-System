package server.core;

import common.protocol.*;
import common.util.LoggerUtil;

import java.io.*;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final DataInputStream in;
    private final DataOutputStream out;
    private String clientId;
    private volatile boolean isRunning = true;

    public ClientHandler(Socket socket) throws IOException {
        this.socket = socket;
        this.in = new DataInputStream(socket.getInputStream());
        this.out = new DataOutputStream(socket.getOutputStream());
    }

    @Override
    public void run() {
        LoggerUtil.info("ClientHandler", "Client ket noi tu: " + socket.getRemoteSocketAddress());
        try {
            while (isRunning && !socket.isClosed()) {
                Packet packet = ProtocolFraming.readPacket(in);
                handlePacket(packet);
            }
        } catch (EOFException | ProtocolException e) {
            LoggerUtil.warn("ClientHandler", "Client da ngat ket noi: " + clientId);
        } catch (IOException e) {
            LoggerUtil.error("ClientHandler", "Loi doc ghi socket", e);
        } finally {
            close();
        }
    }

    private void handlePacket(Packet packet) throws IOException {
        MessageType type = packet.getHeader().getType();
        if (type == MessageType.PING) {
            sendPacket(new Packet(MessageType.PONG, new byte[0]));
        } else {
            LoggerUtil.info("ClientHandler", "Nhan goi tin loai: " + type.name());
        }
    }

    public synchronized void sendPacket(Packet packet) throws IOException {
        ProtocolFraming.writePacket(out, packet);
    }

    public boolean isAlive() {
        return isRunning && socket.isConnected() && !socket.isClosed();
    }

    public void close() {
        isRunning = false;
        if (clientId != null) {
            ClientSessionManager.unregisterSession(clientId);
        }
        try {
            socket.close();
        } catch (IOException ignored) {}
    }
}
