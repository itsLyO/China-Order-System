package server.core;

import common.protocol.Packet;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

public class ClientSessionManager {
    private static final ConcurrentHashMap<String, ClientHandler> onlineSessions = new ConcurrentHashMap<>();

    public static void registerSession(String clientId, ClientHandler handler) {
        onlineSessions.put(clientId, handler);
    }

    public static void unregisterSession(String clientId) {
        if (clientId != null) onlineSessions.remove(clientId);
    }

    public static ClientHandler getSession(String clientId) {
        return onlineSessions.get(clientId);
    }

    public static boolean pushToClient(String clientId, Packet packet) {
        ClientHandler handler = onlineSessions.get(clientId);
        if (handler != null && handler.isAlive()) {
            try {
                handler.sendPacket(packet);
                return true;
            } catch (IOException e) {
                unregisterSession(clientId);
            }
        }
        return false;
    }

    public static ConcurrentHashMap<String, ClientHandler> getAllSessions() {
        return onlineSessions;
    }
}
