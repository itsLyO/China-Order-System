package server.dao;

import common.model.OrderStatus;
import common.model.OrderStatusUpdate;
import common.model.OrderTicket;
import common.model.PriceQuote;
import common.util.LoggerUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object cho Ticket Order, Bao gia va Nhat ky van chuyen
 */
public class OrderDao {

    /**
     * Tao ticket order moi tu Client
     */
    public boolean createTicket(OrderTicket ticket) {
        String sql = "INSERT INTO order_tickets (ticket_id, client_id, product_name, quantity, color, size, " +
                "product_link, image_url, client_note, status, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, datetime('now', 'localtime'), datetime('now', 'localtime'));";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, ticket.getTicketId());
            ps.setString(2, ticket.getClientId());
            ps.setString(3, ticket.getProductName());
            ps.setInt(4, ticket.getQuantity());
            ps.setString(5, ticket.getColor());
            ps.setString(6, ticket.getSize());
            ps.setString(7, ticket.getProductLink());
            ps.setString(8, ticket.getImageUrl());
            ps.setString(9, ticket.getClientNote());
            ps.setString(10, ticket.getStatus() != null ? ticket.getStatus().name() : OrderStatus.PENDING_QUOTE.name());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LoggerUtil.error("OrderDao", "Loi tao ticket order: " + ticket.getTicketId(), e);
            return false;
        }
    }

    /**
     * Lay ticket theo ID
     */
    public OrderTicket getTicketById(String ticketId) {
        String sql = "SELECT * FROM order_tickets WHERE ticket_id = ?;";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, ticketId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToTicket(rs);
                }
            }
        } catch (SQLException e) {
            LoggerUtil.error("OrderDao", "Loi lay ticket theo ID: " + ticketId, e);
        }
        return null;
    }

    /**
     * Lay toan bo danh sach ticket (phuc vu bang quan tri cua Server)
     */
    public List<OrderTicket> getAllTickets() {
        List<OrderTicket> list = new ArrayList<>();
        String sql = "SELECT * FROM order_tickets ORDER BY created_at DESC;";
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapRowToTicket(rs));
            }
        } catch (SQLException e) {
            LoggerUtil.error("OrderDao", "Loi lay toan bo danh sach ticket", e);
        }
        return list;
    }

    /**
     * Lay danh sach ticket theo Client ID
     */
    public List<OrderTicket> getTicketsByClient(String clientId) {
        List<OrderTicket> list = new ArrayList<>();
        String sql = "SELECT * FROM order_tickets WHERE client_id = ? ORDER BY created_at DESC;";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, clientId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToTicket(rs));
                }
            }
        } catch (SQLException e) {
            LoggerUtil.error("OrderDao", "Loi lay ticket theo Client: " + clientId, e);
        }
        return list;
    }

    /**
     * Cap nhat trang thai don hang
     */
    public boolean updateTicketStatus(String ticketId, OrderStatus status) {
        String sql = "UPDATE order_tickets SET status = ?, updated_at = datetime('now', 'localtime') WHERE ticket_id = ?;";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setString(2, ticketId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LoggerUtil.error("OrderDao", "Loi cap nhat trang thai ticket: " + ticketId, e);
            return false;
        }
    }

    /**
     * Luu hoac cap nhat bao gia va thong tin coc
     */
    public boolean savePriceQuote(PriceQuote quote) {
        String sql = "INSERT INTO price_quotes (quote_id, ticket_id, unit_price_cny, exchange_rate, domestic_shipping_cny, " +
                "intl_shipping_vnd, total_estimated_vnd, deposit_required_vnd, qr_payload, deposit_confirmed, quoted_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 0, datetime('now', 'localtime')) " +
                "ON CONFLICT(ticket_id) DO UPDATE SET " +
                "unit_price_cny = excluded.unit_price_cny, " +
                "exchange_rate = excluded.exchange_rate, " +
                "domestic_shipping_cny = excluded.domestic_shipping_cny, " +
                "intl_shipping_vnd = excluded.intl_shipping_vnd, " +
                "total_estimated_vnd = excluded.total_estimated_vnd, " +
                "deposit_required_vnd = excluded.deposit_required_vnd, " +
                "qr_payload = excluded.qr_payload, " +
                "quoted_at = datetime('now', 'localtime');";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, quote.getQuoteId());
            ps.setString(2, quote.getTicketId());
            ps.setDouble(3, quote.getUnitPriceCny());
            ps.setDouble(4, quote.getExchangeRate());
            ps.setDouble(5, quote.getDomesticShippingCny());
            ps.setDouble(6, quote.getIntlShippingVnd());
            ps.setDouble(7, quote.getTotalEstimatedVnd());
            ps.setDouble(8, quote.getDepositRequiredVnd());
            ps.setString(9, quote.getQrPayload());
            boolean success = ps.executeUpdate() > 0;
            if (success) {
                updateTicketStatus(quote.getTicketId(), OrderStatus.QUOTED);
            }
            return success;
        } catch (SQLException e) {
            LoggerUtil.error("OrderDao", "Loi luu bao gia cho ticket: " + quote.getTicketId(), e);
            return false;
        }
    }

    /**
     * Lay bao gia theo Ticket ID
     */
    public PriceQuote getPriceQuoteByTicketId(String ticketId) {
        String sql = "SELECT * FROM price_quotes WHERE ticket_id = ?;";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, ticketId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    PriceQuote quote = new PriceQuote();
                    quote.setQuoteId(rs.getString("quote_id"));
                    quote.setTicketId(rs.getString("ticket_id"));
                    quote.setUnitPriceCny(rs.getDouble("unit_price_cny"));
                    quote.setExchangeRate(rs.getDouble("exchange_rate"));
                    quote.setDomesticShippingCny(rs.getDouble("domestic_shipping_cny"));
                    quote.setIntlShippingVnd(rs.getDouble("intl_shipping_vnd"));
                    quote.setTotalEstimatedVnd(rs.getDouble("total_estimated_vnd"));
                    quote.setDepositRequiredVnd(rs.getDouble("deposit_required_vnd"));
                    quote.setQrPayload(rs.getString("qr_payload"));
                    return quote;
                }
            }
        } catch (SQLException e) {
            LoggerUtil.error("OrderDao", "Loi lay bao gia theo ticket ID: " + ticketId, e);
        }
        return null;
    }

    /**
     * Xac nhan da nhan coc
     */
    public boolean confirmDeposit(String ticketId) {
        String sql = "UPDATE price_quotes SET deposit_confirmed = 1 WHERE ticket_id = ?;";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, ticketId);
            boolean success = ps.executeUpdate() > 0;
            if (success) {
                updateTicketStatus(ticketId, OrderStatus.DEPOSITED);
            }
            return success;
        } catch (SQLException e) {
            LoggerUtil.error("OrderDao", "Loi xac nhan coc ticket: " + ticketId, e);
            return false;
        }
    }

    /**
     * Ghi nhan nhat ky hanh trinh van chuyen (Tracking Log)
     */
    public boolean addTrackingLog(String ticketId, OrderStatus status, String location, String note, String updatedBy) {
        String sql = "INSERT INTO order_tracking_logs (ticket_id, status, location, note, updated_by, timestamp) " +
                "VALUES (?, ?, ?, ?, ?, datetime('now', 'localtime'));";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, ticketId);
            ps.setString(2, status.name());
            ps.setString(3, location);
            ps.setString(4, note);
            ps.setString(5, updatedBy);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LoggerUtil.error("OrderDao", "Loi them nhat ky van chuyen: " + ticketId, e);
            return false;
        }
    }

    /**
     * Lay toan bo nhat ky hanh trinh cua 1 ticket
     */
    public List<OrderStatusUpdate> getTrackingLogs(String ticketId) {
        List<OrderStatusUpdate> logs = new ArrayList<>();
        String sql = "SELECT * FROM order_tracking_logs WHERE ticket_id = ? ORDER BY timestamp ASC;";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, ticketId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    OrderStatusUpdate update = new OrderStatusUpdate();
                    update.setTicketId(rs.getString("ticket_id"));
                    update.setNewStatus(OrderStatus.valueOf(rs.getString("status")));
                    update.setLocation(rs.getString("location"));
                    update.setNote(rs.getString("note"));
                    update.setTimestamp(rs.getString("timestamp"));
                    logs.add(update);
                }
            }
        } catch (SQLException e) {
            LoggerUtil.error("OrderDao", "Loi lay nhat ky van chuyen: " + ticketId, e);
        }
        return logs;
    }

    private OrderTicket mapRowToTicket(ResultSet rs) throws SQLException {
        OrderTicket ticket = new OrderTicket();
        ticket.setTicketId(rs.getString("ticket_id"));
        ticket.setClientId(rs.getString("client_id"));
        ticket.setProductName(rs.getString("product_name"));
        ticket.setQuantity(rs.getInt("quantity"));
        ticket.setColor(rs.getString("color"));
        ticket.setSize(rs.getString("size"));
        ticket.setProductLink(rs.getString("product_link"));
        ticket.setImageUrl(rs.getString("image_url"));
        ticket.setClientNote(rs.getString("client_note"));
        ticket.setStatus(OrderStatus.valueOf(rs.getString("status")));
        ticket.setCreatedAt(rs.getString("created_at"));
        return ticket;
    }
}
