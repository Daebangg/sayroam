package com.daebangg.sayroam.user.application.service;

import com.daebangg.sayroam.global.exception.BusinessException;
import com.daebangg.sayroam.global.exception.CommonErrorCode;
import com.daebangg.sayroam.user.application.command.UpdateUserProfileCommand;
import com.daebangg.sayroam.user.application.result.UserProfileResult;
import com.daebangg.sayroam.user.application.result.UserLoginMethodsResult;
import com.daebangg.sayroam.user.domain.entity.User;
import com.daebangg.sayroam.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;

/** 회원 ID와 인증 버전은 요청 본문이 아닌 인증 결과에서 전달한다. */
@Service
@Transactional
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public UserLoginMethodsResult getLoginMethods(long userId, long expectedAuthVersion) {
        User user = requireCurrentUser(userRepository.findByIdAndDeletedAtIsNull(userId), expectedAuthVersion);
        return new UserLoginMethodsResult(userRepository.hasPassword(user.getId()),
                userRepository.findSocialProviders(user.getId()));
    }

    @Transactional(readOnly = true)
    public UserProfileResult getCurrentUser(long userId, long expectedAuthVersion) {
        User user = requireCurrentUser(userRepository.findByIdAndDeletedAtIsNull(userId),
                expectedAuthVersion);
        return UserProfileResult.from(user);
    }

    public UserProfileResult updateProfile(long userId, long expectedAuthVersion,
                                          UpdateUserProfileCommand command) {
        Objects.requireNonNull(command, "command");
        User user = requireCurrentUser(userRepository.findActiveByIdForUpdate(userId),
                expectedAuthVersion);

        if (command.nickname().present()) {
            user.changeNickname(command.nickname().value());
        }
        if (command.profileImageUrl().present()) {
            user.changeProfileImageUrl(command.profileImageUrl().value());
        }
        if (command.gender().present()) {
            user.changeGender(command.gender().value());
        }
        if (command.birthYear().present()) {
            user.changeBirthYear(command.birthYear().value());
        }

        return UserProfileResult.from(user);
    }

    private static User requireCurrentUser(Optional<User> result, long expectedAuthVersion) {
        User user = result.orElseThrow(() -> new BusinessException(CommonErrorCode.UNAUTHENTICATED));
        if (user.getAuthVersion() != expectedAuthVersion) {
            throw new BusinessException(CommonErrorCode.UNAUTHENTICATED);
        }
        return user;
    }
}
