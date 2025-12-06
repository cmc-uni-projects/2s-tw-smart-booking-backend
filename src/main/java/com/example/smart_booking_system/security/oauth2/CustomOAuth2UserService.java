package com.example.smart_booking_system.security.oauth2;

import com.example.smart_booking_system.entity.Role;
import com.example.smart_booking_system.entity.SocialAccount; // Import mới
import com.example.smart_booking_system.entity.User;
import com.example.smart_booking_system.entity.UserDetail;
import com.example.smart_booking_system.enums.AuthProvider;
import com.example.smart_booking_system.repository.RoleRepository;
import com.example.smart_booking_system.repository.SocialAccountRepository; // Import mới
import com.example.smart_booking_system.repository.UserRepository;
import com.example.smart_booking_system.security.CustomUserDetails;
import com.example.smart_booking_system.security.oauth2.user.FacebookOAuth2UserInfo;
import com.example.smart_booking_system.security.oauth2.user.GoogleOAuth2UserInfo;
import com.example.smart_booking_system.security.oauth2.user.OAuth2UserInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private SocialAccountRepository socialAccountRepository; // Inject Repo mới

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest oAuth2UserRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(oAuth2UserRequest);
        try {
            return processOAuth2User(oAuth2UserRequest, oAuth2User);
        } catch (Exception ex) {
            throw new InternalAuthenticationServiceException(ex.getMessage(), ex.getCause());
        }
    }

    private OAuth2User processOAuth2User(OAuth2UserRequest oAuth2UserRequest, OAuth2User oAuth2User) {
        String registrationId = oAuth2UserRequest.getClientRegistration().getRegistrationId();
        AuthProvider provider = AuthProvider.valueOf(registrationId.toLowerCase());

        OAuth2UserInfo oAuth2UserInfo = getOAuth2UserInfo(registrationId, oAuth2User.getAttributes());

        // 1. Kiểm tra xem SocialAccount này đã tồn tại chưa
        Optional<SocialAccount> socialAccountOptional =
                socialAccountRepository.findByProviderAndProviderId(provider, oAuth2UserInfo.getId());

        User user;
        if (socialAccountOptional.isPresent()) {
            // Case 1: Đã từng login bằng tài khoản MXH này -> Lấy user gốc ra
            user = socialAccountOptional.get().getUser();
            user = updateExistingUser(user, oAuth2UserInfo);
        } else {
            // Case 2: Chưa từng login bằng MXH này -> Tìm xem có User nào dùng chung email không
            String email = oAuth2UserInfo.getEmail();

            // Xử lý email tạm nếu thiếu (giữ logic cũ của bạn)
            if (!StringUtils.hasText(email)) {
                email = oAuth2UserInfo.getId() + "@temp." + registrationId + ".com";
            }

            Optional<User> userOptional = userRepository.findByEmail(email);

            if (userOptional.isPresent()) {
                // Case 2a: Email đã tồn tại (User gốc) -> LIÊN KẾT (LINKING)
                user = userOptional.get();
                linkSocialAccount(user, provider, oAuth2UserInfo);
            } else {
                // Case 2b: User hoàn toàn mới -> Tạo User mới + Link Social
                user = registerNewUser(oAuth2UserRequest, oAuth2UserInfo, email);
                linkSocialAccount(user, provider, oAuth2UserInfo);
            }
        }

        return CustomUserDetails.create(user, oAuth2User.getAttributes());
    }

    // Hàm tạo liên kết mới
    private void linkSocialAccount(User user, AuthProvider provider, OAuth2UserInfo oAuth2UserInfo) {
        SocialAccount socialAccount = new SocialAccount();
        socialAccount.setProvider(provider);
        socialAccount.setProviderId(oAuth2UserInfo.getId());
        socialAccount.setEmail(oAuth2UserInfo.getEmail());
        socialAccount.setName(oAuth2UserInfo.getName());

        // Liên kết 2 chiều
        user.addSocialAccount(socialAccount);

        // Lưu
        socialAccountRepository.save(socialAccount);
    }

    // Các hàm helper khác giữ nguyên logic cũ, chỉ rút gọn
    private OAuth2UserInfo getOAuth2UserInfo(String registrationId, java.util.Map<String, Object> attributes) {
        if (registrationId.equalsIgnoreCase(AuthProvider.google.toString())) {
            return new GoogleOAuth2UserInfo(attributes);
        } else if (registrationId.equalsIgnoreCase(AuthProvider.facebook.toString())) {
            return new FacebookOAuth2UserInfo(attributes);
        } else {
            throw new OAuth2AuthenticationException("Login with " + registrationId + " is not supported yet.");
        }
    }

    private User registerNewUser(OAuth2UserRequest oAuth2UserRequest, OAuth2UserInfo oAuth2UserInfo, String email) {
        User user = new User();
        user.setProvider(AuthProvider.valueOf(oAuth2UserRequest.getClientRegistration().getRegistrationId())); // Set provider chính
        user.setProviderId(oAuth2UserInfo.getId());
        user.setFullName(oAuth2UserInfo.getName());
        user.setEmail(email);
        user.setIsEmailVerified(true);

        if (email.contains("@temp.") && email.endsWith(".com")) {
            user.setStatus("PENDING_EMAIL");
        } else {
            user.setStatus("ACTIVE");
        }

        user.setPasswordHash(UUID.randomUUID().toString());

        Role userRole = roleRepository.findByRoleName("CUSTOMER")
                .orElseThrow(() -> new InternalAuthenticationServiceException("Role 'CUSTOMER' not set."));
        user.addRole(userRole);

        UserDetail userDetail = new UserDetail();
        userDetail.setProfilePhotoUrl(oAuth2UserInfo.getImageUrl());
        userDetail.setUser(user);
        user.setUserDetail(userDetail);

        return userRepository.save(user);
    }

    private User updateExistingUser(User existingUser, OAuth2UserInfo oAuth2UserInfo) {
        // Cập nhật thông tin nếu cần (tùy chọn)
        return userRepository.save(existingUser);
    }
}