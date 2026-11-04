package com.daebangg.sayroam.room.domain;

import com.daebangg.sayroam.global.entity.BaseTimeEntity;
import com.daebangg.sayroam.global.exception.BusinessException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "room")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Room extends BaseTimeEntity {

    private static final int MAX_NAME_LENGTH = 20;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 1:1 방은 NULL로 두고 상대 닉네임을 보여준다
    @Column(length = 20)
    private String name;

    public Room(String name) {
        if (name == null) {
            this.name = null;
            return;
        }

        String trimmed = name.strip();

        if (trimmed.codePointCount(0, trimmed.length()) > MAX_NAME_LENGTH) {
            throw new BusinessException(RoomErrorCode.ROOM_NAME_TOO_LONG);
        }

        this.name = name.isBlank() ? null : trimmed;
    }

    public Long getId() {
        return id;
    }

    public boolean hasName() {
        return name != null;
    }

    public String getName() {
        return name;
    }
}
