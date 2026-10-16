# HƯỚNG DẪN VẬN HÀNH VÀ QUẢN TRỊ SERVER NODE
## Hệ Thống Đặt Hàng & Điều Phối Logistics Nguồn Hàng Trung Quốc (China Order & Fulfillment System)
**Chịu trách nhiệm module Server:** Hiếu (Server Lead)  
**Tương thích & Phối hợp:** Quang (Client Lead)  
**Phiên bản:** 1.0.0 (Release Hoàn thiện Phase 5)

---

## 1. YÊU CẦU MÔI TRƯỜNG & CẤU HÌNH HỆ THỐNG
- **Hệ điều hành:** Windows 10/11 (khuyến nghị), Linux hoặc macOS.
- **Java Runtime Environment:** JDK 17 hoặc JDK 21 trở lên.
- **Biến môi trường:** Đã cấu hình `JAVA_HOME` và lệnh `java` / `javac` có thể thực thi từ Terminal/PowerShell.
- **Thư viện phụ thuộc (`lib/`):**
  - `sqlite-jdbc-3.45.1.0.jar`: Driver kết nối cơ sở dữ liệu SQLite Server.
  - `slf4j-api-1.7.36.jar`: Giao diện logging cho SQLite JDBC.
  - `gson-2.10.1.jar`: Thư viện tuần tự hóa và giải tuần tự JSON giao thức.
  - `flatlaf-3.5.jar`: Thư viện giao diện đồ họa hiện đại FlatLaf (Dark/Light).

---

## 2. HƯỚNG DẪN KHỞI ĐỘNG SERVER

### Cách 1: Khởi động nhanh bằng kịch bản Batch Script (Khuyến nghị trên Windows)
1. Mở thư mục dự án `assignment-network-project/`.
2. Nhấp đúp chuột vào file:
   ```cmd
   scripts\run_server.bat
   ```
   *Hoặc mở Command Prompt / PowerShell tại thư mục dự án và gõ:*
   ```cmd
   .\scripts\run_server.bat
   ```
3. Cửa sổ giao diện **Server Control Center Dashboard** sẽ tự động mở lên với giao diện Dark Theme hiện đại, lắng nghe tại cổng mặc định `8888`.

### Cách 2: Khởi động trực tiếp bằng dòng lệnh Java CLI
Từ thư mục gốc dự án `assignment-network-project`:
```bash
# Khởi động với cổng mặc định 8888:
java -cp "bin;lib/*" server.ui.ServerMainFrame

# Hoặc khởi động với cổng tùy biến (ví dụ 8899):
java -cp "bin;lib/*" server.ui.ServerMainFrame 8899
```

---

## 3. TỔNG QUAN GIAO DIỆN & CÁC TAB CHỨC NĂNG QUẢN TRỊ

Giao diện **Server Control Center** được thiết kế theo cấu trúc TabbedPane với 5 phân hệ chuyên sâu:

### 3.1. Tab 1: Giám sát kết nối (Clients)
- **Thống kê tổng quan:** Thanh Header hiển thị trạng thái Server (`RUNNING - Port 8888`), tổng số Client đang trực tuyến (`Online Clients`), số luồng Worker đang hoạt động và bộ nhớ RAM đã cấp phát.
- **Bảng danh sách kết nối:**
  - `Client ID`: Mã định danh khách hàng (ví dụ: `USR_QUANG`).
  - `Username`: Tên tài khoản đăng nhập (`daily_quang`).
  - `Role`: Vai trò (`CLIENT_RETAILER`, `CLIENT_WHOLESALE`, `ADMIN`).
  - `Remote Address`: Địa chỉ IP và Port kết nối từ xa (`/127.0.0.1:xxxxx`).
  - `Login Time`: Thời điểm xác thực phiên thành công.
  - `Last Active`: Thời gian ghi nhận gói tin cuối cùng (cập nhật liên tục bởi Heartbeat).
- **Thao tác quản trị:** Chọn một Client trong danh sách và bấm **"Ngắt kết nối (Kick/Disconnect)"** để chủ động thu hồi phiên kết nối khi cần thiết.

