package com.daebangg.sayroam.companion;

import com.daebangg.sayroam.common.domain.TravelPeriod;
import com.daebangg.sayroam.companion.application.dto.CompanionMatchingCandidateResult;
import com.daebangg.sayroam.companion.application.dto.CompanionMatchingClaim;
import com.daebangg.sayroam.companion.application.dto.SaveCompanionSearchCommand;
import com.daebangg.sayroam.companion.application.port.out.CompanionCandidateQueryRepository;
import com.daebangg.sayroam.companion.application.service.CompanionMatchingService;
import com.daebangg.sayroam.companion.application.service.CompanionSearchService;
import com.daebangg.sayroam.companion.domain.CompanionDiscovery;
import com.daebangg.sayroam.companion.domain.CompanionDiscoveryRepository;
import com.daebangg.sayroam.companion.domain.CompanionSearchRepository;
import com.daebangg.sayroam.companion.domain.CompanionSearchStatus;
import com.daebangg.sayroam.companion.presentation.scheduling.CompanionMatchingWorker;
import com.daebangg.sayroam.notification.application.dto.NotificationListQuery;
import com.daebangg.sayroam.notification.application.service.NotificationService;
import com.daebangg.sayroam.notification.domain.NotificationTargetType;
import com.daebangg.sayroam.notification.domain.NotificationType;
import com.daebangg.sayroam.support.DatabaseIntegrationTest;
import com.daebangg.sayroam.user.domain.Gender;
import com.daebangg.sayroam.user.domain.Role;
import com.daebangg.sayroam.user.domain.User;
import com.daebangg.sayroam.user.domain.UserRepository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;

class CompanionMatchingIntegrationTest extends DatabaseIntegrationTest {
    @Autowired CompanionSearchService searches;
    @Autowired CompanionMatchingService matching;
    @Autowired UserRepository users;
    @Autowired CompanionDiscoveryRepository discoveries;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager transactions;
    @MockitoSpyBean CompanionCandidateQueryRepository candidates;
    @MockitoSpyBean NotificationService notifications;
    @Autowired CompanionSearchRepository searchRepository;

    private final List<Long> members = new ArrayList<>();
    private final LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
    private long destination;

    @BeforeEach
    void setUp() {
        // 다른 통합 테스트에서 남긴 실행 대기 작업과 이 테스트의 선점을 분리한다.
        jdbc.update("DELETE FROM companion_matching_job");
        destination = jdbc.queryForObject("SELECT id FROM destination WHERE name = '서울'", Long.class);
    }

    @AfterEach
    void cleanUp() {
        reset(candidates, notifications);
        for (long member : members) {
            jdbc.update("DELETE FROM notification WHERE receiver_id = ?", member);
            jdbc.update("DELETE FROM companion_search WHERE user_id = ?", member);
            jdbc.update("DELETE FROM user_travel_style WHERE user_id = ?", member);
            jdbc.update("DELETE FROM users WHERE id = ?", member);
        }
    }

