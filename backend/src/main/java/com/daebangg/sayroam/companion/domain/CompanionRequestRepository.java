package com.daebangg.sayroam.companion.domain;

import java.util.List;
import java.util.Optional;

public interface CompanionRequestRepository {
    <S extends CompanionRequest> S save(S request);

    Optional<CompanionRequest> findBySearchPairForUpdate(long firstSearchId, long secondSearchId);

    Optional<Long> findOtherMemberId(long userId, long requestId);

    List<Long> findSearchIdsForMember(long userId, long requestId);

    Optional<CompanionRequest> findAccessibleForUpdate(long userId, long requestId);

    Optional<CompanionRequest> findAccessible(long userId, long requestId);

    List<CompanionRequest> findForSearch(long searchId);

    List<CompanionRequest> findPendingForUpdate(long searchId);

    List<CompanionRequest> findPendingForUpdate(List<Long> searchIds);
}
