package com.daebangg.sayroam.trip.infrastructure.persistence;

import com.daebangg.sayroam.trip.application.dto.TripPlaceResult;
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
            select new com.daebangg.sayroam.trip.application.dto.TripSummaryResult(
                       t.id, t.title, t.period, t.roomId,
                       d.id, d.name, d.boundsSouth, d.boundsWest, d.boundsNorth, d.boundsEast)
            from Trip t
            join Destination d on d.id = t.destinationId
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
            select new com.daebangg.sayroam.trip.application.dto.TripSummaryResult(
                       t.id, t.title, t.period, t.roomId,
                       d.id, d.name, d.boundsSouth, d.boundsWest, d.boundsNorth, d.boundsEast)
            from Trip t
            join Destination d on d.id = t.destinationId
            where t.id = :tripId
              and ((t.roomId is null and t.createdById = :userId)
                   or t.roomId in (select m.room.id
                                   from RoomMember m
                                   where m.userId = :userId
                                     and m.status = com.daebangg.sayroam.room.domain.RoomMemberStatus.JOINED))
            """)
    Optional<TripSummaryResult> findReadable(@Param("userId") Long userId, @Param("tripId") Long tripId);

    @Override
    @Query("""
            select new com.daebangg.sayroam.trip.application.dto.TripPlaceResult(
                       tp.id, p.source, p.name.value, p.category, p.address,
                       p.coordinate.latitude, p.coordinate.longitude, tp.dayNumber, tp.memo, tp.addedById)
            from TripPlace tp
            join Place p on p.id = tp.placeId
            where tp.trip.id = :tripId
            order by case when tp.dayNumber is null then 1 else 0 end, tp.dayNumber, tp.id
            """)
    List<TripPlaceResult> findPlaces(@Param("tripId") Long tripId);
}
