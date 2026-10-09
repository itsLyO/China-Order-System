package common.model;

import java.util.List;

public class InventorySyncPayload {
    private String clientId;
    private List<InventoryItem> items;
    private String syncedAt;

    public InventorySyncPayload() {}

    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }
    public List<InventoryItem> getItems() { return items; }
    public void setItems(List<InventoryItem> items) { this.items = items; }
    public String getSyncedAt() { return syncedAt; }
    public void setSyncedAt(String syncedAt) { this.syncedAt = syncedAt; }
}
