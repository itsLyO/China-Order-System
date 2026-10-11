package server.dao;

import common.model.InventoryItem;
import common.model.RestockAlert;
import common.util.LoggerUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object cho he thong Dong bo ton kho Dai ly va Canh bao chu dong (Novelty 1)
 */
public class ClientInventoryDao {

    /**
     * Dong bo toan bo danh muc kho tu Client len Server (ho tro Upsert neu mat hang da ton tai)
     */
    public boolean syncInventory(String clientId, List<InventoryItem> items) {
        if (items == null || items.isEmpty()) {
            return true;
        }

        String sql = "INSERT INTO client_inventories (client_id, product_code, product_name, current_stock, sold_quantity, min_safety_threshold, last_synced_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, datetime('now', 'localtime')) " +
                "ON CONFLICT(client_id, product_code) DO UPDATE SET " +
                "  product_name = excluded.product_name, " +
                "  current_stock = excluded.current_stock, " +
                "  sold_quantity = excluded.sold_quantity, " +
                "  min_safety_threshold = excluded.min_safety_threshold, " +
                "  last_synced_at = datetime('now', 'localtime');";

        Connection conn = null;
        boolean originalAutoCommit = true;
        try {
            conn = DatabaseManager.getConnection();
            originalAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (InventoryItem item : items) {
                    ps.setString(1, clientId);
                    ps.setString(2, item.getProductCode());
                    ps.setString(3, item.getProductName());
                    ps.setInt(4, item.getCurrentStock());
                    ps.setInt(5, item.getSoldQuantity());
                    ps.setInt(6, item.getMinSafetyThreshold() > 0 ? item.getMinSafetyThreshold() : 10);
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            LoggerUtil.error("ClientInventoryDao", "Loi dong bo kho cua client: " + clientId, e);
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ignored) {}
            }
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(originalAutoCommit);
                } catch (SQLException ignored) {}
            }
        }
    }

    /**
     * Lay toan bo danh sach ton kho cua mot dai ly
     */
    public List<InventoryItem> getInventoryByClient(String clientId) {
        List<InventoryItem> list = new ArrayList<>();
        String sql = "SELECT product_code, product_name, current_stock, sold_quantity, min_safety_threshold " +
                "FROM client_inventories WHERE client_id = ? ORDER BY product_code ASC;";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, clientId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToItem(rs));
                }
            }
        } catch (SQLException e) {
            LoggerUtil.error("ClientInventoryDao", "Loi lay danh muc kho cua client: " + clientId, e);
        }
        return list;
    }

    /**
     * Lay cac mat hang co ton kho <= nguong an toan cua mot dai ly
     */
    public List<InventoryItem> getLowStockItems(String clientId) {
        List<InventoryItem> list = new ArrayList<>();
        String sql = "SELECT product_code, product_name, current_stock, sold_quantity, min_safety_threshold " +
                "FROM client_inventories " +
                "WHERE client_id = ? AND current_stock <= min_safety_threshold " +
                "ORDER BY current_stock ASC;";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, clientId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToItem(rs));
                }
            }
        } catch (SQLException e) {
            LoggerUtil.error("ClientInventoryDao", "Loi lay mat hang sap het cua client: " + clientId, e);
        }
        return list;
    }

    /**
     * Quet toan bo he thong va tra ve danh sach canh bao sap het hang cho tat ca dai ly (Novelty 1)
     */
    public List<RestockAlert> getAllLowStockAlerts() {
        List<RestockAlert> alerts = new ArrayList<>();
        String sql = "SELECT ci.client_id, ci.product_code, ci.product_name, ci.current_stock, ci.min_safety_threshold, u.full_name " +
                "FROM client_inventories ci " +
                "LEFT JOIN users u ON ci.client_id = u.user_id " +
                "WHERE ci.current_stock <= ci.min_safety_threshold " +
                "ORDER BY ci.client_id ASC, ci.current_stock ASC;";
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                String clientId = rs.getString("client_id");
                String productCode = rs.getString("product_code");
                String productName = rs.getString("product_name");
                int currentStock = rs.getInt("current_stock");
                int threshold = rs.getInt("min_safety_threshold");
                String clientName = rs.getString("full_name");
                if (clientName == null) clientName = clientId;

                String message = String.format("Mặt hàng '%s' (%s) của %s chỉ còn %d sp (ngưỡng an toàn: %d). Đề xuất đặt thêm ngay!",
                        productName, productCode, clientName, currentStock, threshold);

                RestockAlert alert = new RestockAlert(clientId, productCode, productName, currentStock, message);
                alert.setSuggestedQuantity("50 - 100");
                alerts.add(alert);
            }
        } catch (SQLException e) {
            LoggerUtil.error("ClientInventoryDao", "Loi quet danh sach canh bao ton kho thap toan he thong", e);
        }
        return alerts;
    }

    /**
     * Lay chi tiet 1 mat hang cua dai ly
     */
    public InventoryItem getItemByCode(String clientId, String productCode) {
        String sql = "SELECT product_code, product_name, current_stock, sold_quantity, min_safety_threshold " +
                "FROM client_inventories WHERE client_id = ? AND product_code = ?;";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, clientId);
            ps.setString(2, productCode);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToItem(rs);
                }
            }
        } catch (SQLException e) {
            LoggerUtil.error("ClientInventoryDao", "Loi lay mat hang: " + productCode, e);
        }
        return null;
    }

    private InventoryItem mapRowToItem(ResultSet rs) throws SQLException {
        InventoryItem item = new InventoryItem();
        item.setProductCode(rs.getString("product_code"));
        item.setProductName(rs.getString("product_name"));
        item.setCurrentStock(rs.getInt("current_stock"));
        item.setSoldQuantity(rs.getInt("sold_quantity"));
        item.setMinSafetyThreshold(rs.getInt("min_safety_threshold"));
        return item;
    }
}
