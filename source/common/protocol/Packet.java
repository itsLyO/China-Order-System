package common.protocol;

import java.nio.charset.StandardCharsets;

/**
 * Goi tin mang hoan chinh gom PacketHeader va Payload byte array
 */
public class Packet {
    private PacketHeader header;
    private byte[] payload;

    public Packet(MessageType type, byte[] payload) {
        this.header = new PacketHeader(type, payload != null ? payload.length : 0);
        this.payload = payload != null ? payload : new byte[0];
    }

    public Packet(MessageType type, String jsonString) {
        this(type, jsonString != null ? jsonString.getBytes(StandardCharsets.UTF_8) : new byte[0]);
    }

    public PacketHeader getHeader() { return header; }
    public byte[] getPayload() { return payload; }

    public String getPayloadAsString() {
        return new String(payload, StandardCharsets.UTF_8);
    }
}
