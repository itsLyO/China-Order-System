package server.service;

import common.model.ChatMessage;
import common.protocol.MessageType;
import common.protocol.Packet;
import common.util.ChecksumUtil;
import common.util.ImageChunkUtil;
import common.util.JsonUtil;
import common.util.LoggerUtil;
import server.core.ClientHandler;
import server.core.ClientSessionManager;
import server.dao.ChatDao;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Dich vu dam phan & tro chuyen 1-1 giua Server Admin va cac Dai ly
 * Xu ly tin nhan van ban, truyen nhan hinh anh Base64, xac thuc Checksum MD5 va luu tru file anh.
 */
public class ServerChatService {
    private static final String UPLOAD_DIR = "data/uploads";
    private final ChatDao chatDao;

    public ServerChatService() {
        this.chatDao = new ChatDao();
        ensureUploadDirExists();
    }

    public ServerChatService(ChatDao chatDao) {
        this.chatDao = chatDao;
        ensureUploadDirExists();
    }

    private void ensureUploadDirExists() {
        File dir = new File(UPLOAD_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    /**
     * Cau truc DTO payload cho tin nhan hinh anh (CHAT_IMAGE_MSG - 0x11)
     */
    public static class ChatImagePayload {
        private String messageId;
        private String senderId;
        private String receiverId;
        private String fileName;
        private long fileSize;
        private String md5Checksum;
        private String base64Data;
        private String timestamp;

        public ChatImagePayload() {}

        public String getMessageId() { return messageId; }
        public void setMessageId(String messageId) { this.messageId = messageId; }
        public String getSenderId() { return senderId; }
        public void setSenderId(String senderId) { this.senderId = senderId; }
        public String getReceiverId() { return receiverId; }
        public void setReceiverId(String receiverId) { this.receiverId = receiverId; }
        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }
        public long getFileSize() { return fileSize; }
        public void setFileSize(long fileSize) { this.fileSize = fileSize; }
        public String getMd5Checksum() { return md5Checksum; }
        public void setMd5Checksum(String md5Checksum) { this.md5Checksum = md5Checksum; }
        public String getBase64Data() { return base64Data; }
        public void setBase64Data(String base64Data) { this.base64Data = base64Data; }
        public String getTimestamp() { return timestamp; }
        public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    }

    /**
     * Tiep nhan va xu ly tin nhan van ban (CHAT_TEXT_MSG)
     */
    public boolean handleTextMessage(ClientHandler handler, Packet packet) {
        String json = packet.getPayloadAsString();
        ChatMessage msg = JsonUtil.fromJson(json, ChatMessage.class);

        if (msg == null || msg.getContent() == null) {
            LoggerUtil.warn("ServerChatService", "Goi CHAT_TEXT_MSG khong hop le");
            return false;
        }

        if (msg.getMessageId() == null) {
            msg.setMessageId("MSG_" + System.currentTimeMillis());
        }
        if (msg.getTimestamp() == null) {
            msg.setTimestamp(getCurrentTimestamp());
        }

        boolean saved = chatDao.saveMessage(msg);
        if (saved) {
            LoggerUtil.info("ServerChatService", String.format(
                    "Luu tin nhan tu [%s] toi [%s]: %s",
                    msg.getSenderId(), msg.getReceiverId(), msg.getContent()
            ));

            // Neu nguoi nhan la mot Client dai ly dang online, Server Push goi tin xuong
            if (!"SERVER_ADMIN".equalsIgnoreCase(msg.getReceiverId()) && !"USR_ADMIN".equalsIgnoreCase(msg.getReceiverId())) {
                ClientSessionManager.pushToClient(msg.getReceiverId(), packet);
            }
        }
        return saved;
    }

    /**
     * Tiep nhan va xu ly tin nhan hinh anh mau san pham (CHAT_IMAGE_MSG)
     * Giai ma Base64, xac thuc Checksum MD5 va luu file vat ly vao data/uploads/
     */
    public boolean handleImageMessage(ClientHandler handler, Packet packet) {
        String json = packet.getPayloadAsString();
        ChatImagePayload payload = JsonUtil.fromJson(json, ChatImagePayload.class);

        if (payload == null || payload.getBase64Data() == null || payload.getMd5Checksum() == null) {
            LoggerUtil.warn("ServerChatService", "Goi CHAT_IMAGE_MSG thieu du lieu Base64 hoac Checksum");
            return false;
        }

        byte[] imageBytes = ImageChunkUtil.fromBase64(payload.getBase64Data());
        if (imageBytes.length == 0) {
            LoggerUtil.warn("ServerChatService", "Giai ma Base64 file anh bi rong");
            return false;
        }

        // Kiem tra tinh toan ven file bang MD5 Checksum
        String calculatedMd5 = ChecksumUtil.md5Hex(imageBytes);
        if (!calculatedMd5.equalsIgnoreCase(payload.getMd5Checksum())) {
            LoggerUtil.error("ServerChatService", String.format(
                    "XAC THUC ANH THAT BAI: Checksum khong khop! Nhan: %s != Tinh: %s",
                    payload.getMd5Checksum(), calculatedMd5
            ), null);
            return false;
        }

        // Luu file vat ly vao thu muc uploads
        ensureUploadDirExists();
        String safeFileName = (payload.getMessageId() != null ? payload.getMessageId() + "_" : "") +
                (payload.getFileName() != null ? payload.getFileName() : "sample.jpg");
        File destFile = new File(UPLOAD_DIR, safeFileName);

        try (FileOutputStream fos = new FileOutputStream(destFile)) {
            fos.write(imageBytes);
            fos.flush();
        } catch (IOException e) {
            LoggerUtil.error("ServerChatService", "Loi luu file anh vao o dia: " + destFile.getPath(), e);
            return false;
        }

        // Luu metadata tin nhan vao CSDL
        ChatMessage msg = new ChatMessage();
        msg.setMessageId(payload.getMessageId() != null ? payload.getMessageId() : "MSG_IMG_" + System.currentTimeMillis());
        msg.setSenderId(payload.getSenderId());
        msg.setReceiverId(payload.getReceiverId());
        msg.setContentType("IMAGE");
        msg.setContent(destFile.getPath().replace("\\", "/"));
        msg.setTimestamp(payload.getTimestamp() != null ? payload.getTimestamp() : getCurrentTimestamp());

        boolean saved = chatDao.saveMessage(msg, calculatedMd5);
        if (saved) {
            LoggerUtil.info("ServerChatService", String.format(
                    "Luu anh mau thanh cong [%s]: %d bytes, MD5=%s -> %s",
                    safeFileName, imageBytes.length, calculatedMd5, destFile.getPath()
            ));

            // Neu gui toi Client dai ly, chuyen tiep goi tin
            if (!"SERVER_ADMIN".equalsIgnoreCase(msg.getReceiverId()) && !"USR_ADMIN".equalsIgnoreCase(msg.getReceiverId())) {
                ClientSessionManager.pushToClient(msg.getReceiverId(), packet);
            }
        }
        return saved;
    }

    /**
     * Server Admin chu dong gui tin nhan xuong Client
     */
    public boolean sendTextMessageFromAdmin(String receiverClientId, String content) {
        ChatMessage msg = new ChatMessage();
        msg.setMessageId("MSG_" + System.currentTimeMillis());
        msg.setSenderId("USR_ADMIN");
        msg.setReceiverId(receiverClientId);
        msg.setContentType("TEXT");
        msg.setContent(content);
        msg.setTimestamp(getCurrentTimestamp());

        boolean saved = chatDao.saveMessage(msg);
        if (saved) {
            Packet packet = new Packet(MessageType.CHAT_TEXT_MSG, JsonUtil.toJson(msg));
            ClientSessionManager.pushToClient(receiverClientId, packet);
        }
        return saved;
    }

    public List<ChatMessage> getConversation(String user1, String user2, int limit) {
        return chatDao.getConversation(user1, user2, limit);
    }

    private String getCurrentTimestamp() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
    }
}
