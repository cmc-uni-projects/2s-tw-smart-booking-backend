package com.example.smart_booking_system.logging;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.hibernate.Hibernate;
import org.hibernate.proxy.HibernateProxy;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.BindingResult;
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Turns method arguments / return values into readable, safe log text.
 * <ul>
 *   <li>secrets are masked by name (password, token, secret, otp, card/account numbers…), at any depth;</li>
 *   <li>DTOs and other project objects are rendered field by field;</li>
 *   <li>JPA entities show their own column values; relations show only {@code Entity#id} and lazy
 *       collections are not touched, so logging never triggers Hibernate lazy loading;</li>
 *   <li>collections / maps / pages show their first elements; the whole text is truncated.</li>
 * </ul>
 */
final class LogSanitizer {

    static final String MASK = "***";
    private static final Pattern SENSITIVE = Pattern.compile(
            "(?i).*(password|passwd|pwd|token|secret|otp|card|cvv|accountnumber|authorization|credential|apikey).*");
    private static final String PROJECT_PACKAGE = "com.example.smart_booking_system";
    private static final int MAX_ELEMENTS = 5;
    private static final int MAX_DEPTH = 3;
    private static final int MAX_STRING = 200;

    private final int maxLength;
    private final Map<Class<?>, List<Field>> fieldCache = new ConcurrentHashMap<>();

    LogSanitizer(int maxLength) {
        this.maxLength = maxLength;
    }

    String args(String[] names, Object[] values) {
        if (values == null || values.length == 0) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (values[i] instanceof ServletRequest || values[i] instanceof ServletResponse || values[i] instanceof BindingResult) continue;
            if (!sb.isEmpty()) sb.append(", ");
            String name = names != null && i < names.length ? names[i] : "arg" + i;
            sb.append(name).append('=').append(isSensitive(name) ? MASK : render(values[i], 0));
        }
        return truncate(sb.toString(), maxLength);
    }

    String value(Object v) {
        return truncate(render(v, 0), maxLength);
    }

    // ------------------------------------------------------------------ rendering

    private String render(Object v, int depth) {
        try {
            return doRender(v, depth);
        } catch (RuntimeException e) {
            // never let logging break a request
            return v.getClass().getSimpleName() + "(unprintable: " + e.getClass().getSimpleName() + ")";
        }
    }

    private String doRender(Object v, int depth) {
        if (v == null) return "null";
        if (v instanceof CharSequence s) return "\"" + truncate(s.toString(), MAX_STRING) + "\"";
        if (v instanceof Number || v instanceof Boolean || v instanceof Character || v instanceof Enum<?>
                || v instanceof TemporalAccessor || v instanceof UUID) return v.toString();
        if (v instanceof MultipartFile f) return "File(" + f.getOriginalFilename() + ", " + f.getSize() + "B)";
        if (v instanceof byte[] b) return "byte[" + b.length + "]";
        if (v instanceof Optional<?> o) return o.map(x -> "Optional[" + render(x, depth) + "]").orElse("Optional.empty");
        if (v instanceof ResponseEntity<?> re) return "ResponseEntity[" + re.getStatusCode().value() + "] " + render(re.getBody(), depth);
        if (v instanceof Page<?> p) return "Page[" + p.getNumber() + "/" + p.getTotalPages() + ", total=" + p.getTotalElements() + "] "
                + elements(p.getContent(), depth);
        if (v instanceof UserDetails u) return u.getClass().getSimpleName() + "(" + u.getUsername() + ")";
        if (v instanceof Authentication a) return "Authentication(" + a.getName() + ")";
        if (isEntity(v)) return entity(v, depth);
        if (v.getClass().isSynthetic() || v.getClass().getName().contains("$$")) return v.getClass().getSimpleName(); // lambdas, CGLIB proxies
        if (depth >= MAX_DEPTH) return v.getClass().getSimpleName() + "{…}";
        if (v instanceof Collection<?> c) {
            if (!Hibernate.isInitialized(c)) return "(lazy)";
            return v.getClass().getSimpleName() + "(" + c.size() + ")" + elements(c, depth);
        }
        if (v instanceof Map<?, ?> m) return map(m, depth);
        if (v.getClass().isArray()) return array(v, depth);
        if (v.getClass().getName().startsWith(PROJECT_PACKAGE)) return bean(v, depth);
        return plain(v);
    }

    private String elements(Collection<?> c, int depth) {
        StringBuilder sb = new StringBuilder("[");
        Iterator<?> it = c.iterator();
        for (int i = 0; it.hasNext() && i < MAX_ELEMENTS; i++) {
            if (i > 0) sb.append(", ");
            sb.append(render(it.next(), depth + 1));
        }
        if (c.size() > MAX_ELEMENTS) sb.append(", …+").append(c.size() - MAX_ELEMENTS);
        return sb.append(']').toString();
    }

    private String array(Object array, int depth) {
        int length = java.lang.reflect.Array.getLength(array);
        List<Object> list = new ArrayList<>();
        for (int i = 0; i < Math.min(length, MAX_ELEMENTS + 1); i++) list.add(java.lang.reflect.Array.get(array, i));
        return array.getClass().getComponentType().getSimpleName() + "[" + length + "]" + elements(list, depth).replace(", …+1]", ", …]");
    }

    private String map(Map<?, ?> m, int depth) {
        StringBuilder sb = new StringBuilder("{");
        int i = 0;
        for (Map.Entry<?, ?> e : m.entrySet()) {
            if (i == MAX_ELEMENTS * 4) { sb.append(", …+").append(m.size() - i); break; }
            if (i++ > 0) sb.append(", ");
            String key = String.valueOf(e.getKey());
            sb.append(key).append('=').append(isSensitive(key) ? MASK : render(e.getValue(), depth + 1));
        }
        return sb.append('}').toString();
    }

    /** DTOs and other project classes: their fields (secrets masked); nested entities only as Entity#id. */
    private String bean(Object v, int depth) {
        StringBuilder sb = new StringBuilder(v.getClass().getSimpleName()).append('{');
        boolean first = true;
        for (Field f : fields(v.getClass())) {
            if (!first) sb.append(", ");
            sb.append(f.getName()).append('=').append(isSensitive(f.getName()) ? MASK : render(read(f, v), depth + 1));
            first = false;
        }
        return sb.append('}').toString();
    }

    /** Entity column values; relations as Entity#id, collections only when already loaded. */
    private String entity(Object v, int depth) {
        if (v instanceof HibernateProxy proxy && !Hibernate.isInitialized(proxy)) {
            return Hibernate.getClassLazy(proxy).getSimpleName() + "#" + proxy.getHibernateLazyInitializer().getIdentifier();
        }
        Object e = Hibernate.unproxy(v);
        if (depth > 0) return e.getClass().getSimpleName() + "#" + entityId(e);
        StringBuilder sb = new StringBuilder(e.getClass().getSimpleName()).append('{');
        boolean first = true;
        for (Field f : fields(e.getClass())) {
            Object value = read(f, e);
            String text;
            if (isSensitive(f.getName())) text = MASK;
            else if (value instanceof Collection<?> c) text = Hibernate.isInitialized(c) ? "(" + c.size() + ")" : "(lazy)";
            else if (isEntity(value)) text = entity(value, depth + 1);
            else text = render(value, depth + 1);
            if (!first) sb.append(", ");
            sb.append(f.getName()).append('=').append(text);
            first = false;
        }
        return sb.append('}').toString();
    }

    private Object entityId(Object e) {
        for (Field f : fields(e.getClass())) {
            if (f.isAnnotationPresent(Id.class)) return read(f, e);
        }
        return "?";
    }

    private List<Field> fields(Class<?> type) {
        return fieldCache.computeIfAbsent(type, t -> {
            List<Field> list = new ArrayList<>();
            for (Class<?> c = t; c != null && c != Object.class; c = c.getSuperclass()) {
                for (Field f : c.getDeclaredFields()) {
                    if (Modifier.isStatic(f.getModifiers()) || Modifier.isTransient(f.getModifiers())) continue;
                    f.setAccessible(true);
                    list.add(f);
                }
            }
            return list;
        });
    }

    private static Object read(Field f, Object target) {
        try {
            return f.get(target);
        } catch (IllegalAccessException e) {
            return "?";
        }
    }

    private static boolean isEntity(Object v) {
        if (v == null) return false;
        if (v instanceof HibernateProxy) return true;
        return v.getClass().isAnnotationPresent(Entity.class);
    }

    /** Third-party objects: their own toString() if they have one, otherwise the class name. */
    private static String plain(Object v) {
        try {
            if (v.getClass().getMethod("toString").getDeclaringClass() != Object.class) return truncate(v.toString(), MAX_STRING);
        } catch (NoSuchMethodException ignored) {
            // every class has toString
        }
        return v.getClass().getSimpleName();
    }

    private static boolean isSensitive(String name) {
        return name != null && SENSITIVE.matcher(name).matches();
    }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max) + "…(" + s.length() + " chars)";
    }
}
