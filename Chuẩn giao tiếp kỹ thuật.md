# CHUẨN GIAO TIẾP KỸ THUẬT & HỢP ĐỒNG DỮ LIỆU (TECHNICAL & PROTOCOL CONTRACT)

> **Dự án: Hệ thống Order Hàng Trung Quốc (Client/Server)**  
> **Bộ môn: Lập trình mạng (2026–2027)**  
> **Thành viên 1: Hiếu (Server Lead) | Thành viên 2: Quang (Client Lead)**  
> **Trạng thái: ĐÃ KHÓA & THỐNG NHẤT 100% (LOCKED & FINALIZED)**

---

## 1. NGUYÊN TẮC BẤT KHẢ XÂM PHẠM GIỮA 2 THÀNH VIÊN

1. **Phân định phạm vi trách nhiệm (Ownership)**:
   - **Thành viên 1: Hiếu**: Toàn quyền phụ trách phía **Server Node** (`source/server/**`, `data/schema_server.sql`, `scripts/run_server.bat`).
   - **Thành viên 2: Quang**: Toàn quyền phụ trách phía **Client Node** (`source/client/**`, `data/schema_client.sql`, `scripts/run_client.bat`).
2. **Quy tắc cô lập mã nguồn (Isolation Rule)**:
   - Hiếu **tuyệt đối không can thiệp, chỉnh sửa hay xóa** bất kỳ file nào trong thư mục `source/client/` hoặc `data/schema_client.sql`.
   - Quang **tuyệt đối không can thiệp** vào `source/server/` hoặc `data/schema_server.sql`.
3. **Vùng dùng chung đóng băng (Frozen Common Layer)**:
   - Thư mục `source/common/**` (`protocol`, `model`, `util`) và `lib/` là nền tảng chung đã được thống nhất tại văn bản này. Mọi thay đổi trong vùng này bắt buộc phải có sự đồng thuận của cả Hiếu và Quang.
4. **Quy tắc kiểm soát phiên bản Git (No-Push Without Approval)**:
   - Hiếu làm việc độc lập trên nhánh `server`.
   - Quang làm việc độc lập trên nhánh `client`.
   - **Tuyệt đối không tự ý chạy `git push` lên GitHub** nếu chưa có yêu cầu trực tiếp từ Hiếu.

---

## 2. QUY CHUẨN GIAO THỨC MẠNG (CUSTOM FRAMED TCP PROTOCOL)

Hệ thống hoạt động trên kết nối **TCP Socket thuần**, cổng mặc định `8888`.

### 2.1. Cấu trúc khung gói tin (Packet Frame - Big Endian):
```text
+---------------------+------------------+-------------------+--------------------------+-----------------------+
|  Magic Bytes (2B)   |  Version (1B)    |  Msg Type (1B)    | Payload Length (4 Bytes) | Payload Data (N Bytes)|
|  0x43 0x4E ('CN')   |  0x01            |  [Byte Enum]      | [Int: 0 -> 50MB]         | [JSON UTF-8 / Binary] |
+---------------------+------------------+-------------------+--------------------------+-----------------------+
```

