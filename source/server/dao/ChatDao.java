package server.dao;

import common.model.ChatMessage;
import common.util.LoggerUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object cho he thong Chat trao doi & dam phan don hang giua Server va Client
 */
public class ChatDao {

    /**
     * Luu tin nhan chat (van ban hoac metadata anh)
     */
    public boolean saveMessage(ChatMessage message) {
        return saveMessage(message, null);
    }

    /**
     * Luu tin nhan chat co kem theo checksum cua anh (dung cho truyen nhan hinh anh)
     */
    public boolean saveMessage(ChatMessage message, String imageChecksum) {
        String sql = "INSERT INTO chat_messages (message_id, sender_id, receiver_id, content_type, content, image_checksum, timestamp) " +
                "VALUES (?, ?, ?, ?, ?, ?, COALESCE(?, datetime('now', 'localtime')));";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, message.getMessageId());
            ps.setString(2, message.getSenderId());
            ps.setString(3, message.getReceiverId());
            ps.setString(4, message.getContentType() != null ? message.getContentType() : "TEXT");
            ps.setString(5, message.getContent());
            ps.setString(6, imageChecksum);
            ps.setString(7, message.getTimestamp());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LoggerUtil.error("ChatDao", "Loi luu tin nhan chat: " + message.getMessageId(), e);
            return false;
        }
    }

    /**
     * Lay lich su tro chuyen giua 2 nguoi dung theo thu tu thoi gian tang dan
     */
    public List<ChatMessage> getConversation(String user1, String user2, int limit) {
        List<ChatMessage> list = new ArrayList<>();
        String sql = "SELECT * FROM ( " +
                "  SELECT message_id, sender_id, receiver_id, content_type, content, timestamp " +
                "  FROM chat_messages " +
                "  WHERE (sender_id = ? AND receiver_id = ?) OR (sender_id = ? AND receiver_id = ?) " +
                "  ORDER BY timestamp DESC LIMIT ? " +
                ") ORDER BY timestamp ASC;";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user1);
            ps.setString(2, user2);
            ps.setString(3, user2);
            ps.setString(4, user1);
            ps.setInt(5, limit > 0 ? limit : 100);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ChatMessage msg = new ChatMessage();
                    msg.setMessageId(rs.getString("message_id"));
                    msg.setSenderId(rs.getString("sender_id"));
                    msg.setReceiverId(rs.getString("receiver_id"));
                    msg.setContentType(rs.getString("content_type"));
                    msg.setContent(rs.getString("content"));
                    msg.setTimestamp(rs.getString("timestamp"));
                    list.add(msg);
                }
            }
        } catch (SQLException e) {
            LoggerUtil.error("ChatDao", "Loi lay lich su tro chuyen giua " + user1 + " va " + user2, e);
        }
        return list;
    }

    /**
     * Lay danh sach cac ID doi tac da tung chat voi userId
     */
    public List<String> getRecentChatPartners(String userId) {
        List<String> partners = new ArrayList<>();
        String sql = "SELECT DISTINCT partner_id FROM ( " +
                "  SELECT CASE WHEN sender_id = ? THEN receiver_id ELSE sender_id END AS partner_id, timestamp " +
                "  FROM chat_messages " +
                "  WHERE sender_id = ? OR receiver_id = ? " +
                "  ORDER BY timestamp DESC " +
                ");";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userId);
            ps.setString(2, userId);
            ps.setString(3, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    partners.add(rs.getString("partner_id"));
                }
            }
        } catch (SQLException e) {
            LoggerUtil.error("ChatDao", "Loi lay danh sach doi tac chat cua: " + userId, e);
        }
        return partners;
    }
}
