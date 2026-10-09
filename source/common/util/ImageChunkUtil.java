package common.util;

import java.util.Base64;

public class ImageChunkUtil {
    public static String toBase64(byte[] imageBytes) {
        return Base64.getEncoder().encodeToString(imageBytes);
    }

    public static byte[] fromBase64(String base64Str) {
        return Base64.getDecoder().decode(base64Str);
    }
}
