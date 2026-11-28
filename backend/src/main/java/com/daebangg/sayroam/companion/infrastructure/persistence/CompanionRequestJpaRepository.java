package com.daebangg.sayroam.companion.infrastructure.persistence;

import com.daebangg.sayroam.companion.domain.CompanionRequest;
import com.daebangg.sayroam.companion.domain.CompanionRequestRepository;
import com.daebangg.sayroam.companion.domain.CompanionRequestStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

interface CompanionRequestJpaRepository extends CompanionRequestRepository, Repository<CompanionRequest, Long> {
    String FOR_SEARCH = "r.senderSearch.id is not null and (r.senderSearch.id = :searchId or r.receiverSearch.id = :searchId)";

    @Override
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<CompanionRequest> findByFirstSearchIdAndSecondSearchId(long firstSearchId, long secondSearchId);

    @Override
    @Query("select case when r.senderId = :userId then r.receiverId else r.senderId end from CompanionRequest r "
            + "where r.id = :id and r.senderSearch.id is not null and (r.receiverId = :userId or r.senderId = :userId)")
    Optional<Long> findOtherMemberId(long id, long userId);

    @Override
    @Query("select s.id from CompanionSearch s where exists (select r.id from CompanionRequest r "
            + "where r.id = :id and r.senderSearch.id is not null "
            + "and (r.senderId = :userId or r.receiverId = :userId) "
            + "and (r.senderSearch.id = s.id or r.receiverSearch.id = s.id)) order by s.id")
    List<Long> findSearchIdsForMember(long id, long userId);

    @Override
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from CompanionRequest r where r.id = :id and r.senderSearch.id is not null "
            + "and (r.senderId = :userId or r.receiverId = :userId)")
    Optional<CompanionRequest> findOwnedForUpdate(long id, long userId);

    @Override
    @Query("select r from CompanionRequest r join fetch r.senderSearch join fetch r.receiverSearch where "
            + FOR_SEARCH + " and (:status is null or r.status = :status) order by r.id desc")
    List<CompanionRequest> findForSearch(long searchId, CompanionRequestStatus status);

    @Override
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from CompanionRequest r where " + FOR_SEARCH + " and r.status = PENDING order by r.id")
    List<CompanionRequest> findPendingForUpdate(long searchId);

    @Override
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from CompanionRequest r where r.senderSearch.id is not null "
            + "and (r.senderSearch.id in :searchIds or r.receiverSearch.id in :searchIds) "
            + "and r.status = PENDING order by r.id")
    List<CompanionRequest> findPendingForUpdate(List<Long> searchIds);
}
