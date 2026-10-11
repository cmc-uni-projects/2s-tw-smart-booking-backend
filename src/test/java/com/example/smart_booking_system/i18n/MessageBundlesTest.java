package com.example.smart_booking_system.i18n;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/** Every bundle has the same keys in each language, and every value is valid MessageFormat (always-use-message-format=true). */
class MessageBundlesTest {

    private static Properties load(String path) throws IOException {
        Properties p = new Properties();
        try (var in = MessageBundlesTest.class.getClassLoader().getResourceAsStream(path)) {
            assertThat(in).as(path).isNotNull();
            p.load(new InputStreamReader(in, StandardCharsets.UTF_8));
        }
        return p;
    }

    @ParameterizedTest
    @ValueSource(strings = {"i18n/messages", "i18n/emails"})
    void languagesHaveTheSameKeys(String base) throws IOException {
        Properties vi = load(base + ".properties");
        Properties en = load(base + "_en.properties");
        assertThat(en.stringPropertyNames()).containsExactlyInAnyOrderElementsOf(vi.stringPropertyNames());
    }

    @ParameterizedTest
    @ValueSource(strings = {"i18n/messages.properties", "i18n/messages_en.properties", "i18n/emails.properties", "i18n/emails_en.properties"})
    void valuesAreValidMessageFormat(String path) throws IOException {
        Properties p = load(path);
        for (String key : p.stringPropertyNames()) {
            assertThatCode(() -> new MessageFormat(p.getProperty(key), Locale.ENGLISH)).as(path + " " + key).doesNotThrowAnyException();
        }
    }
}
