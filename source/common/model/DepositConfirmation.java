package common.model;

public class DepositConfirmation {
    private String ticketId;
    private String clientId;
    private double amountPaid;
    private String transactionRef;
    private String billImageUrl;
    private boolean isConfirmed;
    private String confirmedAt;

    public DepositConfirmation() {}

    public String getTicketId() { return ticketId; }
    public void setTicketId(String ticketId) { this.ticketId = ticketId; }
    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }
    public double getAmountPaid() { return amountPaid; }
    public void setAmountPaid(double amountPaid) { this.amountPaid = amountPaid; }
    public String getTransactionRef() { return transactionRef; }
    public void setTransactionRef(String transactionRef) { this.transactionRef = transactionRef; }
    public String getBillImageUrl() { return billImageUrl; }
    public void setBillImageUrl(String billImageUrl) { this.billImageUrl = billImageUrl; }
    public boolean isConfirmed() { return isConfirmed; }
    public void setConfirmed(boolean confirmed) { isConfirmed = confirmed; }
    public String getConfirmedAt() { return confirmedAt; }
    public void setConfirmedAt(String confirmedAt) { this.confirmedAt = confirmedAt; }
}
