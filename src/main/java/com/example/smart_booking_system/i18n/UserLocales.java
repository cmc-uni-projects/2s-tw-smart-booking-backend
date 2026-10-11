package com.example.smart_booking_system.i18n;

import com.example.smart_booking_system.entity.User;
import com.example.smart_booking_system.repository.UserRepository;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Optional;

/** Locale of a user (users.preferredLanguage), used where there is no request: emails, jobs, notifications. */
@Component
public class UserLocales {

    private final UserRepository userRepository;

    public UserLocales(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Locale of(User user) {
        return user == null ? I18nConfig.VIETNAMESE : I18nConfig.toSupported(user.getPreferredLanguage());
    }

    /** Locale of the account owning this address, empty when no account uses it. */
    public Optional<Locale> ofAccount(String email) {
        return email == null ? Optional.empty() : userRepository.findByEmail(email).map(this::of);
    }

    /** Locale of whoever owns this address (account email); Vietnamese if unknown (e.g. a guest's contact email). */
    public Locale forEmail(String email) {
        if (email == null) return I18nConfig.VIETNAMESE;
        return userRepository.findByEmail(email).map(this::of).orElse(I18nConfig.VIETNAMESE);
    }
}
