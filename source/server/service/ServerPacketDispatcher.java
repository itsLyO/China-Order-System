package server.service;

import common.protocol.MessageType;
import common.protocol.Packet;
import common.util.LoggerUtil;
import server.core.ClientHandler;
import server.core.PacketDispatcher;

/**
 * Bo dieu phoi trung tam, dinh tuyen cac goi tin nhan duoc tu ClientHandler toi dung Service nghiep vu
 */
public class ServerPacketDispatcher implements PacketDispatcher {
    private final OrderService orderService;
    private final ServerChatService chatService;
    private final InventoryMonitorService inventoryService;

    public ServerPacketDispatcher() {
        this.orderService = new OrderService();
        this.chatService = new ServerChatService();
        this.inventoryService = new InventoryMonitorService();
    }

    public ServerPacketDispatcher(OrderService orderService, ServerChatService chatService, InventoryMonitorService inventoryService) {
        this.orderService = orderService;
        this.chatService = chatService;
        this.inventoryService = inventoryService;
    }

    @Override
    public void dispatch(ClientHandler handler, Packet packet) {
        MessageType type = packet.getHeader().getType();
        String clientId = handler.getClientId() != null ? handler.getClientId() : "Anonymous";

        switch (type) {
            case TICKET_CREATE_REQ:
                orderService.handleCreateTicket(handler, packet);
                break;

            case TICKET_DEPOSIT_CONFIRM:
                orderService.handleDepositConfirm(handler, packet);
                break;

            case REORDER_FAST_REQ:
                orderService.handleFastReorder(handler, packet);
                break;

            case CHAT_TEXT_MSG:
                chatService.handleTextMessage(handler, packet);
                break;

            case CHAT_IMAGE_MSG:
                chatService.handleImageMessage(handler, packet);
                break;

            case INVENTORY_SYNC:
                inventoryService.handleInventorySync(handler, packet);
                break;

            default:
                LoggerUtil.warn("ServerPacketDispatcher", String.format(
                        "Khong co Service xu ly cho goi tin loai: [%s] tu Client [%s]",
                        type.name(), clientId
                ));
                break;
        }
    }

    public OrderService getOrderService() {
        return orderService;
    }

    public ServerChatService getChatService() {
        return chatService;
    }

    public InventoryMonitorService getInventoryService() {
        return inventoryService;
    }
}
