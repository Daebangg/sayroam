package com.daebangg.sayroam.user.presentation.controller;

import com.daebangg.sayroam.user.application.service.UserService;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** 프로필 공개 이미지 리소스. API의 JSON 계약과 분리한다. */
@Hidden
@RestController
@RequiredArgsConstructor
public class ProfileImageController {
    private final UserService users;

    @GetMapping(value = "/images/profiles/{imageId}.jpg", produces = MediaType.IMAGE_JPEG_VALUE)
    public ResponseEntity<byte[]> image(@PathVariable String imageId) {
        return users.getProfileImage(imageId)
                .map(image -> ResponseEntity.ok().cacheControl(CacheControl.noStore())
                        .contentType(MediaType.IMAGE_JPEG).body(image))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