### 2.2. Danh mục mã lệnh (MessageType Dictionary):
| Mã Hex | Enum Name | Chiều truyền | Ý nghĩa |
| :---: | :--- | :---: | :--- |
| `0x01` | `AUTH_LOGIN_REQ` | Client $\rightarrow$ Server | Yêu cầu đăng nhập tài khoản |
| `0x02` | `AUTH_LOGIN_RESP` | Server $\rightarrow$ Client | Phản hồi kết quả đăng nhập |
| `0x10` | `CHAT_TEXT_MSG` | Client $\leftrightarrow$ Server | Tin nhắn văn bản đàm phán 1-1 |
| `0x11` | `CHAT_IMAGE_MSG` | Client $\leftrightarrow$ Server | Gửi hình ảnh mẫu sản phẩm đính kèm |
| `0x20` | `TICKET_CREATE_REQ` | Client $\rightarrow$ Server | Client tạo vé yêu cầu order hàng |
| `0x21` | `TICKET_QUOTE_RESP` | Server $\rightarrow$ Client | Server gửi báo giá chi tiết & mã QR cọc |
| `0x22` | `TICKET_DEPOSIT_CONFIRM` | Client $\leftrightarrow$ Server | Xác nhận nộp cọc (kèm bill) & Server duyệt cọc |
| `0x23` | `ORDER_STATUS_UPDATE` | Server $\rightarrow$ Client | Server Push cập nhật 7 chặng vận chuyển |
| `0x30` | `INVENTORY_SYNC` | Client $\rightarrow$ Server | Client đồng bộ số lượng tồn kho & hàng đã bán |
| `0x31` | `RESTOCK_ALERT_PUSH` | Server $\rightarrow$ Client | Server chủ động Push cảnh báo sắp hết hàng |
| `0x40` | `REORDER_FAST_REQ` | Client $\rightarrow$ Server | Client kích hoạt tái đặt hàng nhanh (1-Click) |
| `0x90` | `PING` | Client $\rightarrow$ Server | Gói tin giữ kết nối (Heartbeat mỗi 15s) |
| `0x91` | `PONG` | Server $\rightarrow$ Client | Phản hồi giữ kết nối |

---

## 3. ĐẶC TẢ CHI TIẾT 10 JSON PAYLOAD SCHEMAS

### 3.1. `AUTH_LOGIN_REQ (0x01)`
```json
{
  "username": "daily_hoangmai",
  "password": "plain_or_hash_pass",
  "clientType": "CLIENT_RETAILER"
}
```

### 3.2. `AUTH_LOGIN_RESP (0x02)`
```json
{
  "success": true,
  "userId": "USR_9921",
  "fullName": "Đại lý Hoàng Mai",
  "role": "CLIENT_RETAILER",
  "message": "Đăng nhập thành công"
}
```

### 3.3. `CHAT_TEXT_MSG (0x10)`
```json
{
  "messageId": "MSG_1728456100_01",
  "senderId": "USR_9921",
  "receiverId": "SERVER_ADMIN",
  "content": "Tôi đang cần tìm lô áo phao này bên xưởng Quảng Châu, bạn xem deal giá giúp tôi nhé",
  "timestamp": "2026-10-09 15:40:00"
}
```

### 3.4. `CHAT_IMAGE_MSG (0x11)`
```json
{
  "messageId": "MSG_IMG_1728456100_02",
  "senderId": "USR_9921",
  "receiverId": "SERVER_ADMIN",
  "fileName": "ao_phao_mau_do.jpg",
  "fileSize": 184520,
  "md5Checksum": "e99a18c428cb38d5f260853678922e03",
  "base64Data": "iVBORw0KGgoAAAANSUhEUgAA...",
  "timestamp": "2026-10-09 15:40:05"
}
```

### 3.5. `TICKET_CREATE_REQ (0x20)`
```json
{
  "ticketId": "TCK_20261009_001",
  "clientId": "USR_9921",
  "productName": "Áo phao đại hàn lông vũ",
  "quantity": 100,
  "color": "Đỏ đô, Đen",
  "size": "L, XL",
  "productLink": "https://detail.1688.com/offer/7123456.html",
  "imageUrl": "ao_phao_mau_do.jpg",
  "clientNote": "Cần hàng về trước ngày 20/11 để kịp bán đợt rét đầu mùa"
}
```

### 3.6. `TICKET_QUOTE_RESP (0x21)`
```json
{
  "quoteId": "QTE_20261009_001",
  "ticketId": "TCK_20261009_001",
  "unitPriceCny": 45.0,
  "exchangeRate": 3650.0,
  "domesticShippingCny": 50.0,
  "intlShippingVnd": 1200000.0,
  "totalEstimatedVnd": 17807500.0,
  "depositRequiredVnd": 5000000.0,
  "qrPayload": "https://img.vietqr.io/image/970422-0987654321-compact2.png?amount=5000000&addInfo=COC%20TCK_20261009_001",
  "status": "QUOTED",
  "quotedAt": "2026-10-09 15:55:00"
}
```

