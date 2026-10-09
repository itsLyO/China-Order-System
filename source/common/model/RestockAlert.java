package common.model;

public class RestockAlert {
    private String clientId;
    private String productCode;
    private String productName;
    private int currentStock;
    private String message;
    private String suggestedQuantity;

    public RestockAlert() {}
    public RestockAlert(String clientId, String productCode, String productName, int currentStock, String message) {
        this.clientId = clientId;
        this.productCode = productCode;
        this.productName = productName;
        this.currentStock = currentStock;
        this.message = message;
    }

    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }
    public String getProductCode() { return productCode; }
    public void setProductCode(String productCode) { this.productCode = productCode; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public int getCurrentStock() { return currentStock; }
    public void setCurrentStock(int currentStock) { this.currentStock = currentStock; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getSuggestedQuantity() { return suggestedQuantity; }
    public void setSuggestedQuantity(String suggestedQuantity) { this.suggestedQuantity = suggestedQuantity; }
}
