package com.daebangg.sayroam.user.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "user_profile_image")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserProfileImage {
    @Getter
    @Id
    @Column(name = "user_id")
    private Long userId;

    @Getter
    @Column(name = "image_id", nullable = false, unique = true, length = 36)
    private String imageId;

    @Getter
    @Lob
    @Column(nullable = false, columnDefinition = "MEDIUMBLOB")
    private byte[] content;

    public UserProfileImage(long userId, byte[] content) {
        this.userId = userId;
        replace(content);
    }

    public void replace(byte[] content) {
        this.imageId = UUID.randomUUID().toString();
        this.content = content.clone();
    }

    public String url() {
        return "/images/profiles/" + imageId + ".jpg";
    }
}
