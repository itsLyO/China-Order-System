package server.core;

import common.model.AuthRequest;
import common.model.AuthResponse;
import common.model.User;
import common.model.UserRole;
import common.protocol.*;
import common.util.JsonUtil;
import common.util.LoggerUtil;
import server.dao.UserDao;

import java.io.*;
import java.net.Socket;
import java.net.SocketAddress;

/**
 * Bo xu ly phien lam viec rieng biet cho tung Client ket noi qua Socket TCP (Worker Runnable)
 */
public class ClientHandler implements Runnable {
    private static PacketDispatcher globalDispatcher;

    private final Socket socket;
    private final DataInputStream in;
    private final DataOutputStream out;
    private final UserDao userDao;

    private String clientId;
    private String fullName;
    private UserRole userRole;
    private long loginTime;
    private volatile long lastActivityTime;
    private volatile boolean isRunning = true;

    public ClientHandler(Socket socket) throws IOException {
        this.socket = socket;
        this.in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
        this.out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
        this.userDao = new UserDao();
        this.lastActivityTime = System.currentTimeMillis();
    }

    public static void setGlobalDispatcher(PacketDispatcher dispatcher) {
        globalDispatcher = dispatcher;
    }

    public static PacketDispatcher getGlobalDispatcher() {
        return globalDispatcher;
    }

    @Override
    public void run() {
        LoggerUtil.info("ClientHandler", "Client ket noi tu: " + socket.getRemoteSocketAddress());
        try {
            while (isRunning && !socket.isClosed()) {
                Packet packet = ProtocolFraming.readPacket(in);
                this.lastActivityTime = System.currentTimeMillis();
                handlePacket(packet);
            }
        } catch (EOFException | ProtocolException e) {
            LoggerUtil.warn("ClientHandler", String.format("Client [%s] da ngat ket noi (EOF)", (clientId != null ? clientId : socket.getRemoteSocketAddress())));
        } catch (IOException e) {
            if (isRunning) {
                LoggerUtil.error("ClientHandler", "Loi doc/ghi socket cua client: " + (clientId != null ? clientId : "Unknown"), e);
            }
        } finally {
            close();
        }
    }

    private void handlePacket(Packet packet) throws IOException {
        MessageType type = packet.getHeader().getType();

        // 1. Xu ly Heartbeat PING -> PONG
        if (type == MessageType.PING) {
            sendPacket(new Packet(MessageType.PONG, new byte[0]));
            return;
        }

        // 2. Xu ly Dang nhap AUTH_LOGIN_REQ
        if (type == MessageType.AUTH_LOGIN_REQ) {
            handleLogin(packet);
            return;
        }

        // 3. Cac loai goi tin nghiep vu khac duoc chuyen tiep toi PacketDispatcher
        if (globalDispatcher != null) {
            globalDispatcher.dispatch(this, packet);
        } else {
            LoggerUtil.info("ClientHandler", String.format(
                    "Nhan goi tin [%s] tu Client [%s] (Chua gan PacketDispatcher)",
                    type.name(), (clientId != null ? clientId : "Anonymous")
            ));
        }
    }

    private void handleLogin(Packet packet) throws IOException {
        String jsonPayload = packet.getPayloadAsString();
        AuthRequest req = JsonUtil.fromJson(jsonPayload, AuthRequest.class);

        if (req == null || req.getUsername() == null || req.getPassword() == null) {
            AuthResponse resp = new AuthResponse(false, null, null, null, "Du lieu dang nhap khong hop le");
            sendPacket(new Packet(MessageType.AUTH_LOGIN_RESP, JsonUtil.toJson(resp)));
            return;
        }

        User user = userDao.authenticate(req.getUsername(), req.getPassword());
        if (user != null) {
            this.clientId = user.getUserId();
            this.fullName = user.getFullName();
            this.userRole = user.getRole();
            this.loginTime = System.currentTimeMillis();

            // Dang ky vao ClientSessionManager
            ClientSessionManager.registerSession(this.clientId, this);

            AuthResponse resp = new AuthResponse(true, user.getUserId(), user.getFullName(), user.getRole(), "Dang nhap thanh cong");
            sendPacket(new Packet(MessageType.AUTH_LOGIN_RESP, JsonUtil.toJson(resp)));
            LoggerUtil.info("ClientHandler", String.format("Xac thuc thanh cong: %s (%s) vai tro %s", user.getUsername(), user.getUserId(), user.getRole()));
        } else {
            AuthResponse resp = new AuthResponse(false, null, null, null, "Sai ten dang nhap hoac mat khau");
            sendPacket(new Packet(MessageType.AUTH_LOGIN_RESP, JsonUtil.toJson(resp)));
            LoggerUtil.warn("ClientHandler", "Dang nhap that bai voi tai khoan: " + req.getUsername());
        }
    }

    /**
     * Gui goi tin xuong socket mot cach dong bo va thread-safe
     */
    public synchronized void sendPacket(Packet packet) throws IOException {
        if (!isAlive()) {
            throw new IOException("Socket da bi dong hoac ngat ket noi");
        }
        ProtocolFraming.writePacket(out, packet);
    }

    public boolean isAlive() {
        return isRunning && socket.isConnected() && !socket.isClosed();
    }

    public void close() {
        isRunning = false;
        if (clientId != null) {
            ClientSessionManager.unregisterSession(clientId, this);
        }
        try {
            if (!socket.isClosed()) {
                socket.close();
            }
        } catch (IOException ignored) {}
    }

    // Getters
    public String getClientId() { return clientId; }
    public String getFullName() { return fullName; }
    public UserRole getUserRole() { return userRole; }
    public long getLoginTime() { return loginTime; }
    public long getLastActivityTime() { return lastActivityTime; }
    public SocketAddress getRemoteAddress() { return socket.getRemoteSocketAddress(); }
}
