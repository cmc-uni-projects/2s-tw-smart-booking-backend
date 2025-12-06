package com.example.smart_booking_system.security.oauth2;

import com.example.smart_booking_system.entity.Role;
import com.example.smart_booking_system.entity.User;
import com.example.smart_booking_system.entity.UserDetail;
import com.example.smart_booking_system.enums.AuthProvider;
import com.example.smart_booking_system.repository.RoleRepository;
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

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest oAuth2UserRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(oAuth2UserRequest);

        try {
            return processOAuth2User(oAuth2UserRequest, oAuth2User);
        } catch (Exception ex) {
            // Throwing an instance of AuthenticationException will trigger the OAuth2AuthenticationFailureHandler
            throw new InternalAuthenticationServiceException(ex.getMessage(), ex.getCause());
        }
    }

    private OAuth2User processOAuth2User(OAuth2UserRequest oAuth2UserRequest, OAuth2User oAuth2User) {
        String registrationId = oAuth2UserRequest.getClientRegistration().getRegistrationId();
        OAuth2UserInfo oAuth2UserInfo;

        if (registrationId.equalsIgnoreCase(AuthProvider.google.toString())) {
            oAuth2UserInfo = new GoogleOAuth2UserInfo(oAuth2User.getAttributes());
        } else if (registrationId.equalsIgnoreCase(AuthProvider.facebook.toString())) {
            oAuth2UserInfo = new FacebookOAuth2UserInfo(oAuth2User.getAttributes());
        } else {
            throw new OAuth2AuthenticationException("Login with " + registrationId + " is not supported yet.");
        }

        if (StringUtils.isEmpty(oAuth2UserInfo.getEmail())) {
            throw new OAuth2AuthenticationException("Email not found from OAuth2 provider");
        }

        Optional<User> userOptional = userRepository.findByEmail(oAuth2UserInfo.getEmail());
        User user;
        if (userOptional.isPresent()) {
            user = userOptional.get();
            if (!user.getProvider().equals(AuthProvider.valueOf(registrationId))) {
                throw new OAuth2AuthenticationException("Looks like you're signed up with " +
                        user.getProvider() + " account. Please use your " + user.getProvider() +
                        " account to login.");
            }
            user = updateExistingUser(user, oAuth2UserInfo);
        } else {
            user = registerNewUser(oAuth2UserRequest, oAuth2UserInfo);
        }

        // Sửa lỗi: Sử dụng phương thức static create() thay vì constructor
        return CustomUserDetails.create(user, oAuth2User.getAttributes());
    }

    private User registerNewUser(OAuth2UserRequest oAuth2UserRequest, OAuth2UserInfo oAuth2UserInfo) {
        User user = new User();

        user.setProvider(AuthProvider.valueOf(oAuth2UserRequest.getClientRegistration().getRegistrationId()));
        user.setProviderId(oAuth2UserInfo.getId());
        user.setFullName(oAuth2UserInfo.getName());
        user.setEmail(oAuth2UserInfo.getEmail());
        user.setIsEmailVerified(true); // Đăng nhập Google/FB thì coi như đã verify email
        user.setStatus("ACTIVE");

        // Vì password not null, ta set một chuỗi random (người dùng này sẽ không đăng nhập bằng pass được)
        user.setPasswordHash(UUID.randomUUID().toString());

        // 1. Xử lý Role: Tìm role CUSTOMER từ DB
        Role userRole = roleRepository.findByRoleName("CUSTOMER")
                .orElseThrow(() -> new InternalAuthenticationServiceException("Role 'CUSTOMER' not set."));
        user.addRole(userRole);

        // 2. Xử lý UserDetail để lưu ảnh
        UserDetail userDetail = new UserDetail();
        userDetail.setProfilePhotoUrl(oAuth2UserInfo.getImageUrl());
        userDetail.setUser(user);

        // Liên kết 2 chiều
        user.setUserDetail(userDetail);

        return userRepository.save(user);
    }

    private User updateExistingUser(User existingUser, OAuth2UserInfo oAuth2UserInfo) {
        existingUser.setFullName(oAuth2UserInfo.getName());

        // Cập nhật ảnh trong UserDetail
        UserDetail userDetail = existingUser.getUserDetail();
        if (userDetail == null) {
            userDetail = new UserDetail();
            userDetail.setUser(existingUser);
            existingUser.setUserDetail(userDetail);
        }
        userDetail.setProfilePhotoUrl(oAuth2UserInfo.getImageUrl());

        return userRepository.save(existingUser);
    }
}