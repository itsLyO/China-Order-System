package server.service;

import common.model.*;
import common.protocol.MessageType;
import common.protocol.Packet;
import common.util.JsonUtil;
import common.util.LoggerUtil;
import server.core.ClientHandler;
import server.core.ClientSessionManager;
import server.dao.OrderDao;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Dich vu quan tri may trang thai don hang (State Machine 9 trang thai),
 * bao gia, xac nhan coc, tracking 7 chang va tu dong Server Push cap nhat thoi gian thuc.
 */
public class OrderService {
    private final OrderDao orderDao;
    private final PricingService pricingService;

    public OrderService() {
        this.orderDao = new OrderDao();
        this.pricingService = new PricingService();
    }

    public OrderService(OrderDao orderDao, PricingService pricingService) {
        this.orderDao = orderDao;
        this.pricingService = pricingService;
    }

    /**
     * Tiep nhan yeu cau tao don order moi tu Client (TICKET_CREATE_REQ)
     */
    public boolean handleCreateTicket(ClientHandler handler, Packet packet) {
        String json = packet.getPayloadAsString();
        OrderTicket ticket = JsonUtil.fromJson(json, OrderTicket.class);

        if (ticket == null || ticket.getTicketId() == null || ticket.getClientId() == null) {
            LoggerUtil.warn("OrderService", "Goi TICKET_CREATE_REQ khong hop le tu client");
            return false;
        }

        ticket.setStatus(OrderStatus.PENDING_QUOTE);
        boolean created = orderDao.createTicket(ticket);
        if (created) {
            orderDao.addTrackingLog(ticket.getTicketId(), OrderStatus.PENDING_QUOTE,
                    "Hệ thống", "Đại lý đã tạo ticket yêu cầu báo giá nguồn hàng", ticket.getClientId());
            LoggerUtil.info("OrderService", String.format(
                    "Tiep nhan ticket moi [%s] tu [%s]: %s (SL: %d)",
                    ticket.getTicketId(), ticket.getClientId(), ticket.getProductName(), ticket.getQuantity()
            ));
        }
        return created;
    }

    /**
     * Server Admin phat hanh bang bao gia & tu dong Server Push TICKET_QUOTE_RESP ve Client
     */
    public boolean issuePriceQuote(PriceQuote quote) {
        if (quote == null || quote.getTicketId() == null) return false;

        boolean saved = orderDao.savePriceQuote(quote);
        if (saved) {
            orderDao.addTrackingLog(quote.getTicketId(), OrderStatus.QUOTED,
                    "Hà Nội (Server)", "Hiếu đã phát hành bảng báo giá và mã VietQR đặt cọc", "Hiếu - Server");

            OrderTicket ticket = orderDao.getTicketById(quote.getTicketId());
            if (ticket != null) {
                // Server Push goi TICKET_QUOTE_RESP xuong Client
                Packet quotePacket = new Packet(MessageType.TICKET_QUOTE_RESP, JsonUtil.toJson(quote));
                boolean pushed = ClientSessionManager.pushToClient(ticket.getClientId(), quotePacket);
                LoggerUtil.info("OrderService", String.format(
                        "Da phat hanh bao gia cho ticket [%s]. Push ve [%s]: %s",
                        quote.getTicketId(), ticket.getClientId(), (pushed ? "THANH CONG" : "CLIENT OFFLINE")
                ));
            }
        }
        return saved;
    }

    /**
     * Tiep nhan xac nhan dat coc tu Client (TICKET_DEPOSIT_CONFIRM)
     */
    public boolean handleDepositConfirm(ClientHandler handler, Packet packet) {
        String json = packet.getPayloadAsString();
        DepositConfirmation confirm = JsonUtil.fromJson(json, DepositConfirmation.class);

        if (confirm == null || confirm.getTicketId() == null) {
            LoggerUtil.warn("OrderService", "Goi TICKET_DEPOSIT_CONFIRM khong hop le");
            return false;
        }

        boolean depositOk = orderDao.confirmDeposit(confirm.getTicketId());
        if (depositOk) {
            String note = "Đại lý xác nhận đã chuyển cọc: " + (confirm.getTransactionRef() != null ? confirm.getTransactionRef() : "Thành công");
            orderDao.addTrackingLog(confirm.getTicketId(), OrderStatus.DEPOSITED, "Việt Nam", note, confirm.getClientId());

            // Server Push thong bao don hang da chuyen sang DEPOSITED
            OrderStatusUpdate update = new OrderStatusUpdate();
            update.setTicketId(confirm.getTicketId());
            update.setOldStatus(OrderStatus.QUOTED);
            update.setNewStatus(OrderStatus.DEPOSITED);
            update.setLocation("Việt Nam");
            update.setNote("Đã xác nhận đặt cọc thành công, chuẩn bị liên hệ xưởng TQ phát hàng");
            update.setTimestamp(getCurrentTimestamp());

            Packet statusPacket = new Packet(MessageType.ORDER_STATUS_UPDATE, JsonUtil.toJson(update));
            ClientSessionManager.pushToClient(confirm.getClientId(), statusPacket);

            LoggerUtil.info("OrderService", String.format(
                    "Xac nhan coc thanh cong cho ticket [%s] tu [%s] (So tien: %.0f VND)",
                    confirm.getTicketId(), confirm.getClientId(), confirm.getAmountPaid()
            ));
        }
        return depositOk;
    }

