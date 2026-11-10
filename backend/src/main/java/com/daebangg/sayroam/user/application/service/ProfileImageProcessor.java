package com.daebangg.sayroam.user.application.service;

import com.daebangg.sayroam.global.exception.BusinessException;
import com.daebangg.sayroam.user.domain.error.UserErrorCode;
import org.springframework.stereotype.Component;

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
import java.util.Base64;
import java.util.Iterator;
import java.util.Set;

/** 파일 확장자 대신 실제 형식을 확인하고, 메타데이터를 제외한 정사각형 JPEG만 저장한다. */
@Component
public class ProfileImageProcessor {
    private static final int MAX_BYTES = 2 * 1024 * 1024;
    private static final int SIZE = 512;
    private static final Set<String> FORMATS = Set.of("jpeg", "jpg", "png");

    public byte[] process(String dataUrl) {
        if (dataUrl == null || dataUrl.length() > 2_800_000
                || !(dataUrl.startsWith("data:image/jpeg;base64,") || dataUrl.startsWith("data:image/png;base64,"))) {
            throw invalid();
        }
        try {
            byte[] bytes = Base64.getDecoder().decode(dataUrl.substring(dataUrl.indexOf(',') + 1));
            if (bytes.length == 0 || bytes.length > MAX_BYTES) throw invalid();
            try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
                Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
                if (!readers.hasNext()) throw invalid();
                ImageReader reader = readers.next();
                try {
                    reader.setInput(input, true, true);
                    int width = reader.getWidth(0);
                    int height = reader.getHeight(0);
                    if (!FORMATS.contains(reader.getFormatName().toLowerCase(java.util.Locale.ROOT))
                            || width < 1 || height < 1 || width > 4096 || height > 4096) throw invalid();
                    BufferedImage original = reader.read(0);
                    BufferedImage resized = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_RGB);
                    Graphics2D graphics = resized.createGraphics();
                    try {
                        graphics.setColor(Color.WHITE);
                        graphics.fillRect(0, 0, SIZE, SIZE);
                        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                        int side = Math.min(width, height);
                        int x = (width - side) / 2;
                        int y = (height - side) / 2;
                        graphics.drawImage(original, 0, 0, SIZE, SIZE, x, y, x + side, y + side, null);
                    } finally {
                        graphics.dispose();
                    }
                    ByteArrayOutputStream output = new ByteArrayOutputStream();
                    if (!ImageIO.write(resized, "jpeg", output)) throw invalid();
                    return output.toByteArray();
                } finally {
                    reader.dispose();
                }
            }
        } catch (IOException | IllegalArgumentException exception) {
            throw new BusinessException(UserErrorCode.USER_PROFILE_IMAGE_INVALID, exception);
        }
    }

    private BusinessException invalid() {
        return new BusinessException(UserErrorCode.USER_PROFILE_IMAGE_INVALID);
    }
}
