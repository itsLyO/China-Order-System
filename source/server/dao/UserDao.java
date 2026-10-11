package server.dao;

import common.model.User;
import common.model.UserRole;
import common.util.LoggerUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object cho tai khoan nguoi dung (Admin va Dai ly)
 */
public class UserDao {

    /**
     * Xac thuc dang nhap bang username va password
     */
    public User authenticate(String username, String password) {
        String sql = "SELECT user_id, username, full_name, role, phone FROM users WHERE username = ? AND password_hash = ?;";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToUser(rs);
                }
            }
        } catch (SQLException e) {
            LoggerUtil.error("UserDao", "Loi xac thuc nguoi dung: " + username, e);
        }
        return null;
    }

    /**
     * Lay thong tin user theo ID
     */
    public User getUserById(String userId) {
        String sql = "SELECT user_id, username, full_name, role, phone FROM users WHERE user_id = ?;";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToUser(rs);
                }
            }
        } catch (SQLException e) {
            LoggerUtil.error("UserDao", "Loi tim nguoi dung theo ID: " + userId, e);
        }
        return null;
    }

    /**
     * Lay danh sach tat ca cac Dai ly ban le (CLIENT_RETAILER)
     */
    public List<User> getAllRetailers() {
        List<User> retailers = new ArrayList<>();
        String sql = "SELECT user_id, username, full_name, role, phone FROM users WHERE role = ? ORDER BY created_at ASC;";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, UserRole.CLIENT_RETAILER.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    retailers.add(mapRowToUser(rs));
                }
            }
        } catch (SQLException e) {
            LoggerUtil.error("UserDao", "Loi lay danh sach dai ly", e);
        }
        return retailers;
    }

    /**
     * Tao moi tai khoan
     */
    public boolean createUser(User user, String password) {
        String sql = "INSERT INTO users (user_id, username, password_hash, full_name, role, phone) VALUES (?, ?, ?, ?, ?, ?);";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getUserId());
            ps.setString(2, user.getUsername());
            ps.setString(3, password);
            ps.setString(4, user.getFullName());
            ps.setString(5, user.getRole().name());
            ps.setString(6, user.getPhone());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LoggerUtil.error("UserDao", "Loi them nguoi dung: " + user.getUsername(), e);
            return false;
        }
    }

    private User mapRowToUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setUserId(rs.getString("user_id"));
        user.setUsername(rs.getString("username"));
        user.setFullName(rs.getString("full_name"));
        String roleStr = rs.getString("role");
        user.setRole(UserRole.valueOf(roleStr));
        user.setPhone(rs.getString("phone"));
        return user;
    }
}
