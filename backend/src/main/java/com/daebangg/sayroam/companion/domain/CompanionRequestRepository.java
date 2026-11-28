package com.daebangg.sayroam.companion.domain;

import java.util.List;
import java.util.Optional;

public interface CompanionRequestRepository {
    <S extends CompanionRequest> S save(S request);
    Optional<CompanionRequest> findByFirstSearchIdAndSecondSearchId(long firstSearchId, long secondSearchId);
    Optional<Long> findOtherMemberId(long id, long userId);
    List<Long> findSearchIdsForMember(long id, long userId);
    Optional<CompanionRequest> findOwnedForUpdate(long id, long userId);
    List<CompanionRequest> findForSearch(long searchId, CompanionRequestStatus status);
    List<CompanionRequest> findPendingForUpdate(long searchId);
    List<CompanionRequest> findPendingForUpdate(List<Long> searchIds);
}
