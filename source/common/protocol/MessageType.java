package common.protocol;

/**
 * Cac loai ma lenh trong giao thuc mang Custom TCP Framing
 */
public enum MessageType {
    // Authentication
    AUTH_LOGIN_REQ((byte) 0x01),
    AUTH_LOGIN_RESP((byte) 0x02),

    // Real-time Chat & Images
    CHAT_TEXT_MSG((byte) 0x10),
    CHAT_IMAGE_MSG((byte) 0x11),

    // Order & Ticketing
    TICKET_CREATE_REQ((byte) 0x20),
    TICKET_QUOTE_RESP((byte) 0x21),
    TICKET_DEPOSIT_CONFIRM((byte) 0x22),
    ORDER_STATUS_UPDATE((byte) 0x23),

    // Inventory & Restocking (Novelty 1)
    INVENTORY_SYNC((byte) 0x30),
    RESTOCK_ALERT_PUSH((byte) 0x31),
    REORDER_FAST_REQ((byte) 0x40),

    // Heartbeat Keep-Alive
    PING((byte) 0x90),
    PONG((byte) 0x91);

    private final byte code;

    MessageType(byte code) {
        this.code = code;
    }

    public byte getCode() {
        return code;
    }

    public static MessageType fromCode(byte code) {
        for (MessageType type : values()) {
            if (type.code == code) return type;
        }
        return null;
    }
}
