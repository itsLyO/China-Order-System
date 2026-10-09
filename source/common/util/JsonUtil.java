package common.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Tien ich tuan tu hoa va giai tuan tu hoa JSON dung chung giua Server va Client
 */
public class JsonUtil {
    private static final Gson gson = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .serializeNulls()
            .create();

    public static String toJson(Object obj) {
        if (obj == null) return "{}";
        return gson.toJson(obj);
    }

    public static <T> T fromJson(String json, Class<T> clazz) {
        if (json == null || json.trim().isEmpty()) return null;
        return gson.fromJson(json, clazz);
    }

    public static Gson getGson() {
        return gson;
    }
}
