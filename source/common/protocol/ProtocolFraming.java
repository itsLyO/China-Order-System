package common.protocol;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Bo ma hoa va giai ma goi tin tren socket, giai quyet van de dinh/xe goi TCP
 */
public class ProtocolFraming {

    public static synchronized void writePacket(DataOutputStream out, Packet packet) throws IOException {
        PacketHeader header = packet.getHeader();
        out.writeByte(PacketHeader.MAGIC_1);
        out.writeByte(PacketHeader.MAGIC_2);
        out.writeByte(header.getVersion());
        out.writeByte(header.getType().getCode());
        out.writeInt(header.getPayloadLength());
        if (packet.getPayload().length > 0) {
            out.write(packet.getPayload());
        }
        out.flush();
    }

    public static Packet readPacket(DataInputStream in) throws IOException, ProtocolException {
        byte m1 = in.readByte();
        byte m2 = in.readByte();
        if (m1 != PacketHeader.MAGIC_1 || m2 != PacketHeader.MAGIC_2) {
            throw new ProtocolException("Goi tin khong hop le: Sai Magic Bytes (" + m1 + ", " + m2 + ")");
        }
        byte version = in.readByte();
        byte typeCode = in.readByte();
        MessageType type = MessageType.fromCode(typeCode);
        if (type == null) {
            throw new ProtocolException("Loai goi tin khong xac dinh: " + typeCode);
        }
        int length = in.readInt();
        if (length < 0 || length > 50 * 1024 * 1024) { // Max 50MB
            throw new ProtocolException("Do dai goi tin vuot nguong an toan: " + length);
        }
        byte[] payload = new byte[length];
        in.readFully(payload);
        return new Packet(type, payload);
    }
}
