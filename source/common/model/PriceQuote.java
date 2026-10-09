package common.model;

public class PriceQuote {
    private String quoteId;
    private String ticketId;
    private double unitPriceCny;
    private double exchangeRate;
    private double domesticShippingCny;
    private double intlShippingVnd;
    private double totalEstimatedVnd;
    private double depositRequiredVnd;
    private String qrPayload;

    public PriceQuote() {}

    public String getQuoteId() { return quoteId; }
    public void setQuoteId(String quoteId) { this.quoteId = quoteId; }
    public String getTicketId() { return ticketId; }
    public void setTicketId(String ticketId) { this.ticketId = ticketId; }
    public double getUnitPriceCny() { return unitPriceCny; }
    public void setUnitPriceCny(double unitPriceCny) { this.unitPriceCny = unitPriceCny; }
    public double getExchangeRate() { return exchangeRate; }
    public void setExchangeRate(double exchangeRate) { this.exchangeRate = exchangeRate; }
    public double getDomesticShippingCny() { return domesticShippingCny; }
    public void setDomesticShippingCny(double domesticShippingCny) { this.domesticShippingCny = domesticShippingCny; }
    public double getIntlShippingVnd() { return intlShippingVnd; }
    public void setIntlShippingVnd(double intlShippingVnd) { this.intlShippingVnd = intlShippingVnd; }
    public double getTotalEstimatedVnd() { return totalEstimatedVnd; }
    public void setTotalEstimatedVnd(double totalEstimatedVnd) { this.totalEstimatedVnd = totalEstimatedVnd; }
    public double getDepositRequiredVnd() { return depositRequiredVnd; }
    public void setDepositRequiredVnd(double depositRequiredVnd) { this.depositRequiredVnd = depositRequiredVnd; }
    public String getQrPayload() { return qrPayload; }
    public void setQrPayload(String qrPayload) { this.qrPayload = qrPayload; }
}
