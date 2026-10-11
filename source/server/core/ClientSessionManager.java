package server.core;

import common.model.UserRole;
import common.protocol.Packet;
import common.util.LoggerUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bo quan ly phien lam viec tap trung cua cac Client dang online (Thread-safe)
 * Ho tro Server Push 2 chieu va Broadcast thong bao thoi gian thuc.
 */
public class ClientSessionManager {
    private static final ConcurrentHashMap<String, ClientHandler> onlineSessions = new ConcurrentHashMap<>();

    /**
     * Dang ky phien lam viec moi khi Client dang nhap thanh cong
     */
    public static void registerSession(String clientId, ClientHandler handler) {
        if (clientId == null || handler == null) return;

        // Neu da co phien cu cung clientId, dong phien cu de tranh xung dot
        ClientHandler oldHandler = onlineSessions.put(clientId, handler);
        if (oldHandler != null && oldHandler != handler) {
            LoggerUtil.warn("ClientSessionManager", "Phat hien phien ket noi cu cua: " + clientId + ", dong phien cu.");
            oldHandler.close();
        }
        LoggerUtil.info("ClientSessionManager", String.format("Client [%s] da dang ky phien online. Tong online: %d", clientId, onlineSessions.size()));
    }

    /**
     * Huy dang ky phien lam viec khi Client ngat ket noi
     */
    public static void unregisterSession(String clientId) {
        if (clientId != null) {
            ClientHandler removed = onlineSessions.remove(clientId);
            if (removed != null) {
                LoggerUtil.info("ClientSessionManager", String.format("Client [%s] da roi mang. Tong online: %d", clientId, onlineSessions.size()));
            }
        }
    }

    /**
     * Huy dang ky chi khi dung phien handler tuong ung (tranh race-condition khi reconnect)
     */
    public static void unregisterSession(String clientId, ClientHandler handler) {
        if (clientId != null && handler != null) {
            boolean removed = onlineSessions.remove(clientId, handler);
            if (removed) {
                LoggerUtil.info("ClientSessionManager", String.format("Client [%s] da ngat ket noi. Tong online: %d", clientId, onlineSessions.size()));
            }
        }
    }

    /**
     * Lay ClientHandler theo clientId
     */
    public static ClientHandler getSession(String clientId) {
        return clientId != null ? onlineSessions.get(clientId) : null;
    }

    /**
     * Kiem tra Client co dang online khong
     */
    public static boolean isClientOnline(String clientId) {
        ClientHandler handler = getSession(clientId);
        return handler != null && handler.isAlive();
    }

    /**
     * Server Push chu dong day goi tin xuong 1 Client cu the
     */
    public static boolean pushToClient(String clientId, Packet packet) {
        ClientHandler handler = getSession(clientId);
        if (handler != null && handler.isAlive()) {
            try {
                handler.sendPacket(packet);
                return true;
            } catch (IOException e) {
                LoggerUtil.error("ClientSessionManager", "Loi khi Push goi tin toi: " + clientId, e);
                unregisterSession(clientId, handler);
            }
        }
        return false;
    }

    /**
     * Phat thong bao (Broadcast) goi tin toi tat ca Client dang online
     */
    public static int broadcast(Packet packet) {
        int count = 0;
        for (ClientHandler handler : onlineSessions.values()) {
            if (handler != null && handler.isAlive()) {
                try {
                    handler.sendPacket(packet);
                    count++;
                } catch (IOException e) {
                    LoggerUtil.error("ClientSessionManager", "Loi khi broadcast toi: " + handler.getClientId(), e);
                }
            }
        }
        return count;
    }

    /**
     * Phat thong bao toi cac Client co cung UserRole (VD: chi gui toi cac dai ly CLIENT_RETAILER)
     */
    public static int broadcastToRole(UserRole role, Packet packet) {
        int count = 0;
        for (ClientHandler handler : onlineSessions.values()) {
            if (handler != null && handler.isAlive() && handler.getUserRole() == role) {
                try {
                    handler.sendPacket(packet);
                    count++;
                } catch (IOException e) {
                    LoggerUtil.error("ClientSessionManager", "Loi khi broadcast to role: " + role, e);
                }
            }
        }
        return count;
    }

    /**
     * Lay so luong Client dang truc tuyen
     */
    public static int getOnlineCount() {
        return onlineSessions.size();
    }

    /**
     * Lay danh sach toan bo Client ID dang truc tuyen
     */
    public static Set<String> getAllOnlineClientIds() {
        return onlineSessions.keySet();
    }

    /**
     * Lay toan bo map phien online (phuc vu giam sat he thong)
     */
    public static ConcurrentHashMap<String, ClientHandler> getAllSessions() {
        return onlineSessions;
    }

    /**
     * Quet va dong cac phien bat dong qua han timeout
     */
    public static List<String> cleanInactiveSessions(long timeoutMillis) {
        List<String> deadClients = new ArrayList<>();
        long now = System.currentTimeMillis();

        for (ClientHandler handler : onlineSessions.values()) {
            if (handler != null) {
                long idleTime = now - handler.getLastActivityTime();
                if (idleTime > timeoutMillis) {
                    String cId = handler.getClientId();
                    LoggerUtil.warn("ClientSessionManager", String.format(
                            "Thu hoi session khong phan hoi: [%s] (Bat dong %d ms > %d ms)",
                            cId, idleTime, timeoutMillis
                    ));
                    deadClients.add(cId);
                    handler.close();
                }
            }
        }
        return deadClients;
    }

    /**
     * Dong toan bo phien khi Server dung hoat dong
     */
    public static void closeAllSessions() {
        LoggerUtil.info("ClientSessionManager", "Dang dong toan bo cac phien Client ket noi...");
        for (ClientHandler handler : onlineSessions.values()) {
            if (handler != null) {
                handler.close();
            }
        }
        onlineSessions.clear();
        LoggerUtil.info("ClientSessionManager", "Da dong tat ca cac phien Client.");
    }
}
