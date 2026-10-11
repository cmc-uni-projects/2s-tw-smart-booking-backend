package com.example.smart_booking_system.logging;

import com.example.smart_booking_system.dto.request.auth.LoginRequest;
import com.example.smart_booking_system.entity.Role;
import com.example.smart_booking_system.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class LogSanitizerTest {

    private final LogSanitizer sanitizer = new LogSanitizer(2000);

    @Test
    void masksSecretsByArgumentNameAndFieldName() {
        LoginRequest login = new LoginRequest();
        login.setEmail("a@b.c");
        login.setPassword("hunter2");
        String text = sanitizer.args(new String[]{"request", "resetToken"}, new Object[]{login, "abc"});
        assertThat(text).contains("email=\"a@b.c\"").contains("password=***").contains("resetToken=***").doesNotContain("hunter2", "abc\"");
    }

    @Test
    void masksSecretsInsideMapsAndNestedValues() {
        String text = sanitizer.value(Map.of("accessToken", "jwt", "user", Map.of("name", "An", "passwordHash", "x")));
        assertThat(text).contains("accessToken=***").contains("passwordHash=***").contains("name=\"An\"").doesNotContain("jwt");
    }

    @Test
    void rendersEntityColumnsAndRelationsWithoutLoadingThem() {
        User user = new User();
        user.setEmail("u@travelmate.vn");
        user.setPasswordHash("$2a$secret");
        user.getRoles().add(new Role());
        String text = sanitizer.value(user);
        assertThat(text).startsWith("User{").contains("email=\"u@travelmate.vn\"").contains("passwordHash=***")
                .contains("roles=(1)").doesNotContain("$2a$secret");
    }

    @Test
    void showsCollectionPageAndResponseEntityContents() {
        assertThat(sanitizer.value(List.of(1, 2, 3, 4, 5, 6, 7))).isEqualTo("ListN(7)[1, 2, 3, 4, 5, …+2]");
        assertThat(sanitizer.value(new PageImpl<>(List.of("a")))).startsWith("Page[0/1, total=1] [\"a\"]");
        assertThat(sanitizer.value(ResponseEntity.status(201).body(Map.of("id", 9)))).isEqualTo("ResponseEntity[201] {id=9}");
    }

    @Test
    void truncatesLongOutput() {
        String text = new LogSanitizer(50).value("x".repeat(500));
        assertThat(text).hasSizeLessThan(80).contains("chars)");
    }
}
