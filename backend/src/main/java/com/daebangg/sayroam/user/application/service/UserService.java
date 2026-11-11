package com.daebangg.sayroam.user.application.service;

import com.daebangg.sayroam.global.exception.BusinessException;
import com.daebangg.sayroam.global.exception.CommonErrorCode;
import com.daebangg.sayroam.user.application.dto.UpdateUserProfileCommand;
import com.daebangg.sayroam.user.application.dto.UserLoginMethodsResult;
import com.daebangg.sayroam.user.application.dto.UserProfileResult;
import com.daebangg.sayroam.user.domain.User;
import com.daebangg.sayroam.user.domain.error.UserErrorCode;
import com.daebangg.sayroam.user.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Base64;
import java.util.Iterator;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** 회원 ID와 인증 버전은 요청 본문이 아닌 인증 결과에서 전달한다. */
@Service
@RequiredArgsConstructor
public class UserService {
    private static final int MAX_IMAGE_BYTES = 2 * 1024 * 1024;
    private static final int PROFILE_IMAGE_SIZE = 512;
    private static final Set<String> IMAGE_FORMATS = Set.of("jpeg", "jpg", "png");

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

    @Transactional
    public UserProfileResult updateProfile(long userId, long expectedAuthVersion,
                                          UpdateUserProfileCommand command) {
        Objects.requireNonNull(command, "command");
        User user = requireCurrentUser(userRepository.findForUpdateByIdAndDeletedAtIsNull(userId),
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
            user.changeBirthYear(command.birthYear().value(), LocalDate.now());
        }
        if (command.birthDate().present()) {
            user.changeBirthDate(command.birthDate().value(), LocalDate.now());
        }
        return UserProfileResult.from(user);
    }

    @Transactional(readOnly = true)
    public Optional<byte[]> getProfileImage(String imageId) {
        return userRepository.findActiveProfileImageContent(imageId);
    }

    @Transactional
    public UserProfileResult uploadProfileImage(long userId, long expectedAuthVersion, String dataUrl) {
        byte[] content = processProfileImage(dataUrl);
        User user = requireCurrentUser(userRepository.findForUpdateByIdAndDeletedAtIsNull(userId), expectedAuthVersion);
        user.uploadProfileImage(content);
        return UserProfileResult.from(user);
    }

    /** JPEG·PNG의 실제 형식을 검사하고 메타데이터 없이 정사각형 JPEG로 변환한다. */
    private static byte[] processProfileImage(String dataUrl) {
        if (dataUrl == null || dataUrl.length() > 2_800_000
                || !(dataUrl.startsWith("data:image/jpeg;base64,") || dataUrl.startsWith("data:image/png;base64,"))) {
            throw invalidImage();
        }
        try {
            byte[] bytes = Base64.getDecoder().decode(dataUrl.substring(dataUrl.indexOf(',') + 1));
            if (bytes.length == 0 || bytes.length > MAX_IMAGE_BYTES) throw invalidImage();
            try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
                Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
                if (!readers.hasNext()) throw invalidImage();
                ImageReader reader = readers.next();
                try {
                    reader.setInput(input, true, true);
                    int width = reader.getWidth(0);
                    int height = reader.getHeight(0);
                    if (!IMAGE_FORMATS.contains(reader.getFormatName().toLowerCase(Locale.ROOT))
                            || width < 1 || height < 1 || width > 4096 || height > 4096) throw invalidImage();
                    BufferedImage original = reader.read(0);
                    BufferedImage resized = new BufferedImage(PROFILE_IMAGE_SIZE, PROFILE_IMAGE_SIZE, BufferedImage.TYPE_INT_RGB);
                    Graphics2D graphics = resized.createGraphics();
                    try {
                        graphics.setColor(Color.WHITE);
                        graphics.fillRect(0, 0, PROFILE_IMAGE_SIZE, PROFILE_IMAGE_SIZE);
                        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                        int side = Math.min(width, height);
                        int x = (width - side) / 2;
                        int y = (height - side) / 2;
                        graphics.drawImage(original, 0, 0, PROFILE_IMAGE_SIZE, PROFILE_IMAGE_SIZE, x, y, x + side, y + side, null);
                    } finally {
                        graphics.dispose();
                    }
                    ByteArrayOutputStream output = new ByteArrayOutputStream();
                    if (!ImageIO.write(resized, "jpeg", output)) throw invalidImage();
                    return output.toByteArray();
                } finally {
                    reader.dispose();
                }
            }
        } catch (IOException | IllegalArgumentException exception) {
            throw new BusinessException(UserErrorCode.USER_PROFILE_IMAGE_INVALID, exception);
        }
    }

    private static BusinessException invalidImage() {
        return new BusinessException(UserErrorCode.USER_PROFILE_IMAGE_INVALID);
    }

    private static User requireCurrentUser(Optional<User> result, long expectedAuthVersion) {
        User user = result.orElseThrow(() -> new BusinessException(CommonErrorCode.UNAUTHENTICATED));
        if (!user.isActiveWithAuthVersion(expectedAuthVersion)) {
            throw new BusinessException(CommonErrorCode.UNAUTHENTICATED);
        }
        return user;
    }
}