    @Test
    void queuesCommittedCreatesUpdatesAndReopensButNotRepeatedActiveRequests() {
        long member = member(Gender.MALE, 30);
        long source = searches.create(member, conditions(null, null, null)).id();
        assertThat(jobCount(source)).isEqualTo(1);
        searches.update(member, source, conditions(null, null, null));
        assertThat(jobCount(source)).isEqualTo(2);
        searches.changeStatus(member, source, CompanionSearchStatus.CLOSED);
        assertThat(jobCount(source)).isEqualTo(2);
        searches.changeStatus(member, source, CompanionSearchStatus.ACTIVE);
        searches.changeStatus(member, source, CompanionSearchStatus.ACTIVE);
        assertThat(jobCount(source)).isEqualTo(3);
        assertThat(jdbc.queryForObject("SELECT matching_revision FROM companion_search WHERE id = ?",
                Long.class, source)).isEqualTo(5);

        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            searches.update(member, source, conditions(Gender.FEMALE, 20, 40));
            status.setRollbackOnly();
        });
        assertThat(jobCount(source)).isEqualTo(3);
        assertThat(jdbc.queryForObject("SELECT matching_revision FROM companion_search WHERE id = ?",
                Long.class, source)).isEqualTo(5);
        drain();
        assertThat(jdbc.queryForList("SELECT status FROM companion_matching_job WHERE search_id = ? ORDER BY id",
                String.class, source)).containsExactly("SKIPPED", "SKIPPED", "SUCCEEDED");
    }

    @Test
    void findsEverySearchPairWithMutualConditionsAndPreservesFirstDiscoveryOnReverseJobs() {
        long owner = member(Gender.MALE, 30);
        long other = member(Gender.FEMALE, 28);
        long source = searches.create(owner, conditions(Gender.FEMALE, 25, 35)).id();
        long first = seed(other, destination, today.plusDays(11), today.plusDays(15), "ACTIVE", "MALE", 29, 31);
        long second = seed(other, destination, today.plusDays(10), today.plusDays(12), "ACTIVE", null, null, null);
        seed(member(Gender.FEMALE, 28), destination, today.plusDays(10), today.plusDays(15), "ACTIVE", "FEMALE", null, null);
        seed(member(Gender.FEMALE, 40), destination, today.plusDays(10), today.plusDays(15), "ACTIVE", null, null, null);
        seed(member(null, null), destination, today.plusDays(10), today.plusDays(15), "ACTIVE", null, null, null);
        seed(member(Gender.FEMALE, 28), destination, today.plusDays(10), today.plusDays(15), "ACTIVE", "MALE", 31, null);
        seed(other, destination, today.plusDays(10), today.plusDays(15), "CLOSED", null, null, null);
        seed(other, destination, today.plusDays(30), today.plusDays(31), "ACTIVE", null, null, null);
        long busan = jdbc.queryForObject("SELECT id FROM destination WHERE name = '부산'", Long.class);
        seed(other, busan, today.plusDays(10), today.plusDays(15), "ACTIVE", null, null, null);
        seed(owner, destination, today.plusDays(10), today.plusDays(15), "ACTIVE", null, null, null);
        long deleted = member(Gender.FEMALE, 28);
        seed(deleted, destination, today.plusDays(10), today.plusDays(15), "ACTIVE", null, null, null);
        jdbc.update("UPDATE users SET deleted_at = UTC_TIMESTAMP(6) WHERE id = ?", deleted);

        drain();
        assertThat(discoveryCount(source)).isEqualTo(2);
        assertThat(notificationCount(owner)).isEqualTo(1);
        assertThat(notificationCount(other)).isEqualTo(2);
        assertThat(jdbc.queryForList("SELECT second_search_id FROM companion_discovery WHERE first_search_id = ? ORDER BY second_search_id",
                Long.class, source)).containsExactly(first, second);
        assertThat(jdbc.queryForObject("SELECT overlap_end FROM companion_discovery WHERE first_search_id = ? AND second_search_id = ?",
                LocalDate.class, source, second)).isEqualTo(today.plusDays(12));
        String created = jdbc.queryForObject("SELECT created_at FROM companion_discovery WHERE first_search_id = ? AND second_search_id = ?",
                String.class, source, first);
        searches.update(other, first, conditions(Gender.MALE, 29, 31));
        drain();
        assertThat(discoveryCount(source)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT created_at FROM companion_discovery WHERE first_search_id = ? AND second_search_id = ?",
                String.class, source, first)).isEqualTo(created);
        assertThat(jdbc.queryForObject("SELECT second_revision FROM companion_discovery WHERE first_search_id = ? AND second_search_id = ?",
                Long.class, source, first)).isEqualTo(1);
    }

    @Test
    void skipsChangedClosedExpiredAndDeletedSources() {
        long owner = member(null, null);
        long other = member(null, null);
        seed(other, destination, today.plusDays(10), today.plusDays(15), "ACTIVE", null, null, null);
        long source = searches.create(owner, conditions(null, null, null)).id();
        CompanionMatchingClaim stale = matching.claim().orElseThrow();
        searches.update(owner, source, conditions(null, null, null));
        matching.process(stale);
        assertThat(status(stale)).isEqualTo("SKIPPED");
        CompanionMatchingClaim closed = matching.claim().orElseThrow();
        searches.changeStatus(owner, source, CompanionSearchStatus.CLOSED);
        matching.process(closed);
        assertThat(status(closed)).isEqualTo("SKIPPED");
        searches.changeStatus(owner, source, CompanionSearchStatus.ACTIVE);
        drain();
        assertThat(discoveryCount(source)).isEqualTo(1);

        long expired = searches.create(owner, new SaveCompanionSearchCommand(destination, today.minusDays(5),
                today.minusDays(1), null, null, null, null, null, null, null)).id();
        CompanionMatchingClaim expiredClaim = matching.claim().orElseThrow();
        matching.process(expiredClaim);
        assertThat(status(expiredClaim)).isEqualTo("SKIPPED");
        assertThat(discoveryCount(expired)).isZero();

        long deleted = searches.create(other, conditions(null, null, null)).id();
        jdbc.update("UPDATE users SET deleted_at = UTC_TIMESTAMP(6) WHERE id = ?", other);
        drain();
        assertThat(discoveryCount(deleted)).isZero();
    }

    @Test
    void excludesExpiredOverlapsButIncludesTheKoreanReferenceDay() {
        long owner = member(null, null);
        long other = member(null, null);
        long source = searches.create(owner, new SaveCompanionSearchCommand(destination, today.minusDays(5),
                today.plusDays(5), null, null, null, null, null, null, null)).id();
        seed(other, destination, today.minusDays(4), today.minusDays(1), "ACTIVE", null, null, null);
        long todayMatch = seed(other, destination, today.minusDays(4), today, "ACTIVE", null, null, null);
        drain();
        assertThat(discoveryCount(source)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT second_search_id FROM companion_discovery WHERE first_search_id = ?",
                Long.class, source)).isEqualTo(todayMatch);
    }

    @Test
    void usesKeysetBatchesWithoutTruncatingAtOneHundredMatches() {
        long owner = member(null, null);
        long other = member(null, null);
        long source = searches.create(owner, conditions(null, null, null)).id();
        for (int index = 0; index < 105; index++) {
            seed(other, destination, today.plusDays(10), today.plusDays(15), "ACTIVE", null, null, null);
        }
        CompanionMatchingClaim first = matching.claim().orElseThrow();
        matching.process(first);
        assertThat(status(first)).isEqualTo("PENDING");
        assertThat(discoveryCount(source)).isEqualTo(100);
        assertThat(notificationCount(owner)).isEqualTo(1);
        long notice = jdbc.queryForObject("SELECT id FROM notification WHERE receiver_id = ?", Long.class, owner);
        notifications.read(owner, notice);
        drain();
        assertThat(discoveryCount(source)).isEqualTo(105);
        assertThat(notificationCount(owner)).isEqualTo(1);
        assertThat(status(first)).isEqualTo("SUCCEEDED");
    }

    @Test
    void rollsBackPartialResultsThenRetriesOnlyAfterBackoff() {
        long owner = member(null, null);
        long other = member(null, null);
        long source = searches.create(owner, conditions(null, null, null)).id();
        long target = seed(other, destination, today.plusDays(10), today.plusDays(15), "ACTIVE", null, null, null);
        CompanionMatchingClaim claim = matching.claim().orElseThrow();
        doThrow(new IllegalStateException("두 번째 수신자 알림 저장 실패"))
                .when(notifications).createGroupedIfAbsent(argThat(command -> command.receiverId() == other));
        assertThatThrownBy(() -> matching.process(claim)).isInstanceOf(RuntimeException.class);
        assertThat(discoveryCount(source)).isZero();
        assertThat(notificationCount(owner)).isZero();
        assertThat(notificationCount(other)).isZero();
        assertThat(status(claim)).isEqualTo("PROCESSING");
        assertThat(jdbc.queryForObject("SELECT candidate_cursor FROM companion_matching_job WHERE id = ?",
                Long.class, claim.jobId())).isZero();
        matching.retry(claim);
        assertThat(status(claim)).isEqualTo("RETRY");
        assertThat(matching.claim()).isEmpty();
        reset(notifications);
        jdbc.update("UPDATE companion_matching_job SET available_at = UTC_TIMESTAMP(6) - INTERVAL 1 SECOND WHERE id = ?", claim.jobId());
        drain();
        assertThat(discoveryCount(source)).isEqualTo(1);
        assertThat(status(claim)).isEqualTo("SUCCEEDED");
        assertThat(notificationCount(owner)).isEqualTo(1);
        assertThat(notificationCount(other)).isEqualTo(1);
    }

    @Test
    void reclaimsInterruptedWorkAndRejectsTheOldWorkersToken() {
        searches.create(member(null, null), conditions(null, null, null));
        CompanionMatchingClaim abandoned = matching.claim().orElseThrow();
        assertThat(matching.claim()).isEmpty();
        jdbc.update("UPDATE companion_matching_job SET available_at = UTC_TIMESTAMP(6) - INTERVAL 1 SECOND WHERE id = ?", abandoned.jobId());
        CompanionMatchingClaim recovered = matching.claim().orElseThrow();
        assertThat(recovered.jobId()).isEqualTo(abandoned.jobId());
        assertThat(recovered.token()).isNotEqualTo(abandoned.token());
        matching.process(abandoned);
        matching.retry(abandoned);
        assertThat(status(recovered)).isEqualTo("PROCESSING");
        matching.process(recovered);
        assertThat(status(recovered)).isEqualTo("SUCCEEDED");
    }

    @Test
    void concurrentWorkersClaimDifferentJobsAndKeepOneRecordPerPair() throws Exception {
        searches.create(member(null, null), conditions(null, null, null));
        searches.create(member(null, null), conditions(null, null, null));
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<CompanionMatchingClaim> first = executor.submit(() -> matching.claim().orElseThrow());
            Future<CompanionMatchingClaim> second = executor.submit(() -> matching.claim().orElseThrow());
            CompanionMatchingClaim left = first.get(10, TimeUnit.SECONDS);
            CompanionMatchingClaim right = second.get(10, TimeUnit.SECONDS);
            assertThat(left.jobId()).isNotEqualTo(right.jobId());
            Future<?> leftResult = executor.submit(() -> processOrRetry(left));
            Future<?> rightResult = executor.submit(() -> processOrRetry(right));
            leftResult.get(15, TimeUnit.SECONDS);
            rightResult.get(15, TimeUnit.SECONDS);
        }
        jdbc.update("UPDATE companion_matching_job SET available_at = UTC_TIMESTAMP(6) - INTERVAL 1 SECOND WHERE status = 'RETRY'");
        drain();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM companion_discovery WHERE first_search_id IN (SELECT id FROM companion_search WHERE user_id = ?)",
                Long.class, members.getFirst())).isEqualTo(1);
        assertThat(notificationCount(members.getFirst())).isEqualTo(1);
        assertThat(notificationCount(members.getLast())).isEqualTo(1);
    }

    @Test
    void olderSnapshotsCannotOverwriteNewerVersionsOrSynthesizeAnUnobservedPair() {
        long first = searches.create(member(null, null), conditions(null, null, null)).id();
        long second = searches.create(member(null, null), conditions(null, null, null)).id();
        TravelPeriod overlap = new TravelPeriod(today.plusDays(10), today.plusDays(15));
        LocalDateTime foundAt = LocalDateTime.of(2030, 1, 1, 0, 0);
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            discoveries.record(CompanionDiscovery.found(first, 2, second, 2, overlap, foundAt));
            discoveries.record(CompanionDiscovery.found(second, 3, first, 1,
                    new TravelPeriod(today.plusDays(11), today.plusDays(12)), foundAt.plusMinutes(1)));
        });
        assertThat(jdbc.queryForMap("""
                SELECT first_revision, second_revision, overlap_start FROM companion_discovery
                WHERE first_search_id = ? AND second_search_id = ?
                """, first, second)).containsEntry("first_revision", 2L).containsEntry("second_revision", 2L)
                .containsEntry("overlap_start", java.sql.Date.valueOf(overlap.startDate()));
    }

    @Test
    void workerRunsThePersistentQueue() {
        long source = searches.create(member(null, null), conditions(null, null, null)).id();
        seed(member(null, null), destination, today.plusDays(10), today.plusDays(15), "ACTIVE", null, null, null);
        new CompanionMatchingWorker(matching, Runnable::run).requestRun();
        assertThat(discoveryCount(source)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT status FROM companion_matching_job WHERE search_id = ?", String.class, source))
                .isEqualTo("SUCCEEDED");
    }

    @Test
    void notifiesBothOwnersOnceAndKeepsReadDeletedAndSnapshotValuesOnRediscovery() {
        long owner = member(Gender.MALE, 30);
        long other = member(Gender.FEMALE, 28);
        long source = searches.create(owner, conditions(null, null, null)).id();
        long target = searches.create(other, conditions(null, null, null)).id();
        drain();
        assertThat(notificationCount(owner)).isEqualTo(1);
        assertThat(notificationCount(other)).isEqualTo(1);
        assertThat(notifications.findAll(new NotificationListQuery(owner, null, 20, false)))
                .singleElement().satisfies(result -> {
                    assertThat(result.type()).isEqualTo(NotificationType.COMPANION_MATCH_FOUND);
                    assertThat(result.targetType()).isEqualTo(NotificationTargetType.COMPANION_SEARCH);
                    assertThat(result.targetId()).isEqualTo(source);
                });
        assertThat(notifications.findAll(new NotificationListQuery(other, null, 20, false)))
                .singleElement().satisfies(result -> assertThat(result.targetId()).isEqualTo(target));
        long readId = jdbc.queryForObject("SELECT id FROM notification WHERE receiver_id = ?", Long.class, owner);
        long deletedId = jdbc.queryForObject("SELECT id FROM notification WHERE receiver_id = ?", Long.class, other);
        notifications.read(owner, readId);
        notifications.delete(other, deletedId);
        List<Map<String, Object>> before = jdbc.queryForList(
                "SELECT * FROM notification WHERE receiver_id IN (?, ?) ORDER BY id", owner, other);
        searches.update(owner, source, conditions(null, null, null));
        searches.changeStatus(other, target, CompanionSearchStatus.CLOSED);
        searches.changeStatus(other, target, CompanionSearchStatus.ACTIVE);
        drain();
        assertThat(jdbc.queryForList("SELECT * FROM notification WHERE receiver_id IN (?, ?) ORDER BY id", owner, other))
                .isEqualTo(before);
        assertThat(notifications.findAll(new NotificationListQuery(other, null, 20, false))).isEmpty();
    }

    @Test
    void groupsSeveralCandidatesAndReverseJobsIntoOneUnreadNoticePerSearch() {
        long owner = member(null, null);
        long firstMember = member(null, null);
        long secondMember = member(null, null);
        long source = searches.create(owner, conditions(null, null, null)).id();
        searches.create(firstMember, conditions(null, null, null));
        searches.create(secondMember, conditions(null, null, null));
        drain();
        assertThat(discoveryCount(source)).isEqualTo(2);
        assertThat(notificationCount(owner)).isEqualTo(1);
        assertThat(notificationCount(firstMember)).isEqualTo(1);
        assertThat(notificationCount(secondMember)).isEqualTo(1);
        searches.update(owner, source, conditions(null, null, null));
        drain();
        assertThat(notificationCount(owner)).isEqualTo(1);
    }

    @Test
    void onlyNewCandidatesAfterReadOrDeletionCanCreateAnotherGroupedNotice() {
        long owner = member(null, null);
        long source = searches.create(owner, conditions(null, null, null)).id();
        searches.create(member(null, null), conditions(null, null, null));
        drain();
        long firstNotice = jdbc.queryForObject("SELECT id FROM notification WHERE receiver_id = ?", Long.class, owner);
        notifications.read(owner, firstNotice);
        searches.update(owner, source, conditions(null, null, null));
        drain();
        assertThat(notificationCount(owner)).isEqualTo(1);
        searches.create(member(null, null), conditions(null, null, null));
        drain();
        assertThat(notificationCount(owner)).isEqualTo(2);
        long secondNotice = jdbc.queryForObject("SELECT MAX(id) FROM notification WHERE receiver_id = ?", Long.class, owner);
        notifications.delete(owner, secondNotice);
        searches.update(owner, source, conditions(null, null, null));
        drain();
        assertThat(notificationCount(owner)).isEqualTo(2);
        searches.create(member(null, null), conditions(null, null, null));
        drain();
        assertThat(notificationCount(owner)).isEqualTo(3);
        assertThat(notifications.findAll(new NotificationListQuery(owner, null, 20, true))).hasSize(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"revision", "closed", "deleted", "profile", "destination"})
    void revalidatesCandidatesBeforeSavingDiscoveryAndNotifications(String change) {
        long owner = member(Gender.MALE, 30);
        long other = member(Gender.FEMALE, 28);
        long source = searches.create(owner, conditions(Gender.FEMALE, 25, 35)).id();
        long target = seed(other, destination, today.plusDays(10), today.plusDays(15), "ACTIVE", "MALE", null, null);
        List<CompanionMatchingCandidateResult> snapshot = candidates.findMatchingCandidates(
                searchRepository.findByIdAndUserId(source, owner).orElseThrow(), today, 0, 100);
        assertThat(snapshot).hasSize(1);
        doAnswer(invocation -> {
            switch (change) {
                case "revision" -> jdbc.update("UPDATE companion_search SET matching_revision = matching_revision + 1 WHERE id = ?", target);
                case "closed" -> jdbc.update("UPDATE companion_search SET status = 'CLOSED' WHERE id = ?", target);
                case "deleted" -> jdbc.update("UPDATE users SET deleted_at = UTC_TIMESTAMP(6) WHERE id = ?", other);
                case "profile" -> jdbc.update("UPDATE users SET gender = 'MALE' WHERE id = ?", other);
                case "destination" -> jdbc.update("UPDATE destination SET is_active = FALSE WHERE id = ?", destination);
                default -> throw new AssertionError(change);
            }
            return snapshot;
        }).when(candidates).findMatchingCandidates(any(), any(), anyLong(), anyInt());
        try {
            drain();
            assertThat(discoveryCount(source)).isZero();
            assertThat(notificationCount(owner)).isZero();
            assertThat(notificationCount(other)).isZero();
        } finally {
            jdbc.update("UPDATE destination SET is_active = TRUE WHERE id = ?", destination);
        }
    }

    private long notificationCount(long member) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM notification WHERE receiver_id = ? AND type = 'COMPANION_MATCH_FOUND'",
                Long.class, member);
    }

    private void processOrRetry(CompanionMatchingClaim claim) {
        try {
            matching.process(claim);
        } catch (RuntimeException exception) {
            matching.retry(claim);
        }
    }

    private long member(Gender gender, Integer age) {
        User user = users.saveNewAccount(User.builder().nickname("백그라운드 검증").role(Role.USER).build());
        members.add(user.getId());
        jdbc.update("UPDATE users SET gender = ?, birth_date = ? WHERE id = ?",
                gender == null ? null : gender.name(), age == null ? null : today.minusYears(age), user.getId());
        jdbc.update("""
                INSERT INTO user_travel_style (user_id, item_id, score, created_at, updated_at)
                SELECT ?, id, 3, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6) FROM travel_style_item
                WHERE is_active = TRUE AND code IN ('planning', 'daily-pace', 'time-together', 'spending')
                """, user.getId());
        return user.getId();
    }

    private SaveCompanionSearchCommand conditions(Gender gender, Integer minAge, Integer maxAge) {
        return new SaveCompanionSearchCommand(destination, today.plusDays(10), today.plusDays(15),
                gender, minAge, maxAge, null, null, null, null);
    }

    private long seed(long member, long place, LocalDate start, LocalDate end, String status,
                      String preferredGender, Integer minAge, Integer maxAge) {
        jdbc.update("""
                INSERT INTO companion_search
                    (user_id, destination_id, start_date, end_date, status, preferred_gender, preferred_min_age,
                     preferred_max_age, created_at, updated_at, style_planning, style_daily_pace, style_time_together, style_spending)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6), 3, 3, 3, 3)
                """, member, place, start, end, status, preferredGender, minAge, maxAge);
        return jdbc.queryForObject("SELECT MAX(id) FROM companion_search WHERE user_id = ?", Long.class, member);
    }

    private long jobCount(long search) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM companion_matching_job WHERE search_id = ?", Long.class, search);
    }

    private long discoveryCount(long search) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM companion_discovery WHERE first_search_id = ? OR second_search_id = ?",
                Long.class, search, search);
    }

    private String status(CompanionMatchingClaim claim) {
        return jdbc.queryForObject("SELECT status FROM companion_matching_job WHERE id = ?", String.class, claim.jobId());
    }

    private void drain() {
        for (int index = 0; index < 200; index++) {
            Optional<CompanionMatchingClaim> next = matching.claim();
            if (next.isEmpty()) return;
            matching.process(next.get());
        }
        throw new AssertionError("매칭 작업이 제한 횟수 안에 완료되지 않았습니다.");
    }
}
