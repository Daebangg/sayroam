package com.daebangg.sayroam.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "user_profile_image")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserProfileImage {
    @Id
    @Column(name = "user_id")
    private Long userId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "image_id", nullable = false, unique = true, length = 36)
    private String imageId;

    @Lob
    @Column(nullable = false, columnDefinition = "MEDIUMBLOB")
    private byte[] content;

    UserProfileImage(User user, byte[] content) {
        this.user = user;
        replace(content);
    }

    void replace(byte[] content) {
        this.imageId = UUID.randomUUID().toString();
        this.content = content.clone();
    }

    String url() {
        return ProfileImageUrl.UPLOAD_PATH_PREFIX + imageId + ".jpg";
    }
}
