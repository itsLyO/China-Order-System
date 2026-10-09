package client.core;

import common.protocol.*;
import common.util.LoggerUtil;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;

public class PacketListenerThread extends Thread {
    private final DataInputStream in;
    private final ClientSocketConnection connection;
    private volatile boolean running = true;

    public PacketListenerThread(DataInputStream in, ClientSocketConnection connection) {
        super("ClientPacketListener");
        this.in = in;
        this.connection = connection;
    }

    @Override
    public void run() {
        try {
            while (running) {
                Packet packet = ProtocolFraming.readPacket(in);
                onPacketReceived(packet);
            }
        } catch (EOFException e) {
            LoggerUtil.warn("PacketListener", "Mat ket noi voi Server!");
        } catch (IOException | ProtocolException e) {
            LoggerUtil.error("PacketListener", "Loi doc goi tin", e);
        } finally {
            connection.close();
        }
    }

    private void onPacketReceived(Packet packet) {
        LoggerUtil.info("PacketListener", "Nhan goi tin tu Server: " + packet.getHeader().getType().name());
    }
}
