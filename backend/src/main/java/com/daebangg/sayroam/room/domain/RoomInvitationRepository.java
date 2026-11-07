package com.daebangg.sayroam.room.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoomInvitationRepository extends JpaRepository<RoomInvitation, Long> {
    Optional<RoomInvitation> findByCode(String code);
}
