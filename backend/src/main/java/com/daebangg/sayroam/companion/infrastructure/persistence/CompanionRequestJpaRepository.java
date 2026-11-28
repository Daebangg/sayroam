package com.daebangg.sayroam.companion.infrastructure.persistence;

import com.daebangg.sayroam.companion.domain.CompanionRequest;
import com.daebangg.sayroam.companion.domain.CompanionRequestRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

interface CompanionRequestJpaRepository extends CompanionRequestRepository, Repository<CompanionRequest, Long> {
    String FOR_SEARCH = "r.senderSearch.id is not null and (r.senderSearch.id = :searchId or r.receiverSearch.id = :searchId)";

    @Override
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from CompanionRequest r where r.firstSearchId = :firstSearchId and r.secondSearchId = :secondSearchId")
    Optional<CompanionRequest> findBySearchPairForUpdate(@Param("firstSearchId") long firstSearchId,
                                                       @Param("secondSearchId") long secondSearchId);

    @Override
    @Query("select case when r.senderId = :userId then r.receiverId else r.senderId end from CompanionRequest r "
            + "where r.id = :requestId and r.senderSearch.id is not null and (r.receiverId = :userId or r.senderId = :userId)")
    Optional<Long> findOtherMemberId(@Param("userId") long userId, @Param("requestId") long requestId);

    @Override
    @Query("select s.id from CompanionSearch s where exists (select r.id from CompanionRequest r "
            + "where r.id = :requestId and r.senderSearch.id is not null "
            + "and (r.senderId = :userId or r.receiverId = :userId) "
            + "and (r.senderSearch.id = s.id or r.receiverSearch.id = s.id)) order by s.id")
    List<Long> findSearchIdsForMember(@Param("userId") long userId, @Param("requestId") long requestId);

    @Override
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from CompanionRequest r where r.id = :requestId and r.senderSearch.id is not null "
            + "and (r.senderId = :userId or r.receiverId = :userId)")
    Optional<CompanionRequest> findAccessibleForUpdate(@Param("userId") long userId, @Param("requestId") long requestId);

    @Override
    @Query("select r from CompanionRequest r join fetch r.senderSearch join fetch r.receiverSearch "
            + "where r.id = :requestId and (r.senderId = :userId or r.receiverId = :userId)")
    Optional<CompanionRequest> findAccessible(@Param("userId") long userId, @Param("requestId") long requestId);

    @Override
    @Query("select r from CompanionRequest r join fetch r.senderSearch join fetch r.receiverSearch where "
            + FOR_SEARCH + " order by r.id desc")
    List<CompanionRequest> findForSearch(@Param("searchId") long searchId);

    @Override
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from CompanionRequest r where " + FOR_SEARCH + " and r.status = PENDING order by r.id")
    List<CompanionRequest> findPendingForUpdate(@Param("searchId") long searchId);

    @Override
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from CompanionRequest r where r.senderSearch.id is not null "
            + "and (r.senderSearch.id in :searchIds or r.receiverSearch.id in :searchIds) "
            + "and r.status = PENDING order by r.id")
    List<CompanionRequest> findPendingForUpdate(@Param("searchIds") List<Long> searchIds);
}
