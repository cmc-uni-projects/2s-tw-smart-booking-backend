package com.example.smart_booking_system.logging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.multipart.MultipartFile;

import java.time.temporal.TemporalAccessor;
import java.util.Collection;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Turns method arguments / return values into short, safe log text.
 * - secrets (password, token, secret, otp, card/account numbers) are masked by name;
 * - DTOs are rendered as JSON (masked), entities and other objects only as their class name
 *   so logging never triggers Hibernate lazy loading;
 * - output is truncated.
 */
final class LogSanitizer {

    static final String MASK = "***";
    private static final int MAX_LENGTH = 300;
    private static final Pattern SENSITIVE = Pattern.compile(
            "(?i).*(password|passwd|pwd|token|secret|otp|card|cvv|accountnumber|authorization|credential).*");
    private static final String DTO_PACKAGE = "com.example.smart_booking_system.dto";

    private final ObjectMapper mapper;

    LogSanitizer(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    String args(String[] names, Object[] values) {
        if (values == null || values.length == 0) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) sb.append(", ");
            String name = names != null && i < names.length ? names[i] : "arg" + i;
            sb.append(name).append('=').append(isSensitive(name) ? MASK : value(values[i]));
        }
        return truncate(sb.toString());
    }

    String value(Object v) {
        if (v == null) return "null";
        if (v instanceof CharSequence s) return "\"" + truncate(s.toString(), 80) + "\"";
        if (v instanceof Number || v instanceof Boolean || v instanceof Enum<?> || v instanceof TemporalAccessor) return v.toString();
        if (v instanceof MultipartFile f) return "File(" + f.getOriginalFilename() + ", " + f.getSize() + "B)";
        if (v instanceof MultipartFile[] fs) return "Files[" + fs.length + "]";
        if (v instanceof Collection<?> c) return v.getClass().getSimpleName() + "[" + c.size() + "]";
        if (v instanceof Map<?, ?> m) return "Map[" + m.size() + "]";
        if (v.getClass().isArray()) return v.getClass().getComponentType().getSimpleName() + "[]";
        if (v.getClass().getName().startsWith(DTO_PACKAGE)) return dto(v);
        return v.getClass().getSimpleName();
    }

    private String dto(Object v) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = mapper.convertValue(v, Map.class);
            map.replaceAll((k, val) -> isSensitive(k) ? MASK : (val instanceof Map || val instanceof Collection ? "…" : val));
            return v.getClass().getSimpleName() + map;
        } catch (IllegalArgumentException e) {
            return v.getClass().getSimpleName();
        }
    }

    private static boolean isSensitive(String name) {
        return name != null && SENSITIVE.matcher(name).matches();
    }

    private static String truncate(String s) {
        return truncate(s, MAX_LENGTH);
    }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }
}