### 3.2. Tab 2: Quản lý Ticket & Báo giá (Tickets)
- **Tiếp nhận yêu cầu (Ticket):** Khi phía Client gửi yêu cầu tìm nguồn hàng (`TICKET_CREATE_REQ`), ticket sẽ xuất hiện ngay lập tức tại bảng với trạng thái `PENDING_QUOTE`.
- **Tính giá sỉ bậc thang & Phát hành báo giá:**
  1. Chọn ticket cần báo giá.
  2. Bấm nút **"Phát hành Báo giá"** (hoặc mở hộp thoại báo giá).
  3. Hệ thống `PricingService` áp dụng thuật toán chiết khấu tự động:
     - Số lượng từ 50 - 99: Chiết khấu 3%.
     - Số lượng từ 100 - 499: Chiết khấu 5%.
     - Số lượng từ 500 trở lên: Chiết khấu 10% - 15%.
     - Tỷ giá NDT (CNY/VND) và phí dịch vụ được quy đổi minh bạch.
  4. Hệ thống tự động tạo mã thanh toán **VietQR chuẩn Napas 247**:
     - Cú pháp thanh toán: `COC <MÃ_TICKET>`.
     - Số tiền cọc quy định: 30% tổng giá trị đơn hàng.
  5. Bấm gửi báo giá: Gói tin `TICKET_QUOTE_RESP` sẽ được **Server Push** lập tức về màn hình của Client tương ứng.
- **Duyệt đặt cọc:** Khi nhận được chuyển khoản cọc, Admin chọn ticket và bấm **"Xác nhận đã cọc"**. Trạng thái đơn lập tức đổi thành `DEPOSITED` và thông báo tới Client theo thời gian thực.

### 3.3. Tab 3: Điều phối & Tracking Logistics (Tracking)
Quản lý chuỗi cung ứng 5 chặng vận chuyển độc quyền Việt - Trung:
1. `ORDERED_CN`: Đã đặt hàng nhà sản xuất tại Trung Quốc.
2. `DOMESTIC_SHIPPING_CN`: Xưởng TQ xuất hàng & đang vận chuyển nội địa Trung Quốc.
3. `WAREHOUSE_BORDER_CN`: Hàng đã tập kết tại Tổng kho Bằng Tường / Đông Hưng (biên giới).
4. `CUSTOMS_CLEARANCE`: Đang làm thủ tục thông quan hàng hóa chính ngạch tại cửa khẩu.
5. `SHIPPING_VN`: Đã nhập kho Việt Nam (Hà Nội/TP.HCM) và giao hàng nội địa tới tay đại lý.

**Cách điều phối:**
- Chọn mã Ticket trên bảng điều phối.
- Chọn chặng logistics tiếp theo, nhập vị trí kho hiện tại (ví dụ: `Kho Đông Hưng, Quảng Tây`) và ghi chú vận tải.
- Bấm **"Chuyển chặng & Push thông báo"**: Server ghi log lịch sử tracking vào SQLite và **Server Push** gói tin `ORDER_STATUS_UPDATE` tới Client trong tích tắc (< 50ms).

### 3.4. Tab 4: Tin nhắn & Tiếp nhận mẫu ảnh (Chat & Samples)
- **Hỗ trợ tư vấn 1-1:** Khung chat trực tiếp giữa Admin Hiếu và từng đối tác Client.
- **Tiếp nhận ảnh mẫu sản phẩm:**
  - Nhận luồng dữ liệu nhị phân ảnh được chia nhỏ (chunked transfer).
  - Tự động ráp nối các chunk và tính toán mã băm **MD5 Checksum**.
  - So sánh MD5 thực tế với mã MD5 trong header: Đảm bảo ảnh lưu xuống đĩa tại `data/uploads/` không bao giờ bị méo mó hay sai lệch byte do đường truyền.
- **Nút tiện ích "Mở thư mục ảnh (`data/uploads`)":** Bấm nút để mở nhanh thư mục lưu trữ file ảnh trên Windows Explorer.

