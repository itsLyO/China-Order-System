package server.core;

import common.protocol.Packet;

/**
 * Interface dinh tuyen va phan phoi goi tin nhan duoc tu Client toi cac Service nghiep vu
 */
public interface PacketDispatcher {
    /**
     * Xu ly goi tin nhan duoc tu Client
     *
     * @param handler Phien ket noi ClientHandler dang tuong tac
     * @param packet  Goi tin mang da duoc giai ma qua ProtocolFraming
     */
    void dispatch(ClientHandler handler, Packet packet);
}
