package com.daebangg.sayroam.room.domain;

import com.daebangg.sayroam.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "room")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Room extends BaseTimeEntity {

    @Getter
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Getter
    @Enumerated(EnumType.STRING)
    @Column(name = "room_type", nullable = false, updatable = false, length = 20)
    private RoomType type;

    private Room(RoomType type) {
        this.type = type;
    }

    public static Room createForTrip() {
        return new Room(RoomType.TRIP);
    }

    public static Room createForCompanion() {
        return new Room(RoomType.COMPANION);
    }
}
