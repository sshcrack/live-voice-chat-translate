package me.sshcrack.live_voice_translate.config;

import java.util.LinkedHashMap;
import java.util.Map;

public class LanguageCodes {
    private static final LinkedHashMap<String, String> CODES = new LinkedHashMap<>();

    static {
        CODES.put("en", "English");
        CODES.put("es", "Spanish");
        CODES.put("fr", "French");
        CODES.put("de", "German");
        CODES.put("it", "Italian");
        CODES.put("pt", "Portuguese");
        CODES.put("ru", "Russian");
        CODES.put("ja", "Japanese");
        CODES.put("ko", "Korean");
        CODES.put("zh", "Chinese (Simplified)");
        CODES.put("ar", "Arabic");
        CODES.put("hi", "Hindi");
        CODES.put("nl", "Dutch");
        CODES.put("pl", "Polish");
        CODES.put("tr", "Turkish");
        CODES.put("vi", "Vietnamese");
        CODES.put("th", "Thai");
        CODES.put("sv", "Swedish");
        CODES.put("da", "Danish");
        CODES.put("fi", "Finnish");
        CODES.put("nb", "Norwegian");
        CODES.put("cs", "Czech");
        CODES.put("hu", "Hungarian");
        CODES.put("ro", "Romanian");
        CODES.put("uk", "Ukrainian");
        CODES.put("el", "Greek");
        CODES.put("he", "Hebrew");
    }

    public static Map<String, String> getAll() {
        return CODES;
    }

    public static boolean isValid(String code) {
        return CODES.containsKey(code);
    }

    public static String getDisplayName(String code) {
        return CODES.getOrDefault(code, code);
    }
}
