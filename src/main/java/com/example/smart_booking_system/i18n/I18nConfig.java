package com.example.smart_booking_system.i18n;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import java.util.List;
import java.util.Locale;

/**
 * Request language = Accept-Language header (vi | en), default Vietnamese.
 * Texts live in resources/i18n/messages*.properties (API/notifications/validation) and
 * resources/i18n/emails*.properties (email templates); see spring.messages.basename.
 */
@Configuration
public class I18nConfig {

    public static final Locale VIETNAMESE = Locale.forLanguageTag("vi");
    public static final List<Locale> SUPPORTED = List.of(VIETNAMESE, Locale.ENGLISH);

    @Bean
    public LocaleResolver localeResolver() {
        AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
        resolver.setSupportedLocales(SUPPORTED);
        resolver.setDefaultLocale(VIETNAMESE);
        return resolver;
    }

    /** "en", "en-US", "vi"… -> a supported locale (falls back to Vietnamese). */
    public static Locale toSupported(String languageTag) {
        if (languageTag == null || languageTag.isBlank()) return VIETNAMESE;
        String lang = Locale.forLanguageTag(languageTag.trim()).getLanguage();
        return SUPPORTED.stream().filter(l -> l.getLanguage().equals(lang)).findFirst().orElse(VIETNAMESE);
    }
}
