package com.daebangg.sayroam.user.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "JPEG 또는 PNG의 Base64 data URL. 디코딩 후 최대 2MB, 가로·세로 최대 4096px.")
public record UploadProfileImageRequest(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 2800000)
        String dataUrl) {
}