### 3.5. Tab 5: Nhật ký hệ thống (System Logs)
- Ghi nhận chi tiết từng sự kiện vận hành: Khởi động server, kết nối socket mới, xác thực tài khoản, xử lý gói tin, tính toán báo giá, lưu ảnh, lỗi đường truyền.
- Định dạng log chuyên nghiệp: `[YYYY-MM-DD HH:mm:ss.SSS] [MỨC ĐỘ] [TÊN LUỒNG] [CLASS]: Thông điệp`.
- Hỗ trợ ô tìm kiếm/lọc nhật ký thời gian thực.
- Nút **"Xóa log"** (Clear screen) và nút **"Xuất nhật ký ra file"** (Export to `.log`).

---

## 4. TÍNH NĂNG ĐỘC ĐÁO & ĐIỂM SÁNG TẠO: NOVELTY 1
Hệ thống tích hợp giải thuật giám sát tồn kho đại lý và cảnh báo chủ động từ Server:
1. **Đồng bộ tồn kho (`INVENTORY_SYNC`):** Phía Client định kỳ hoặc sau mỗi giao dịch gửi danh mục hàng hóa trong kho lên Server.
2. **Server quét ngưỡng cảnh báo:** `InventoryMonitorService` tự động đối soát `current_stock` so với `min_threshold`.
3. **Chủ động Server Push (`RESTOCK_ALERT_PUSH`):**
   - Khi phát hiện mặt hàng sắp cạn kiệt, Server chủ động phát gói cảnh báo xuống Client đang mở app.
   - Gợi ý số lượng nhập bổ sung ưu đãi từ xưởng sản xuất cũ.
4. **Tái đặt hàng nhanh 1-Click (`REORDER_FAST_REQ`):**
   - Khách hàng bấm xác nhận tái đặt hàng trên popup cảnh báo.
   - Server lập tức tạo Ticket mới liên kết trực tiếp với thông tin xưởng cũ, bỏ qua bước đàm phán lại từ đầu.

---

## 5. THÔNG TIN PHỐI HỢP & TEST TÍCH HỢP CLIENT (QUANG)
Để phía Client Node của bạn Quang kết nối kiểm thử thành công:
- **Địa chỉ Host:** `localhost` (nếu chạy cùng máy) hoặc địa chỉ IPv4 LAN của máy Server.
- **Cổng TCP:** `8888` (hoặc cổng cấu hình lúc start).
- **Tài khoản đăng nhập kiểm thử khởi tạo sẵn (Seed Data):**
  | Tài khoản (Username) | Mật khẩu | Mã User | Vai trò |
  | :--- | :--- | :--- | :--- |
  | `admin_hieu` | `admin123` | `USR_ADMIN` | `ADMIN` (Quản trị hệ thống) |
  | `daily_quang` | `quang123` | `USR_QUANG` | `CLIENT_RETAILER` (Đại lý bán lẻ) |
  | `khach_linh` | `linh123` | `USR_LINH` | `CLIENT_WHOLESALE` (Khách buôn sỉ) |

---

## 6. XỬ LÝ SỰ CỐ THƯỜNG GẶP (TROUBLESHOOTING)
1. **Lỗi `Address already in use: bind`:**
   - Nguyên nhân: Cổng 8888 đang bị chiếm dụng bởi một tiến trình cũ chưa tắt hẳn.
   - Khắc phục: Khởi động Server với cổng khác (`java -cp "bin;lib/*" server.ui.ServerMainFrame 8899`) hoặc mở Task Manager / gõ `netstat -ano | findstr :8888` để kill tiến trình cũ.
2. **Lỗi `AccessDeniedException` trên Windows PowerShell:**
   - Khi chạy script hoặc lệnh build trên Windows có dấu tiếng Việt, sử dụng đường dẫn tương đối và chạy với quyền User hợp lệ.
3. **Dữ liệu Database:** File CSDL được lưu trữ tự động tại `data/server_storage.db`. Khi cần reset về trạng thái ban đầu, chỉ cần xóa file này; Server khi khởi động lại sẽ tự động tạo bảng mới và nạp dữ liệu mẫu ban đầu.
