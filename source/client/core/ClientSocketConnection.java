package client.core;

import common.protocol.*;
import common.util.LoggerUtil;

import java.io.*;
import java.net.Socket;

public class ClientSocketConnection {
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private PacketListenerThread listenerThread;
    private volatile boolean connected = false;

    public synchronized void connect(String host, int port) throws IOException {
        LoggerUtil.info("ClientSocket", "Dang ket noi den Server: " + host + ":" + port);
        socket = new Socket(host, port);
        in = new DataInputStream(socket.getInputStream());
        out = new DataOutputStream(socket.getOutputStream());
        connected = true;

        listenerThread = new PacketListenerThread(in, this);
        listenerThread.start();
        LoggerUtil.info("ClientSocket", "Ket noi thanh cong!");
    }

    public synchronized void sendPacket(Packet packet) throws IOException {
        if (!connected || socket == null || socket.isClosed()) {
            throw new IOException("Chua ket noi den Server!");
        }
        ProtocolFraming.writePacket(out, packet);
    }

    public boolean isConnected() {
        return connected && socket != null && !socket.isClosed();
    }

    public void close() {
        connected = false;
        try {
            if (socket != null) socket.close();
        } catch (IOException ignored) {}
    }
}
