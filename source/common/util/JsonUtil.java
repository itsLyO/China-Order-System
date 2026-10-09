package common.util;

/**
 * Placeholder JsonUtil - co the tich hop voi Gson khi them gson.jar
 */
public class JsonUtil {
    public static String toJson(Object obj) {
        if (obj == null) return "{}";
        // Khi co gson-2.10.1.jar se goi new Gson().toJson(obj)
        return obj.toString();
    }
}
