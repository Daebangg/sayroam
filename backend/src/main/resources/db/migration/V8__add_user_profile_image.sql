CREATE TABLE user_profile_image (
    user_id BIGINT NOT NULL,
    image_id VARCHAR(36) NOT NULL,
    content MEDIUMBLOB NOT NULL,
    PRIMARY KEY (user_id),
    UNIQUE KEY uk_user_profile_image_id (image_id),
    CONSTRAINT fk_user_profile_image_user FOREIGN KEY (user_id) REFERENCES users (id)
);
