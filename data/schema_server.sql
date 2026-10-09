-- =========================================================================
-- DDL Schema for Server Database (SQLite)
-- China Order & Fulfillment System
-- =========================================================================

-- 1. Bảng tài khoản người dùng
CREATE TABLE IF NOT EXISTS users (
    user_id TEXT PRIMARY KEY,
    username TEXT UNIQUE NOT NULL,
    password_hash TEXT NOT NULL,
    full_name TEXT NOT NULL,
    role TEXT NOT NULL, -- 'SERVER_ADMIN' hoặc 'CLIENT_RETAILER'
    phone TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. Bảng vé đặt hàng (Order Tickets)
CREATE TABLE IF NOT EXISTS order_tickets (
    ticket_id TEXT PRIMARY KEY,
    client_id TEXT NOT NULL,
    product_name TEXT NOT NULL,
    quantity INTEGER NOT NULL,
    color TEXT,
    size TEXT,
    product_link TEXT,
    image_url TEXT,
    client_note TEXT,
    status TEXT NOT NULL, -- PENDING_QUOTE, QUOTED, DEPOSITED, ORDERED_CN, DOMESTIC_CN, WAREHOUSE_BORDER, CUSTOMS, SHIPPING_VN, COMPLETED, CANCELLED
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (client_id) REFERENCES users(user_id)
);

-- 3. Bảng báo giá và thông tin cọc (Price Quotes & Deposits)
CREATE TABLE IF NOT EXISTS price_quotes (
    quote_id TEXT PRIMARY KEY,
    ticket_id TEXT UNIQUE NOT NULL,
    unit_price_cny REAL NOT NULL,
    exchange_rate REAL NOT NULL,
    domestic_shipping_cny REAL DEFAULT 0.0,
    intl_shipping_vnd REAL NOT NULL,
    total_estimated_vnd REAL NOT NULL,
    deposit_required_vnd REAL NOT NULL,
    qr_payload TEXT,
    deposit_confirmed INTEGER DEFAULT 0, -- 0: Chưa cọc, 1: Đã xác nhận cọc
    quoted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (ticket_id) REFERENCES order_tickets(ticket_id)
);

-- 4. Bảng lịch sử thay đổi trạng thái vận chuyển (Tracking History)
CREATE TABLE IF NOT EXISTS order_tracking_logs (
    log_id INTEGER PRIMARY KEY AUTOINCREMENT,
    ticket_id TEXT NOT NULL,
    status TEXT NOT NULL,
    location TEXT,
    note TEXT,
    updated_by TEXT NOT NULL,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (ticket_id) REFERENCES order_tickets(ticket_id)
);

-- 5. Bảng lịch sử tin nhắn đàm phán 1-1 (Chat Messages)
CREATE TABLE IF NOT EXISTS chat_messages (
    message_id TEXT PRIMARY KEY,
    sender_id TEXT NOT NULL,
    receiver_id TEXT NOT NULL,
    content_type TEXT NOT NULL, -- 'TEXT' hoặc 'IMAGE'
    content TEXT NOT NULL,
    image_checksum TEXT,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 6. Bảng theo dõi tồn kho của các đại lý (Server Replica Inventory)
CREATE TABLE IF NOT EXISTS client_inventories (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    client_id TEXT NOT NULL,
    product_code TEXT NOT NULL,
    product_name TEXT NOT NULL,
    current_stock INTEGER NOT NULL,
    sold_quantity INTEGER NOT NULL,
    min_safety_threshold INTEGER DEFAULT 10,
    last_synced_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(client_id, product_code),
    FOREIGN KEY (client_id) REFERENCES users(user_id)
);
