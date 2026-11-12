package com.daebangg.sayroam.trip.infrastructure.persistence;

import com.daebangg.sayroam.trip.application.dto.TripSummaryResult;
import com.daebangg.sayroam.trip.application.port.out.TripQueryRepository;
import com.daebangg.sayroam.trip.domain.Trip;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TripQueryJpaRepository extends TripQueryRepository, Repository<Trip, Long> {
    @Override
    @Query("""
            select new com.daebangg.sayroam.trip.application.dto.TripSummaryResult(t.id, t.title, t.period, t.roomId)
            from Trip t
            where (t.roomId is null and t.createdById = :userId)
               or t.roomId in (select m.room.id
                               from RoomMember m
                               where m.userId = :userId
                                 and m.status = com.daebangg.sayroam.room.domain.RoomMemberStatus.JOINED)
            order by t.id desc
            """)
    List<TripSummaryResult> findAllReadable(@Param("userId") Long userId);

    @Override
    @Query("""
            select new com.daebangg.sayroam.trip.application.dto.TripSummaryResult(t.id, t.title, t.period, t.roomId)
            from Trip t
            where t.id = :tripId
              and ((t.roomId is null and t.createdById = :userId)
                   or t.roomId in (select m.room.id
                                   from RoomMember m
                                   where m.userId = :userId
                                     and m.status = com.daebangg.sayroam.room.domain.RoomMemberStatus.JOINED))
            """)
    Optional<TripSummaryResult> findReadable(@Param("userId") Long userId, @Param("tripId") Long tripId);
}