### 3.7. `TICKET_DEPOSIT_CONFIRM (0x22)`
```json
{
  "ticketId": "TCK_20261009_001",
  "clientId": "USR_9921",
  "amountPaid": 5000000.0,
  "transactionRef": "MBVCB.987654321",
  "billImageUrl": "receipt_5tr.jpg",
  "isConfirmed": true,
  "confirmedAt": "2026-10-09 16:10:00"
}
```

### 3.8. `ORDER_STATUS_UPDATE (0x23)`
Vòng đời 9 trạng thái:
`PENDING_QUOTE` $\rightarrow$ `QUOTED` $\rightarrow$ `DEPOSITED` $\rightarrow$ `ORDERED_CN` $\rightarrow$ `DOMESTIC_SHIPPING_CN` $\rightarrow$ `WAREHOUSE_BORDER_CN` $\rightarrow$ `CUSTOMS_CLEARANCE` $\rightarrow$ `SHIPPING_VN` $\rightarrow$ `COMPLETED` (hoặc `CANCELLED`).
```json
{
  "ticketId": "TCK_20261009_001",
  "oldStatus": "ORDERED_CN",
  "newStatus": "WAREHOUSE_BORDER_CN",
  "location": "Kho Bằng Tường / Đông Hưng (Trung Quốc)",
  "note": "Kiện hàng đã tập kết tại kho biên giới, chuẩn bị đóng bao thông quan",
  "timestamp": "2026-10-12 09:30:00"
}
```

### 3.9. `INVENTORY_SYNC (0x30)`
```json
{
  "clientId": "USR_9921",
  "items": [
    {
      "productCode": "SP_AP01",
      "productName": "Áo phao đại hàn lông vũ",
      "currentStock": 8,
      "soldQuantity": 92,
      "minSafetyThreshold": 15
    }
  ],
  "syncedAt": "2026-10-15 14:00:00"
}
```

### 3.10. `RESTOCK_ALERT_PUSH (0x31)` & `REORDER_FAST_REQ (0x40)`
**Server Push Cảnh báo (`0x31`):**
```json
{
  "clientId": "USR_9921",
  "productCode": "SP_AP01",
  "productName": "Áo phao đại hàn lông vũ",
  "currentStock": 8,
  "minSafetyThreshold": 15,
  "suggestedQuantity": 100,
  "message": "Hàng sắp hết rồi, ông có muốn lấy thêm không? Đợt này xưởng đang có sẵn lô mới giá tốt!",
  "canFastReorder": true,
  "referenceTicketId": "TCK_20261009_001"
}
```
**Client 1-Click Reorder (`0x40`):**
```json
{
  "newTicketId": "TCK_20261015_009",
  "referenceTicketId": "TCK_20261009_001",
  "clientId": "USR_9921",
  "quantity": 100,
  "note": "Tái đặt hàng nhanh theo gợi ý của Server"
}
```

---

## 4. BỘ THƯ VIỆN CHUẨN ĐÃ TÍCH HỢP TRONG `lib/`

Cả Hiếu và Quang đều sử dụng chung 3 file JAR sau (đã có sẵn trong `assignment-network-project/lib/`):
1. **`gson-2.10.1.jar`**: Xử lý JSON chuẩn hóa cho toàn bộ các payload trên.
2. **`sqlite-jdbc-3.45.1.0.jar`**: Kết nối CSDL SQLite độc lập cho cả Server và Client.
3. **`flatlaf-3.5.jar`**: Thư viện Look and Feel giao diện Swing hiện đại.

---

**VĂN BẢN ĐÃ ĐƯỢC CHỐT VÀ CÓ HIỆU LỰC TOÀN DIỆN CHO CẢ HIẾU VÀ QUANG TRƯỚC KHI BƯỚC VÀO GIAI ĐOẠN LẬP TRÌNH.**
