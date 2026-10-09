package common.model;

public class FastReorderRequest {
    private String newTicketId;
    private String referenceTicketId;
    private String clientId;
    private int quantity;
    private String note;

    public FastReorderRequest() {}

    public String getNewTicketId() { return newTicketId; }
    public void setNewTicketId(String newTicketId) { this.newTicketId = newTicketId; }
    public String getReferenceTicketId() { return referenceTicketId; }
    public void setReferenceTicketId(String referenceTicketId) { this.referenceTicketId = referenceTicketId; }
    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
