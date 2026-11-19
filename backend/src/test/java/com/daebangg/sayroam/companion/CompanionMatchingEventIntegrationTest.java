package com.daebangg.sayroam.companion;

import com.daebangg.sayroam.companion.application.dto.SaveCompanionSearchCommand;
import com.daebangg.sayroam.companion.application.service.CompanionSearchService;
import com.daebangg.sayroam.companion.presentation.scheduling.CompanionMatchingWorker;
import com.daebangg.sayroam.support.DatabaseIntegrationTest;
import com.daebangg.sayroam.user.domain.Role;
import com.daebangg.sayroam.user.domain.User;
import com.daebangg.sayroam.user.domain.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@TestPropertySource(properties = {"app.companion.matching.enabled=true", "app.companion.matching.recovery-delay-ms=3600000"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class CompanionMatchingEventIntegrationTest extends DatabaseIntegrationTest {
    @Autowired CompanionSearchService searches;
    @Autowired UserRepository users;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager transactions;
    @Autowired @Qualifier("companionMatchingExecutor") ThreadPoolTaskExecutor executor;
    @MockitoSpyBean CompanionMatchingWorker worker;
    private final List<Long> members = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        for (long member : members) {
            jdbc.update("DELETE FROM companion_search WHERE user_id = ?", member);
            jdbc.update("DELETE FROM user_travel_style WHERE user_id = ?", member);
            jdbc.update("DELETE FROM users WHERE id = ?", member);
        }
    }

    @Test
    void committedEventsDiscoverCandidatesBeforeTheRecoveryTimer() {
        long destination = jdbc.queryForObject("SELECT id FROM destination WHERE name = '서울'", Long.class);
        LocalDate start = LocalDate.now(ZoneId.of("Asia/Seoul")).plusDays(10);
        SaveCompanionSearchCommand conditions = new SaveCompanionSearchCommand(destination, start, start.plusDays(3),
                null, null, null, null, null, null, null);
        long first = searches.create(member(), conditions).id();
        long second = searches.create(member(), conditions).id();
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            assertThat(jdbc.queryForObject("""
                    SELECT COUNT(*) FROM companion_matching_job
                    WHERE search_id IN (?, ?) AND status = 'SUCCEEDED'
                    """, Long.class, first, second)).isEqualTo(2);
            assertThat(jdbc.queryForObject("""
                    SELECT COUNT(*) FROM companion_discovery WHERE first_search_id = ? AND second_search_id = ?
                    """, Long.class, first, second)).isEqualTo(1);
        });
    }

    @Test
    void doesNotWakeTheWorkerBeforeCommitOrAfterRollback() throws Exception {
        // 시작 이벤트의 DB 확인이 끝난 뒤 이번 트랜잭션의 실행 신호만 검증한다.
        executor.submit(() -> {}).get(10, TimeUnit.SECONDS);
        clearInvocations(worker);
        long owner = member();
        long destination = jdbc.queryForObject("SELECT id FROM destination WHERE name = '서울'", Long.class);
        LocalDate start = LocalDate.now(ZoneId.of("Asia/Seoul")).plusDays(10);
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            searches.create(owner, new SaveCompanionSearchCommand(destination, start, start.plusDays(3),
                    null, null, null, null, null, null, null));
            verify(worker, never()).onMatchingRequested(any());
            status.setRollbackOnly();
        });
        verify(worker, never()).onMatchingRequested(any());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM companion_search WHERE user_id = ?", Long.class, owner))
                .isZero();
    }

    @Test
    void aCommitAfterTheWorkerRanDoesNotLeaveTheNewJobSleeping() {
        long destination = jdbc.queryForObject("SELECT id FROM destination WHERE name = '서울'", Long.class);
        LocalDate start = LocalDate.now(ZoneId.of("Asia/Seoul")).plusDays(10);
        long owner = member();
        long source = new TransactionTemplate(transactions).execute(status -> {
            long id = searches.create(owner, new SaveCompanionSearchCommand(destination, start, start.plusDays(3),
                    null, null, null, null, null, null, null)).id();
            // 커밋 전 별도 스레드는 이 작업을 볼 수 없다. 커밋 이후 이벤트가 다시 깨워야 한다.
            worker.requestRun();
            await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                    assertThat(executor.getActiveCount()).isZero());
            return id;
        });
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                assertThat(jdbc.queryForObject("SELECT status FROM companion_matching_job WHERE search_id = ?",
                        String.class, source)).isEqualTo("SUCCEEDED"));
    }

    @Test
    void recoversAPersistedJobWhoseEventWasNotDelivered() {
        long destination = jdbc.queryForObject("SELECT id FROM destination WHERE name = '서울'", Long.class);
        long owner = member();
        jdbc.update("""
                INSERT INTO companion_search
                    (user_id, destination_id, start_date, end_date, status, created_at, updated_at)
                VALUES (?, ?, '2035-01-01', '2035-01-03', 'ACTIVE', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6))
                """, owner, destination);
        long source = jdbc.queryForObject("SELECT MAX(id) FROM companion_search WHERE user_id = ?", Long.class, owner);
        jdbc.update("""
                INSERT INTO companion_matching_job
                    (search_id, matching_revision, status, available_at, created_at, updated_at)
                VALUES (?, 0, 'PENDING', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6), UTC_TIMESTAMP(6))
                """, source);
        worker.recover();
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                assertThat(jdbc.queryForObject("SELECT status FROM companion_matching_job WHERE search_id = ?",
                        String.class, source)).isEqualTo("SUCCEEDED"));
    }

    private long member() {
        User user = users.saveNewAccount(User.builder().nickname("자동 실행 검증").role(Role.USER).build());
        members.add(user.getId());
        jdbc.update("""
                INSERT INTO user_travel_style (user_id, item_id, score, created_at, updated_at)
                SELECT ?, id, 3, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6) FROM travel_style_item
                WHERE is_active = TRUE AND code IN ('planning', 'daily-pace', 'time-together', 'spending')
                """, user.getId());
        return user.getId();
    }
}
