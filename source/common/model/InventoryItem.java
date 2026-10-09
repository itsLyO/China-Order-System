package common.model;

public class InventoryItem {
    private String productCode;
    private String productName;
    private int currentStock;
    private int soldQuantity;
    private int minSafetyThreshold;

    public InventoryItem() {}
    public InventoryItem(String productCode, String productName, int currentStock, int soldQuantity, int minSafetyThreshold) {
        this.productCode = productCode;
        this.productName = productName;
        this.currentStock = currentStock;
        this.soldQuantity = soldQuantity;
        this.minSafetyThreshold = minSafetyThreshold;
    }

    public String getProductCode() { return productCode; }
    public void setProductCode(String productCode) { this.productCode = productCode; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public int getCurrentStock() { return currentStock; }
    public void setCurrentStock(int currentStock) { this.currentStock = currentStock; }
    public int getSoldQuantity() { return soldQuantity; }
    public void setSoldQuantity(int soldQuantity) { this.soldQuantity = soldQuantity; }
    public int getMinSafetyThreshold() { return minSafetyThreshold; }
    public void setMinSafetyThreshold(int minSafetyThreshold) { this.minSafetyThreshold = minSafetyThreshold; }
}
