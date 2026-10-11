package com.example.smart_booking_system.i18n;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/** Renders every email template in each supported language with the same message setup as application.properties. */
class EmailTemplatesTest {

    private static final Pattern VIETNAMESE = Pattern.compile("[À-ỹđĐ]");
    // Values that are data, not template text (shown verbatim in any language).
    private static final Map<String, Object> VARIABLES = Map.ofEntries(
            Map.entry("username", "Alice <b>"), Map.entry("name", "Alice"), Map.entry("ownerName", "Alice"),
            Map.entry("applicantName", "Alice"), Map.entry("bookingId", "42"), Map.entry("propertyName", "Sea View"),
            Map.entry("roomName", "Deluxe"), Map.entry("reason", "Test reason"), Map.entry("totalPrice", "1,000,000"),
            Map.entry("penaltyAmount", "0"), Map.entry("refundAmount", "1,000,000"), Map.entry("checkInDate", "2026-10-12"),
            Map.entry("checkOutDate", "2026-10-13"), Map.entry("applicationId", "7"), Map.entry("submittedAt", "2026-10-11"),
            Map.entry("verificationUrl", "http://x/verify"), Map.entry("resetPasswordUrl", "http://x/reset"),
            Map.entry("bookingUrl", "http://x/b"), Map.entry("reviewUrl", "http://x/r"), Map.entry("contactUrl", "http://x/c"),
            Map.entry("helpUrl", "http://x/help"), Map.entry("loginUrl", "http://x/login"), Map.entry("dashboardUrl", "http://x/d"));

    private static final SpringTemplateEngine ENGINE = engine();

    private static SpringTemplateEngine engine() {
        ResourceBundleMessageSource messages = new ResourceBundleMessageSource();
        messages.setBasenames("i18n/messages", "i18n/emails");
        messages.setDefaultEncoding("UTF-8");
        messages.setFallbackToSystemLocale(false);
        messages.setAlwaysUseMessageFormat(true);

        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setCharacterEncoding("UTF-8");

        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        engine.setTemplateEngineMessageSource(messages);
        return engine;
    }

    static Stream<String> templates() throws IOException {
        return Arrays.stream(new PathMatchingResourcePatternResolver().getResources("classpath:templates/email/*.html"))
                .map(r -> "email/" + r.getFilename().replace(".html", ""));
    }

    @ParameterizedTest
    @MethodSource("templates")
    void rendersInVietnamese(String template) {
        String html = ENGINE.process(template, new Context(I18nConfig.VIETNAMESE, VARIABLES));
        assertThat(html).doesNotContain("??").doesNotContain("Alice <b>");
    }

    @ParameterizedTest
    @MethodSource("templates")
    void rendersInEnglishWithoutVietnameseText(String template) {
        String html = ENGINE.process(template, new Context(Locale.ENGLISH, VARIABLES));
        assertThat(html).doesNotContain("??").doesNotContain("Alice <b>");
        String visible = html.replaceAll("(?s)<style.*?</style>", "").replaceAll("<[^>]*>", " ");
        assertThat(VIETNAMESE.matcher(visible).find()).as("Vietnamese text in English %s:%n%s", template, visible).isFalse();
    }
}
