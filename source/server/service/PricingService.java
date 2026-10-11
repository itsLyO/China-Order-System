package server.service;

import common.model.PriceQuote;
import common.util.LoggerUtil;
import common.util.QRCodeGenerator;

/**
 * Dich vu tinh toan bang gia, quy doi ngoai te, tinh chiet khau bac thang va tao ma VietQR
 */
public class PricingService {
    public static final double DEFAULT_EXCHANGE_RATE = 3650.0; // VND / CNY
    public static final String DEFAULT_BANK_BIN = "970422";    // MB Bank
    public static final String DEFAULT_ACCOUNT_NO = "0987654321"; // STK Hieu Admin

    /**
     * Tinh ti le chiet khau si theo bac thang so luong
     * - 1 - 50 sp: Gia goc (0% giam)
     * - 51 - 200 sp: Giam 5% gia CNY
     * - Tren 200 sp: Giam 10% gia CNY
     */
    public double getDiscountRate(int quantity) {
        if (quantity > 200) {
            return 0.10;
        } else if (quantity > 50) {
            return 0.05;
        } else {
            return 0.0;
        }
    }

    /**
     * Uoc tinh phi van chuyen quoc te TQ-VN dua tren so luong / trong luong
     * Mua nhieu gia ship trung binh/san pham cang re, toi thieu 500,000 VND
     */
    public double estimateIntlShipping(int quantity) {
        double fee = quantity * 12000.0; // 12,000 VND/sp
        return Math.max(500000.0, fee);
    }

    /**
     * Tinh toan day du bao gia va sinh link ma VietQR dat coc
     */
    public PriceQuote calculatePriceQuote(String ticketId, double rawUnitPriceCny, int quantity,
                                          double exchangeRate, double domesticShippingCny, double intlShippingVnd) {
        if (exchangeRate <= 0) {
            exchangeRate = DEFAULT_EXCHANGE_RATE;
        }
        if (intlShippingVnd <= 0) {
            intlShippingVnd = estimateIntlShipping(quantity);
        }

        double discountRate = getDiscountRate(quantity);
        double effectiveUnitPriceCny = rawUnitPriceCny * (1.0 - discountRate);

        // Tong tien VND = (Don gia tệ sau giam * So luong + Ship noi dia TQ) * Ti gia + Ship quoc te VND
        double totalEstimatedVnd = ((effectiveUnitPriceCny * quantity + domesticShippingCny) * exchangeRate) + intlShippingVnd;

        // Tien coc toi thieu 30%, lam tron den hang chuc nghin dong de dep giao dich
        double depositRaw = totalEstimatedVnd * 0.30;
        double depositRequiredVnd = Math.round(depositRaw / 10000.0) * 10000.0;

        // Sinh link VietQR dong
        String memo = "COC " + ticketId;
        String qrPayload = QRCodeGenerator.generateVietQRUrl(DEFAULT_BANK_BIN, DEFAULT_ACCOUNT_NO, depositRequiredVnd, memo);

        PriceQuote quote = new PriceQuote();
        quote.setQuoteId("QTE_" + ticketId);
        quote.setTicketId(ticketId);
        quote.setUnitPriceCny(effectiveUnitPriceCny);
        quote.setExchangeRate(exchangeRate);
        quote.setDomesticShippingCny(domesticShippingCny);
        quote.setIntlShippingVnd(intlShippingVnd);
        quote.setTotalEstimatedVnd(totalEstimatedVnd);
        quote.setDepositRequiredVnd(depositRequiredVnd);
        quote.setQrPayload(qrPayload);

        LoggerUtil.info("PricingService", String.format(
                "Tao bao gia cho ticket [%s]: Sl=%d, Gia Te goc=%.2f, Sau giam=%.2f (Giam %.0f%%), Tong=%.0f VND, Coc=%.0f VND",
                ticketId, quantity, rawUnitPriceCny, effectiveUnitPriceCny, discountRate * 100, totalEstimatedVnd, depositRequiredVnd
        ));

        return quote;
    }
}
