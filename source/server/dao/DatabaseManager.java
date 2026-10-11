package server.dao;

import common.model.*;
import common.util.LoggerUtil;

import java.io.File;
import java.sql.*;

/**
 * Bo dieu phoi CSDL tap trung cho Server (SQLite)
 * Tu dong khoi tao thu muc, tao bang va nap du lieu mau ban dau (Seed Data).
 */
public class DatabaseManager {
    private static final String DB_DIR = "data";
    private static final String DB_FILE = "data/server_storage.db";
    private static final String JDBC_URL = "jdbc:sqlite:" + DB_FILE;

    private static Connection connection;

    /**
     * Lay ket noi duy nhat toi CSDL SQLite (Auto-reconnect neu connection bi dong)
     */
    public static synchronized Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            ensureDirectoryExists();
            try {
                Class.forName("org.sqlite.JDBC");
            } catch (ClassNotFoundException e) {
                throw new SQLException("Khong tim thay SQLite JDBC Driver trong lib!", e);
            }
            connection = DriverManager.getConnection(JDBC_URL);
            // Bat foreign keys cho SQLite
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("PRAGMA foreign_keys = ON;");
            }
        }
        return connection;
    }

    private static void ensureDirectoryExists() {
        File dir = new File(DB_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    /**
     * Khoi tao CSDL: Tao cac bang neu chua co va nap du lieu mau
     */
    public static synchronized void initialize() {
        LoggerUtil.info("DatabaseManager", "Khoi tao co so du lieu SQLite tai: " + DB_FILE);
        try {
            Connection conn = getConnection();
            createTablesIfNotExist(conn);
            seedInitialDataIfEmpty(conn);
            LoggerUtil.info("DatabaseManager", "Khoi tao co so du lieu thanh cong 100%!");
        } catch (SQLException e) {
            LoggerUtil.error("DatabaseManager", "Loi trong qua trinh khoi tao co so du lieu", e);
        }
    }

    private static void createTablesIfNotExist(Connection conn) throws SQLException {
        String[] ddlStatements = {
                // 1. Bang users
                "CREATE TABLE IF NOT EXISTS users (" +
                        "user_id TEXT PRIMARY KEY, " +
                        "username TEXT UNIQUE NOT NULL, " +
                        "password_hash TEXT NOT NULL, " +
                        "full_name TEXT NOT NULL, " +
                        "role TEXT NOT NULL, " +
                        "phone TEXT, " +
                        "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP);",

                // 2. Bang order_tickets
                "CREATE TABLE IF NOT EXISTS order_tickets (" +
                        "ticket_id TEXT PRIMARY KEY, " +
                        "client_id TEXT NOT NULL, " +
                        "product_name TEXT NOT NULL, " +
                        "quantity INTEGER NOT NULL, " +
                        "color TEXT, " +
                        "size TEXT, " +
                        "product_link TEXT, " +
                        "image_url TEXT, " +
                        "client_note TEXT, " +
                        "status TEXT NOT NULL, " +
                        "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                        "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                        "FOREIGN KEY (client_id) REFERENCES users(user_id));",

                // 3. Bang price_quotes
                "CREATE TABLE IF NOT EXISTS price_quotes (" +
                        "quote_id TEXT PRIMARY KEY, " +
                        "ticket_id TEXT UNIQUE NOT NULL, " +
                        "unit_price_cny REAL NOT NULL, " +
                        "exchange_rate REAL NOT NULL, " +
                        "domestic_shipping_cny REAL DEFAULT 0.0, " +
                        "intl_shipping_vnd REAL NOT NULL, " +
                        "total_estimated_vnd REAL NOT NULL, " +
                        "deposit_required_vnd REAL NOT NULL, " +
                        "qr_payload TEXT, " +
                        "deposit_confirmed INTEGER DEFAULT 0, " +
                        "quoted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                        "FOREIGN KEY (ticket_id) REFERENCES order_tickets(ticket_id));",

                // 4. Bang order_tracking_logs
                "CREATE TABLE IF NOT EXISTS order_tracking_logs (" +
                        "log_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "ticket_id TEXT NOT NULL, " +
                        "status TEXT NOT NULL, " +
                        "location TEXT, " +
                        "note TEXT, " +
                        "updated_by TEXT NOT NULL, " +
                        "timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                        "FOREIGN KEY (ticket_id) REFERENCES order_tickets(ticket_id));",

                // 5. Bang chat_messages
                "CREATE TABLE IF NOT EXISTS chat_messages (" +
                        "message_id TEXT PRIMARY KEY, " +
                        "sender_id TEXT NOT NULL, " +
                        "receiver_id TEXT NOT NULL, " +
                        "content_type TEXT NOT NULL, " +
                        "content TEXT NOT NULL, " +
                        "image_checksum TEXT, " +
                        "timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP);",

                // 6. Bang client_inventories
                "CREATE TABLE IF NOT EXISTS client_inventories (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "client_id TEXT NOT NULL, " +
                        "product_code TEXT NOT NULL, " +
                        "product_name TEXT NOT NULL, " +
                        "current_stock INTEGER NOT NULL, " +
                        "sold_quantity INTEGER NOT NULL, " +
                        "min_safety_threshold INTEGER DEFAULT 10, " +
                        "last_synced_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                        "UNIQUE(client_id, product_code), " +
                        "FOREIGN KEY (client_id) REFERENCES users(user_id));"
        };

        try (Statement stmt = conn.createStatement()) {
            for (String sql : ddlStatements) {
                stmt.execute(sql);
            }
        }
    }

    private static void seedInitialDataIfEmpty(Connection conn) throws SQLException {
        // Kiem tra bang users
        try (Statement checkStmt = conn.createStatement();
             ResultSet rs = checkStmt.executeQuery("SELECT COUNT(*) FROM users;")) {
            if (rs.next() && rs.getInt(1) > 0) {
                return; // Da co du lieu, khong can seed
            }
        }

        LoggerUtil.info("DatabaseManager", "Phat hien CSDL trong, dang nap du lieu mau ban dau (Seed Data)...");

        // 1. Seed Users (Admin Hieu + Dai ly Quang + Dai ly Hoang Mai)
        String insertUserSql = "INSERT INTO users (user_id, username, password_hash, full_name, role, phone) VALUES (?, ?, ?, ?, ?, ?);";
        try (PreparedStatement ps = conn.prepareStatement(insertUserSql)) {
            // Admin Hieu
            ps.setString(1, "USR_ADMIN");
            ps.setString(2, "admin");
            ps.setString(3, "admin123");
            ps.setString(4, "Hiếu - Chuyên Order & Logistics TQ");
            ps.setString(5, UserRole.SERVER_ADMIN.name());
            ps.setString(6, "0901234567");
            ps.executeUpdate();

            // Client Quang
            ps.setString(1, "USR_QUANG");
            ps.setString(2, "daily_quang");
            ps.setString(3, "123456");
            ps.setString(4, "Đại lý Quang - Hà Nội");
            ps.setString(5, UserRole.CLIENT_RETAILER.name());
            ps.setString(6, "0912345678");
            ps.executeUpdate();

            // Client Hoang Mai
            ps.setString(1, "USR_HOANGMAI");
            ps.setString(2, "daily_hoangmai");
            ps.setString(3, "123456");
            ps.setString(4, "Đại lý Hoàng Mai");
            ps.setString(5, UserRole.CLIENT_RETAILER.name());
            ps.setString(6, "0987654321");
            ps.executeUpdate();
        }

        // 2. Seed Order Tickets
        String insertTicketSql = "INSERT INTO order_tickets (ticket_id, client_id, product_name, quantity, color, size, product_link, image_url, client_note, status, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";
        try (PreparedStatement ps = conn.prepareStatement(insertTicketSql)) {
            // Ticket 1: Dang o kho bien gioi (Da bao gia, da coc)
            ps.setString(1, "TCK_20261009_001");
            ps.setString(2, "USR_QUANG");
            ps.setString(3, "Áo phao đại hàn lông vũ");
            ps.setInt(4, 100);
            ps.setString(5, "Đỏ đô, Đen");
            ps.setString(6, "L, XL");
            ps.setString(7, "https://detail.1688.com/offer/7123456.html");
            ps.setString(8, "ao_phao_mau_do.jpg");
            ps.setString(9, "Cần hàng về trước ngày 20/11 để kịp bán đợt rét đầu mùa");
            ps.setString(10, OrderStatus.WAREHOUSE_BORDER_CN.name());
            ps.setString(11, "2026-10-09 14:00:00");
            ps.executeUpdate();

            // Ticket 2: Cho bao gia
            ps.setString(1, "TCK_20261010_002");
            ps.setString(2, "USR_QUANG");
            ps.setString(3, "Giày thể thao sneaker Quảng Châu");
            ps.setInt(4, 50);
            ps.setString(5, "Trắng ngà");
            ps.setString(6, "40, 41, 42");
            ps.setString(7, "https://detail.1688.com/offer/8992144.html");
            ps.setString(8, "giay_the_thao.jpg");
            ps.setString(9, "Deal giá tốt giúp tôi số lượng 50 đôi nhé");
            ps.setString(10, OrderStatus.PENDING_QUOTE.name());
            ps.setString(11, "2026-10-10 09:30:00");
            ps.executeUpdate();

            // Ticket 3: Hoan tat
            ps.setString(1, "TCK_20261005_003");
            ps.setString(2, "USR_HOANGMAI");
            ps.setString(3, "Túi xách da nữ cao cấp");
            ps.setInt(4, 200);
            ps.setString(5, "Nâu tây, Đen");
            ps.setString(6, "Tiêu chuẩn");
            ps.setString(7, "https://detail.1688.com/offer/6543210.html");
            ps.setString(8, "tui_xach.jpg");
            ps.setString(9, "Đã nhận hàng thành công, chất da rất đẹp");
            ps.setString(10, OrderStatus.COMPLETED.name());
            ps.setString(11, "2026-10-05 10:15:00");
            ps.executeUpdate();
        }

        // 3. Seed Price Quotes
        String insertQuoteSql = "INSERT INTO price_quotes (quote_id, ticket_id, unit_price_cny, exchange_rate, domestic_shipping_cny, intl_shipping_vnd, total_estimated_vnd, deposit_required_vnd, qr_payload, deposit_confirmed, quoted_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";
        try (PreparedStatement ps = conn.prepareStatement(insertQuoteSql)) {
            ps.setString(1, "QTE_20261009_001");
            ps.setString(2, "TCK_20261009_001");
            ps.setDouble(3, 45.0);
            ps.setDouble(4, 3650.0);
            ps.setDouble(5, 50.0);
            ps.setDouble(6, 1200000.0);
            ps.setDouble(7, 17807500.0);
            ps.setDouble(8, 5000000.0);
            ps.setString(9, "https://img.vietqr.io/image/970422-0987654321-compact2.png?amount=5000000&addInfo=COC%20TCK_20261009_001");
            ps.setInt(10, 1);
            ps.setString(11, "2026-10-09 15:00:00");
            ps.executeUpdate();
        }

        // 4. Seed Tracking Logs
        String insertLogSql = "INSERT INTO order_tracking_logs (ticket_id, status, location, note, updated_by, timestamp) VALUES (?, ?, ?, ?, ?, ?);";
        try (PreparedStatement ps = conn.prepareStatement(insertLogSql)) {
            ps.setString(1, "TCK_20261009_001");
            ps.setString(2, OrderStatus.ORDERED_CN.name());
            ps.setString(3, "Quảng Châu (Trung Quốc)");
            ps.setString(4, "Đã thanh toán cho nhà xưởng, đang đóng gói");
            ps.setString(5, "Hiếu - Server");
            ps.setString(6, "2026-10-09 17:00:00");
            ps.executeUpdate();

            ps.setString(1, "TCK_20261009_001");
            ps.setString(2, OrderStatus.DOMESTIC_SHIPPING_CN.name());
            ps.setString(3, "Trung tâm trung chuyển Nam Ninh (TQ)");
            ps.setString(4, "Hàng đang di chuyển trên đường cao tốc TQ");
            ps.setString(5, "Hiếu - Server");
            ps.setString(6, "2026-10-10 14:00:00");
            ps.executeUpdate();

            ps.setString(1, "TCK_20261009_001");
            ps.setString(2, OrderStatus.WAREHOUSE_BORDER_CN.name());
            ps.setString(3, "Kho Bằng Tường / Đông Hưng (Biên giới TQ)");
            ps.setString(4, "Hàng đã tập kết tại kho biên giới, chuẩn bị thủ tục thông quan");
            ps.setString(5, "Hiếu - Server");
            ps.setString(6, "2026-10-11 09:00:00");
            ps.executeUpdate();
        }

        // 5. Seed Client Inventories (Co mat hang ton thap duoi nguong an toan de test Novelty 1)
        String insertInvSql = "INSERT INTO client_inventories (client_id, product_code, product_name, current_stock, sold_quantity, min_safety_threshold, last_synced_at) VALUES (?, ?, ?, ?, ?, ?, ?);";
        try (PreparedStatement ps = conn.prepareStatement(insertInvSql)) {
            // Mat hang SAP HET: stock = 8 < threshold = 15 -> Se duoc trigger canh bao chu dong
            ps.setString(1, "USR_QUANG");
            ps.setString(2, "SP_AP01");
            ps.setString(3, "Áo phao đại hàn lông vũ");
            ps.setInt(4, 8);
            ps.setInt(5, 92);
            ps.setInt(6, 15);
            ps.setString(7, "2026-10-11 10:00:00");
            ps.executeUpdate();

            // Mat hang con nhieu
            ps.setString(1, "USR_QUANG");
            ps.setString(2, "SP_GTT02");
            ps.setString(3, "Giày thể thao Quảng Châu");
            ps.setInt(4, 45);
            ps.setInt(5, 15);
            ps.setInt(6, 10);
            ps.setString(7, "2026-10-11 10:00:00");
            ps.executeUpdate();

            // Mat hang cua dai ly Hoang Mai
            ps.setString(1, "USR_HOANGMAI");
            ps.setString(2, "SP_TX03");
            ps.setString(3, "Túi xách da nữ cao cấp");
            ps.setInt(4, 120);
            ps.setInt(5, 80);
            ps.setInt(6, 20);
            ps.setString(7, "2026-10-11 09:00:00");
            ps.executeUpdate();
        }

        // 6. Seed Chat Messages
        String insertChatSql = "INSERT INTO chat_messages (message_id, sender_id, receiver_id, content_type, content, timestamp) VALUES (?, ?, ?, ?, ?, ?);";
        try (PreparedStatement ps = conn.prepareStatement(insertChatSql)) {
            ps.setString(1, "MSG_001");
            ps.setString(2, "USR_QUANG");
            ps.setString(3, "USR_ADMIN");
            ps.setString(4, "TEXT");
            ps.setString(5, "Chào Hiếu, đợt này bên xưởng có mẫu áo phao mới nào ấm và nhẹ không?");
            ps.setString(6, "2026-10-09 13:45:00");
            ps.executeUpdate();

            ps.setString(1, "MSG_002");
            ps.setString(2, "USR_ADMIN");
            ps.setString(3, "USR_QUANG");
            ps.setString(4, "TEXT");
            ps.setString(5, "Có bạn ơi! Xưởng bên Hàng Châu vừa ra mẫu lông vũ 90% siêu nhẹ, giá tầm 45 tệ.");
            ps.setString(6, "2026-10-09 13:48:00");
            ps.executeUpdate();
        }

        LoggerUtil.info("DatabaseManager", "Nap du lieu mau ban dau hoan tat!");
    }

    public static synchronized void closeConnection() {
        if (connection != null) {
            try {
                if (!connection.isClosed()) {
                    connection.close();
                }
            } catch (SQLException ignored) {}
            connection = null;
        }
    }
}