    /**
     * Cap nhat chang hanh trinh van chuyen (7 chang logistics) va tu dong Server Push xuong Client
     */
    public boolean updateTrackingStage(String ticketId, OrderStatus newStatus, String location, String note, String updatedBy) {
        OrderTicket ticket = orderDao.getTicketById(ticketId);
        if (ticket == null) {
            LoggerUtil.warn("OrderService", "Khong tim thay ticket: " + ticketId);
            return false;
        }

        OrderStatus oldStatus = ticket.getStatus();
        boolean updated = orderDao.updateTicketStatus(ticketId, newStatus);
        if (updated) {
            orderDao.addTrackingLog(ticketId, newStatus, location, note, updatedBy);

            // Server Push thoi gian thuc xuong Client
            OrderStatusUpdate statusUpdate = new OrderStatusUpdate();
            statusUpdate.setTicketId(ticketId);
            statusUpdate.setOldStatus(oldStatus);
            statusUpdate.setNewStatus(newStatus);
            statusUpdate.setLocation(location);
            statusUpdate.setNote(note);
            statusUpdate.setTimestamp(getCurrentTimestamp());

            Packet pushPacket = new Packet(MessageType.ORDER_STATUS_UPDATE, JsonUtil.toJson(statusUpdate));
            boolean pushed = ClientSessionManager.pushToClient(ticket.getClientId(), pushPacket);

            LoggerUtil.info("OrderService", String.format(
                    "Chuyen chang ticket [%s]: %s -> %s tai '%s'. Push ve [%s]: %s",
                    ticketId, oldStatus, newStatus, location, ticket.getClientId(), (pushed ? "THANH CONG" : "CLIENT OFFLINE")
            ));
        }
        return updated;
    }

    /**
     * Tiep nhan yeu cau tai dat hang nhanh 1-Click tu Client (REORDER_FAST_REQ - Novelty 1)
     */
    public boolean handleFastReorder(ClientHandler handler, Packet packet) {
        String json = packet.getPayloadAsString();
        FastReorderRequest req = JsonUtil.fromJson(json, FastReorderRequest.class);

        if (req == null || req.getNewTicketId() == null || req.getReferenceTicketId() == null) {
            LoggerUtil.warn("OrderService", "Goi REORDER_FAST_REQ khong hop le");
            return false;
        }

        OrderTicket refTicket = orderDao.getTicketById(req.getReferenceTicketId());
        if (refTicket == null) {
            LoggerUtil.warn("OrderService", "Khong tim thay ticket goc de tai dat: " + req.getReferenceTicketId());
            return false;
        }

        OrderTicket newTicket = new OrderTicket();
        newTicket.setTicketId(req.getNewTicketId());
        newTicket.setClientId(req.getClientId());
        newTicket.setProductName(refTicket.getProductName());
        newTicket.setQuantity(req.getQuantity() > 0 ? req.getQuantity() : refTicket.getQuantity());
        newTicket.setColor(refTicket.getColor());
        newTicket.setSize(refTicket.getSize());
        newTicket.setProductLink(refTicket.getProductLink());
        newTicket.setImageUrl(refTicket.getImageUrl());
        String clientNote = "Tái đặt hàng nhanh từ vé " + req.getReferenceTicketId();
        if (req.getNote() != null && !req.getNote().isEmpty()) {
            clientNote += " - Ghi chú: " + req.getNote();
        }
        newTicket.setClientNote(clientNote);
        newTicket.setStatus(OrderStatus.PENDING_QUOTE);

        boolean created = orderDao.createTicket(newTicket);
        if (created) {
            orderDao.addTrackingLog(newTicket.getTicketId(), OrderStatus.PENDING_QUOTE,
                    "Hệ thống", "Đại lý kích hoạt tái đặt hàng nhanh 1-Click từ vé " + req.getReferenceTicketId(), req.getClientId());

            LoggerUtil.info("OrderService", String.format(
                    "TAI DAT HANG NHANH THANH CONG! Tao ticket [%s] tu goc [%s] cho [%s] (SL: %d)",
                    newTicket.getTicketId(), req.getReferenceTicketId(), req.getClientId(), newTicket.getQuantity()
            ));
        }
        return created;
    }

    public OrderTicket getTicketById(String ticketId) {
        return orderDao.getTicketById(ticketId);
    }

    public List<OrderTicket> getAllTickets() {
        return orderDao.getAllTickets();
    }

    public List<OrderStatusUpdate> getTrackingLogs(String ticketId) {
        return orderDao.getTrackingLogs(ticketId);
    }

    private String getCurrentTimestamp() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
    }
}
