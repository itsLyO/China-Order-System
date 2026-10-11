package server.service;

import common.model.InventoryItem;
import common.model.InventorySyncPayload;
import common.model.OrderTicket;
import common.protocol.MessageType;
import common.protocol.Packet;
import common.util.JsonUtil;
import common.util.LoggerUtil;
import server.core.ClientHandler;
import server.core.ClientSessionManager;
import server.dao.ClientInventoryDao;
import server.dao.OrderDao;

import java.util.List;

/**
 * NOVELTY 1: He thong giam sat ton kho dai ly chu dong (Proactive Inventory Restocking)
 * Tu dong phan tich ton kho, phat hien hang duoi nguong an toan va Server Push canh bao tai dat hang 1-Click.
 */
public class InventoryMonitorService {
    private final ClientInventoryDao inventoryDao;
    private final OrderDao orderDao;

    public InventoryMonitorService() {
        this.inventoryDao = new ClientInventoryDao();
        this.orderDao = new OrderDao();
    }

    public InventoryMonitorService(ClientInventoryDao inventoryDao, OrderDao orderDao) {
        this.inventoryDao = inventoryDao;
        this.orderDao = orderDao;
    }

    /**
     * DTO payload cho goi tin RESTOCK_ALERT_PUSH (0x31)
     */
    public static class RestockAlertPushPayload {
        private String clientId;
        private String productCode;
        private String productName;
        private int currentStock;
        private int minSafetyThreshold;
        private int suggestedQuantity;
        private String message;
        private boolean canFastReorder;
        private String referenceTicketId;

        public RestockAlertPushPayload() {}

        public String getClientId() { return clientId; }
        public void setClientId(String clientId) { this.clientId = clientId; }
        public String getProductCode() { return productCode; }
        public void setProductCode(String productCode) { this.productCode = productCode; }
        public String getProductName() { return productName; }
        public void setProductName(String productName) { this.productName = productName; }
        public int getCurrentStock() { return currentStock; }
        public void setCurrentStock(int currentStock) { this.currentStock = currentStock; }
        public int getMinSafetyThreshold() { return minSafetyThreshold; }
        public void setMinSafetyThreshold(int minSafetyThreshold) { this.minSafetyThreshold = minSafetyThreshold; }
        public int getSuggestedQuantity() { return suggestedQuantity; }
        public void setSuggestedQuantity(int suggestedQuantity) { this.suggestedQuantity = suggestedQuantity; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public boolean isCanFastReorder() { return canFastReorder; }
        public void setCanFastReorder(boolean canFastReorder) { this.canFastReorder = canFastReorder; }
        public String getReferenceTicketId() { return referenceTicketId; }
        public void setReferenceTicketId(String referenceTicketId) { this.referenceTicketId = referenceTicketId; }
    }

    /**
     * Tiep nhan goi dong bo ton kho tu Client (INVENTORY_SYNC - 0x30)
     */
    public boolean handleInventorySync(ClientHandler handler, Packet packet) {
        String json = packet.getPayloadAsString();
        InventorySyncPayload payload = JsonUtil.fromJson(json, InventorySyncPayload.class);

        if (payload == null || payload.getClientId() == null || payload.getItems() == null) {
            LoggerUtil.warn("InventoryMonitorService", "Goi INVENTORY_SYNC khong hop le tu client");
            return false;
        }

        boolean synced = inventoryDao.syncInventory(payload.getClientId(), payload.getItems());
        if (synced) {
            LoggerUtil.info("InventoryMonitorService", String.format(
                    "Dong bo thanh cong %d mat hang kho cho client [%s]",
                    payload.getItems().size(), payload.getClientId()
            ));

            // Kich hoat ngay bo phan tich ton kho de Push canh bao neu co mat hang sap het
            scanAndAlertClient(payload.getClientId());
        }
        return synced;
    }

    /**
     * Quet danh muc kho cua mot Client va Server Push canh bao cac mat hang duoi nguong an toan
     */
    public int scanAndAlertClient(String clientId) {
        List<InventoryItem> lowItems = inventoryDao.getLowStockItems(clientId);
        if (lowItems.isEmpty()) {
            return 0;
        }

        List<OrderTicket> clientTickets = orderDao.getTicketsByClient(clientId);
        int alertsSent = 0;

        for (InventoryItem item : lowItems) {
            // Tim ticket cu phu hop lam ticket tham chieu cho 1-Click Reorder
            String refTicketId = null;
            for (OrderTicket t : clientTickets) {
                if (t.getProductName() != null && t.getProductName().contains(item.getProductName())) {
                    refTicketId = t.getTicketId();
                    break;
                }
            }
            if (refTicketId == null && !clientTickets.isEmpty()) {
                refTicketId = clientTickets.get(0).getTicketId();
            }

            int suggestedQty = Math.max(50, item.getSoldQuantity() > 0 ? (int) (item.getSoldQuantity() * 1.2) : 100);

            RestockAlertPushPayload alert = new RestockAlertPushPayload();
            alert.setClientId(clientId);
            alert.setProductCode(item.getProductCode());
            alert.setProductName(item.getProductName());
            alert.setCurrentStock(item.getCurrentStock());
            alert.setMinSafetyThreshold(item.getMinSafetyThreshold());
            alert.setSuggestedQuantity(suggestedQty);
            alert.setMessage(String.format(
                    "Mặt hàng '%s' chỉ còn %d sp (ngưỡng an toàn: %d). Xưởng đang có sẵn lô mới giá ưu đãi, bạn có muốn nhập thêm %d sp không?",
                    item.getProductName(), item.getCurrentStock(), item.getMinSafetyThreshold(), suggestedQty
            ));
            alert.setCanFastReorder(refTicketId != null);
            alert.setReferenceTicketId(refTicketId);

            // Server Push chu dong goi tin RESTOCK_ALERT_PUSH xuong Client
            Packet alertPacket = new Packet(MessageType.RESTOCK_ALERT_PUSH, JsonUtil.toJson(alert));
            boolean pushed = ClientSessionManager.pushToClient(clientId, alertPacket);

            LoggerUtil.info("InventoryMonitorService", String.format(
                    "[NOVELTY 1 PUSH] Gui canh bao ton kho thap [%s - %s] ve [%s]: %s",
                    item.getProductCode(), item.getProductName(), clientId, (pushed ? "THANH CONG" : "CLIENT OFFLINE")
            ));
            alertsSent++;
        }
        return alertsSent;
    }

    /**
     * Quet toan bo he thong va thuc hien canh bao cho tat ca cac dai ly online
     */
    public int scanAllAndAlert() {
        int totalAlerts = 0;
        for (String clientId : ClientSessionManager.getAllOnlineClientIds()) {
            totalAlerts += scanAndAlertClient(clientId);
        }
        return totalAlerts;
    }
}
