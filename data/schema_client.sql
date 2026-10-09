-- =========================================================================
-- DDL Schema for Client Local Store (SQLite)
-- Local Retail Inventory & Order Cache
-- =========================================================================

-- 1. Bảng quản lý kho hàng cục bộ của đại lý (POS & Inventory)
CREATE TABLE IF NOT EXISTS local_inventory (
    product_code TEXT PRIMARY KEY,
    product_name TEXT NOT NULL,
    category TEXT,
    cost_price REAL NOT NULL,
    retail_price REAL NOT NULL,
    current_stock INTEGER NOT NULL,
    sold_quantity INTEGER DEFAULT 0,
    min_safety_threshold INTEGER DEFAULT 10,
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. Bảng lưu vết bán lẻ (Retail Sales Transactions)
CREATE TABLE IF NOT EXISTS local_sales_records (
    sale_id INTEGER PRIMARY KEY AUTOINCREMENT,
    product_code TEXT NOT NULL,
    quantity_sold INTEGER NOT NULL,
    sale_price REAL NOT NULL,
    sold_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (product_code) REFERENCES local_inventory(product_code)
);

-- 3. Bảng bộ đệm đơn hàng cục bộ để theo dõi offline (Order Tracking Cache)
CREATE TABLE IF NOT EXISTS local_order_cache (
    ticket_id TEXT PRIMARY KEY,
    product_name TEXT NOT NULL,
    quantity INTEGER NOT NULL,
    last_known_status TEXT NOT NULL,
    latest_update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
