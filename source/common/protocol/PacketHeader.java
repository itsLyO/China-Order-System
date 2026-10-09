package common.protocol;

/**
 * Tieu de goi tin co dinh 8 bytes:
 * [Magic (2B)][Version (1B)][Type (1B)][Payload Length (4B)]
 */
public class PacketHeader {
    public static final byte MAGIC_1 = 0x43; // 'C'
    public static final byte MAGIC_2 = 0x4E; // 'N'
    public static final byte CURRENT_VERSION = 0x01;
    public static final int HEADER_SIZE = 8;

    private byte version;
    private MessageType type;
    private int payloadLength;

    public PacketHeader(MessageType type, int payloadLength) {
        this(CURRENT_VERSION, type, payloadLength);
    }

    public PacketHeader(byte version, MessageType type, int payloadLength) {
        this.version = version;
        this.type = type;
        this.payloadLength = payloadLength;
    }

    public byte getVersion() { return version; }
    public MessageType getType() { return type; }
    public int getPayloadLength() { return payloadLength; }
}
