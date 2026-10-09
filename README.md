# NETWORK PROGRAMMING – FINAL PROJECT

> **Academic Year 2026–2027 | Môn học: Lập trình mạng**

---

# 1. Project Information

## 1.1. Project Name
**Hệ thống Order và Vận chuyển Hàng Trung Quốc theo mô hình Client/Server (China Order & Fulfillment System)**

## 1.2. Topic
**Chủ đề 3 – Ứng dụng mạng (Network Application)**

## 1.3. Group Information

| No. | Student ID | Full Name | Email | Main Responsibility | Contribution |
| :---: | :---: | :---: | :---: | :--- | :---: |
| 1 | ... | Thành viên 1 | ... | **Server Core & Network Protocol Lead**: Custom TCP Framing, ServerSocket & ThreadPool, Server Push, SQLite DB, Server Dashboard UI, Scalability Benchmark | 50% |
| 2 | ... | Thành viên 2 | ... | **Client Core & Business Workflow Lead**: Client Socket Controller, Packet Listener, Heartbeat & Reconnect, Chunked Image Transfer, Kho hàng đại lý, Tracking Timeline UI, Latency Benchmark | 50% |
| | | | | **Total** | **100%** |

## 1.4. Instructor
**Giảng viên phụ trách bộ môn Lập trình mạng**

---

# 2. Project Summary

## 2.1. Problem
Trong hoạt động nhập hàng kinh doanh từ Trung Quốc về Việt Nam:
- Các đại lý bán lẻ (Client) gặp rào cản về ngoại ngữ, đàm phán giá cả, lựa chọn quy cách mẫu mã và theo dõi tiến độ vận chuyển xuyên biên giới.
- Người làm dịch vụ gom hàng & vận chuyển (Server) hoạt động theo mô hình **không có kho hàng (Zero Warehouse)**, thuần túy là đầu mối đàm phán, kết nối xưởng sản xuất Trung Quốc và tổ chức logistics về Việt Nam.
- Phương thức truyền thống thường giao tiếp rời rạc qua mạng xã hội, dẫn đến tình trạng mất dấu đơn hàng, bị động đợi khách hết hàng mới đặt tiếp, và thiếu một hệ thống mạng đồng bộ thời gian thực.

## 2.2. Motivation
Xây dựng một hệ thống phần mềm chuyên biệt theo mô hình mạng **Client/Server** nhằm:
- Số hóa toàn bộ quy trình: Chat đàm phán 1-1 kèm gửi hình ảnh $\rightarrow$ Tạo ticket order trực tiếp trong chat $\rightarrow$ Báo giá bậc thang và thanh toán cọc bằng mã QR $\rightarrow$ Cập nhật tracking 7 chặng vận chuyển thời gian thực.
- Giúp Server chủ động theo dõi tồn kho của các đại lý để kịp thời tư vấn nhập hàng bổ sung khi sắp hết hàng ("Hàng sắp hết rồi, ông có muốn lấy thêm không?"), biến quan hệ mua bán thụ động thành chuỗi cung ứng chủ động.

## 2.3. Objectives
1. Hiện thực hóa giao thức truyền thông tùy biến (**Custom Framed TCP Protocol**) với cơ chế chống dính/xé gói.
2. Xây dựng máy chủ đa luồng hiệu năng cao (**ThreadPoolExecutor**) phục vụ đồng thời hàng trăm đại lý.
3. Triển khai cơ chế **Server Push Notification** thời gian thực để cảnh báo tồn kho và cập nhật hành trình đơn.
4. Đảm bảo khả năng phục hồi mạng (**Fault-Tolerant Auto-Reconnect**) và truyền ảnh sản phẩm tối ưu (**Chunked Image Transfer**).

---

# 3. System Architecture & Protocol

## 3.1. Sơ đồ kiến trúc
Chi tiết tại `statics/architecture.png` và tài liệu kế hoạch `China_order_system_plan.md`.

## 3.2. Cấu trúc gói tin mạng (Packet Framing)
```text
+----------------+----------------+----------------+--------------------------+-----------------------+
|  Magic Bytes   | Version (1B)   | Msg Type (1B)  | Payload Length (4 Bytes) | Payload Data (N Bytes)|
|  0x43 0x4E     | 0x01           | [Enum Type]    | [Big-Endian Integer]     | [JSON UTF-8 / Binary] |
+----------------+----------------+----------------+--------------------------+-----------------------+
```

---

# 4. Hướng dẫn Biên dịch và Khởi chạy (Build & Run)

### 4.1. Yêu cầu môi trường
- **Java SE Development Kit (JDK)**: Phiên bản 21 trở lên (đã kiểm tra tương thích trên JDK 21 LTS).
- **Hệ điều hành**: Windows 10/11, Linux hoặc macOS.

### 4.2. Biên dịch (Build)
Chạy script tự động trong thư mục `scripts/`:
```cmd
cd scripts
build.bat
```

### 4.3. Chạy Server
```cmd
cd scripts
run_server.bat
```

### 4.4. Chạy Client
```cmd
cd scripts
run_client.bat
```

### 4.5. Chạy Kiểm thử đo lường hiệu năng (Benchmark)
```cmd
cd scripts
run_benchmark.bat
```

---

# 5. Điểm mới kỹ thuật (Novelty & Contributions)
1. **Proactive Inventory Monitoring & Server Push Engine**: Server giám sát ngưỡng an toàn của đại lý và chủ động Push cảnh báo qua Socket thời gian thực kèm nút 1-Click Reorder.
2. **Chunked Image Transfer Protocol**: Cơ chế cắt nhỏ file ảnh sản phẩm thành các frame nhị phân kèm mã băm MD5 toàn vẹn mà không gây nghẽn kênh điều khiển TCP.
3. **Fault-Tolerant Reconnection & Order State Machine**: Tự động phục hồi kết nối mạng khi chập chờn và đồng bộ chính xác 9 trạng thái vòng đời đơn hàng.
