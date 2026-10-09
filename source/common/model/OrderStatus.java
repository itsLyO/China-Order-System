package common.model;

public enum OrderStatus {
    PENDING_QUOTE("Cho bao gia"),
    QUOTED("Da bao gia / Cho coc"),
    DEPOSITED("Da dat coc / Cho dat hang"),
    ORDERED_CN("Da dat hang ben Trung Quoc"),
    DOMESTIC_SHIPPING_CN("Van chuyen noi dia Trung Quoc"),
    WAREHOUSE_BORDER_CN("Ve kho bien gioi / Tap ket"),
    CUSTOMS_CLEARANCE("Dang thong quan"),
    SHIPPING_VN("Dang giao toi Dai ly"),
    COMPLETED("Giao thanh cong / Hoan tat"),
    CANCELLED("Da huy don");

    private final String description;
    OrderStatus(String description) { this.description = description; }
    public String getDescription() { return description; }
}
