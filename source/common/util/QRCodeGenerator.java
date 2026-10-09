package common.util;

public class QRCodeGenerator {
    public static String generateVietQRUrl(String bankBin, String accountNumber, double amount, String memo) {
        return String.format("https://img.vietqr.io/image/%s-%s-compact2.png?amount=%.0f&addInfo=%s",
                bankBin, accountNumber, amount, memo.replace(" ", "%20"));
    }
}
