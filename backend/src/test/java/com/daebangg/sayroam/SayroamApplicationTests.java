package com.daebangg.sayroam;

import com.daebangg.sayroam.common.domain.TravelPeriod;
import com.daebangg.sayroam.support.DatabaseIntegrationTest;
import com.daebangg.sayroam.room.domain.Room;
import com.daebangg.sayroam.trip.domain.Trip;
import com.daebangg.sayroam.user.domain.User;
import com.daebangg.sayroam.user.domain.Role;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceContext;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.mysql.MySQLContainer;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SayroamApplicationTests extends DatabaseIntegrationTest {

    @Autowired
    EntityManagerFactory entityManagerFactory;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    Environment environment;

    @PersistenceContext
    EntityManager entityManager;

    @Test
    void schemaMatchesEntitiesAndRepeatedMigrationDoesNotReapply() {
        assertThat(environment.getProperty("spring.flyway.enabled")).isEqualTo("false");
        assertThat(environment.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("validate");
        assertThat(entityManagerFactory.getMetamodel().getEntities()).hasSize(20);
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM information_schema.tables
                WHERE table_schema = DATABASE() AND table_type = 'BASE TABLE'
                """, Integer.class)).isEqualTo(21);
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("28");
        flyway.validate();
        assertThat(flyway.migrate().migrationsExecuted).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM flyway_schema_history WHERE success = 1",
                Integer.class)).isEqualTo(28);
    }

    @Test
    void 버전_27은_기존_방의_연결을_유지하며_여행과_동행_유형을_채운다() {
        try (MySQLContainer mysql = new MySQLContainer("mysql:8.4")
                .withCommand("--log-bin-trust-function-creators=1")) {
            mysql.start();
            DriverManagerDataSource source = new DriverManagerDataSource(
                    mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword());
            Flyway.configure().dataSource(source).target("26").load().migrate();
            JdbcTemplate db = new JdbcTemplate(source);
            db.update("""
                    INSERT INTO users (id, nickname, role, created_at, updated_at)
                    VALUES (10, '회원 가', 'USER', NOW(), NOW()), (20, '회원 나', 'USER', NOW(), NOW())
                    """);
            db.update("""
                    INSERT INTO room (id, created_at, updated_at)
                    VALUES (1, NOW(), NOW()), (2, NOW(), NOW()), (3, NOW(), NOW()), (4, NOW(), NOW())
                    """);
            db.update("""
                    INSERT INTO companion_search (id, user_id, region, start_date, end_date, status, created_at, updated_at)
                    VALUES (1, 20, '서울', '2026-12-01', '2026-12-04', 'CLOSED', NOW(), NOW())
                    """);
            db.update("""
                    INSERT INTO companion_request (sender_id, receiver_id, companion_search_id, room_id,
                        status, created_at, updated_at)
                    VALUES (10, 20, 1, 2, 'ACCEPTED', NOW(), NOW()), (10, 20, 1, 3, 'ACCEPTED', NOW(), NOW())
                    """);
            Long destinationId = db.queryForObject("SELECT MIN(id) FROM destination", Long.class);
            db.update("""
                    INSERT INTO trip (created_by, room_id, destination_id, title, start_date, end_date, created_at, updated_at)
                    VALUES (10, 1, ?, '여행 방', '2026-12-01', '2026-12-04', NOW(), NOW()),
                           (10, 3, ?, '기존 동행 여행 방', '2026-12-01', '2026-12-04', NOW(), NOW())
                    """, destinationId, destinationId);
            db.update("""
                    INSERT INTO room_member (room_id, user_id, status, notification_muted, joined_at, created_at, updated_at)
                    VALUES (2, 10, 'JOINED', 0, NOW(), NOW(), NOW()), (2, 20, 'JOINED', 0, NOW(), NOW(), NOW())
                    """);
            db.update("""
                    INSERT INTO chat_message (room_id, sender_id, type, content, created_at, updated_at)
                    VALUES (2, 10, 'TEXT', '기존 대화', NOW(), NOW())
                    """);

            Flyway upgraded = Flyway.configure().dataSource(source).target("27").load();
            assertThat(upgraded.migrate().migrationsExecuted).isEqualTo(1);

            assertThat(db.queryForList("SELECT room_type FROM room ORDER BY id", String.class))
                    .containsExactly("TRIP", "COMPANION", "TRIP", "TRIP");
            assertThat(db.queryForList("SELECT room_id FROM trip ORDER BY room_id", Long.class))
                    .containsExactly(1L, 3L);
            assertThat(db.queryForList("SELECT room_id FROM companion_request ORDER BY room_id", Long.class))
                    .containsExactly(2L, 3L);
            assertThat(db.queryForObject("SELECT COUNT(*) FROM room_member WHERE room_id = 2", Integer.class))
                    .isEqualTo(2);
            assertThat(db.queryForObject("SELECT content FROM chat_message WHERE room_id = 2", String.class))
                    .isEqualTo("기존 대화");
            assertThatThrownBy(() -> db.update("""
                    INSERT INTO room (room_type, created_at, updated_at) VALUES (NULL, NOW(), NOW())
                    """)).isInstanceOf(DataAccessException.class);
            assertThatThrownBy(() -> db.update("""
                    INSERT INTO room (room_type, created_at, updated_at) VALUES ('DIRECT', NOW(), NOW())
                    """)).isInstanceOf(DataAccessException.class);
            upgraded.validate();
            assertThat(upgraded.migrate().migrationsExecuted).isZero();
        }
    }

    @Test
    void upgradesVersionEightWithExistingAnswersAndSeedsDestinations() {
        try (MySQLContainer mysql = new MySQLContainer("mysql:8.4")
                .withCommand("--log-bin-trust-function-creators=1")) {
            mysql.start();
            DriverManagerDataSource dataSource = new DriverManagerDataSource(
                    mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword());
            Flyway.configure().dataSource(dataSource).target("8").load().migrate();
            JdbcTemplate legacy = new JdbcTemplate(dataSource);
            legacy.update("""
                    INSERT INTO users (id, nickname, role, created_at, updated_at)
                    VALUES (10, '기존 회원', 'USER', NOW(), NOW())
                    """);
            legacy.update("""
                    INSERT INTO travel_style_item (id, name, min_label, max_label, sort_order, is_active, created_at, updated_at)
                    VALUES (20, '기존 질문', '낮음', '높음', 10, 0, NOW(), NOW())
                    """);
            legacy.update("""
                    INSERT INTO user_travel_style (user_id, item_id, score, created_at, updated_at)
                    VALUES (10, 20, 3, NOW(), NOW())
                    """);

            assertThat(Flyway.configure().dataSource(dataSource).target("9").load().migrate().migrationsExecuted)
                    .isEqualTo(1);
            assertThat(legacy.queryForObject("SELECT code FROM travel_style_item WHERE id = 20", String.class))
                    .isEqualTo("legacy-20");
            assertThat(legacy.queryForObject("SELECT score FROM user_travel_style WHERE user_id = 10 AND item_id = 20",
                    Integer.class)).isEqualTo(3);
            assertThat(legacy.queryForList("SELECT code FROM travel_style_item WHERE is_active = 1 ORDER BY sort_order",
                    String.class)).containsExactly("planning", "daily-pace", "time-together", "spending");
            assertThat(legacy.queryForObject("""
                    SELECT COUNT(*) FROM information_schema.columns
                    WHERE table_schema = DATABASE() AND table_name = 'travel_style_item'
                        AND column_name IN ('name', 'min_label', 'max_label')
                    """, Integer.class)).isZero();
            assertThat(Flyway.configure().dataSource(dataSource).target("10").load().migrate().migrationsExecuted)
                    .isEqualTo(1);
            assertThat(Flyway.configure().dataSource(dataSource).target("11").load().migrate().migrationsExecuted)
                    .isEqualTo(1);
            assertThat(legacy.queryForObject("SELECT COUNT(*) FROM destination", Integer.class)).isEqualTo(160);
            assertThat(legacy.queryForObject("SELECT score FROM user_travel_style WHERE user_id = 10 AND item_id = 20",
                    Integer.class)).isEqualTo(3);

            legacy.update("""
                    INSERT INTO companion_search (id, user_id, region, start_date, end_date, status, created_at, updated_at)
                    VALUES (1, 10, '기존 여행지', '2026-12-01', '2026-12-02', 'ACTIVE', NOW(), NOW())
                    """);
            assertThat(Flyway.configure().dataSource(dataSource).target("12").load().migrate().migrationsExecuted)
                    .isEqualTo(1);
            assertThat(Flyway.configure().dataSource(dataSource).target("13").load().migrate().migrationsExecuted)
                    .isEqualTo(1);
            legacy.update("UPDATE companion_search SET introduction = '이전 소개글' WHERE id = 1");
            assertThat(Flyway.configure().dataSource(dataSource).target("14").load().migrate().migrationsExecuted).isEqualTo(1);
            legacy.update("""
                    INSERT INTO companion_search
                        (id, user_id, destination_id, start_date, end_date, status, created_at, updated_at)
                    SELECT 2, 10, id, '2035-01-01', '2035-01-05', 'ACTIVE', NOW(), NOW()
                    FROM destination WHERE name = '서울'
                    """);
            assertThat(Flyway.configure().dataSource(dataSource).target("15").load().migrate().migrationsExecuted).isEqualTo(1);
            legacy.update("""
                    INSERT INTO user_travel_style (user_id, item_id, score, created_at, updated_at)
                    SELECT 10, id, 4, NOW(), NOW() FROM travel_style_item WHERE is_active = TRUE
                    """);
            assertThat(Flyway.configure().dataSource(dataSource).target("16").load().migrate().migrationsExecuted).isEqualTo(1);
            assertThat(legacy.queryForObject("""
                    SELECT COUNT(*) FROM companion_search WHERE style_planning IS NULL AND style_daily_pace IS NULL
                        AND style_time_together IS NULL AND style_spending IS NULL
                    """, Integer.class)).isEqualTo(2);
            assertThat(legacy.queryForObject("SELECT COUNT(*) FROM user_travel_style WHERE user_id = 10", Integer.class))
                    .isEqualTo(5);
            assertThatThrownBy(() -> legacy.update("UPDATE companion_search SET style_planning = 3 WHERE id = 2"))
                    .isInstanceOf(DataAccessException.class).hasMessageContaining("chk_companion_search_style");
            assertThatThrownBy(() -> legacy.update("""
                    UPDATE companion_search SET style_planning = 6, style_daily_pace = 3,
                        style_time_together = 3, style_spending = 3 WHERE id = 2
                    """)).isInstanceOf(DataAccessException.class).hasMessageContaining("chk_companion_search_style");
            legacy.update("""
                    UPDATE companion_search SET style_planning = 2, style_daily_pace = 4,
                        style_time_together = 2, style_spending = 5 WHERE id = 2
                    """);
            assertThat(legacy.queryForList("SELECT search_id FROM companion_matching_job", Long.class))
                    .containsExactly(2L);
            assertThat(legacy.queryForMap("SELECT region, destination_id, title FROM companion_search WHERE id = 1"))
                    .containsEntry("region", "기존 여행지")
                    .containsEntry("destination_id", null)
                    .containsEntry("title", null);
            assertThat(legacy.queryForObject("""
                    SELECT COUNT(*) FROM companion_search
                    WHERE id = 1 AND show_age = FALSE AND show_gender = FALSE AND introduction = '이전 소개글'
                    """, Integer.class)).isEqualTo(1);
            assertThat(Flyway.configure().dataSource(dataSource).target("18").load().migrate().migrationsExecuted)
                    .isEqualTo(2);
        }
    }

    @Test
    void upgradesVersionSixteenPreservingNotificationsAndPreventsDuplicateEventsAfterDeletion() {
        try (MySQLContainer mysql = new MySQLContainer("mysql:8.4")
                .withCommand("--log-bin-trust-function-creators=1")) {
            mysql.start();
            DriverManagerDataSource dataSource = new DriverManagerDataSource(
                    mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword());
            Flyway.configure().dataSource(dataSource).target("16").load().migrate();
            JdbcTemplate legacy = new JdbcTemplate(dataSource);
            legacy.update("""
                    INSERT INTO users (id, nickname, role, created_at, updated_at)
                    VALUES (10, '기존 수신자', 'USER', NOW(), NOW()),
                           (20, '다른 수신자', 'USER', NOW(), NOW())
                    """);
            legacy.update("""
                    INSERT INTO notification
                        (id, receiver_id, type, target_type, target_id, content, read_at, created_at, updated_at)
                    VALUES (100, 10, 'TRIP_INVITE', 'TRIP', 42, ?, '2026-11-20 09:00:00', NOW(), NOW()),
                           (101, 10, 'TRIP_INVITE', 'TRIP', 42, ?, NULL, NOW(), NOW())
                    """, "가".repeat(255), "가".repeat(255));
            String originalColumns = """
                    SELECT id, receiver_id, type, target_type, target_id, content, read_at, created_at, updated_at
                    FROM notification ORDER BY id
                    """;
            List<Map<String, Object>> originalNotifications = legacy.queryForList(originalColumns);

            Flyway upgraded = Flyway.configure().dataSource(dataSource).target("17").load();
            assertThat(upgraded.migrate().migrationsExecuted).isEqualTo(1);
            assertThat(legacy.queryForList(originalColumns)).isEqualTo(originalNotifications);
            assertThat(legacy.queryForList("SELECT event_key FROM notification ORDER BY id", String.class))
                    .containsExactly("legacy:notification:100", "legacy:notification:101");
            assertThat(legacy.queryForObject("SELECT COUNT(*) FROM notification WHERE deleted_at IS NOT NULL",
                    Integer.class)).isZero();
            assertThatThrownBy(() -> legacy.update("""
                    INSERT INTO notification (receiver_id, type, content, created_at, updated_at)
                    VALUES (10, 'TRIP_INVITE', '사건 식별값 누락', NOW(), NOW())
                    """)).isInstanceOf(DataAccessException.class).hasMessageContaining("event_key");

            String insertNotification = """
                    INSERT INTO notification (receiver_id, type, event_key, content, created_at, updated_at)
                    VALUES (?, 'TRIP_INVITE', 'trip-invite:42', '새 알림', NOW(), NOW())
                    """;
            legacy.update(insertNotification, 10);
            assertThatThrownBy(() -> legacy.update(insertNotification, 10))
                    .isInstanceOf(DataIntegrityViolationException.class)
                    .hasMessageContaining("uk_notification_receiver_event");
            legacy.update(insertNotification, 20);
            legacy.update("""
                    UPDATE notification SET deleted_at = NOW()
                    WHERE receiver_id = 10 AND event_key = 'trip-invite:42'
                    """);
            assertThatThrownBy(() -> legacy.update(insertNotification, 10))
                    .isInstanceOf(DataIntegrityViolationException.class)
                    .hasMessageContaining("uk_notification_receiver_event");
            List<Map<String, Object>> beforeWelcome = legacy.queryForList("SELECT * FROM notification ORDER BY id");
            Flyway withWelcome = Flyway.configure().dataSource(dataSource).target("18").load();
            assertThat(withWelcome.migrate().migrationsExecuted).isEqualTo(1);
            assertThat(legacy.queryForList("SELECT * FROM notification ORDER BY id")).isEqualTo(beforeWelcome);
            legacy.update("""
                    INSERT INTO notification (receiver_id, type, event_key, content, created_at, updated_at)
                    VALUES (10, 'WELCOME', 'account-signup:10', 'Welcome to SayRoam!', NOW(), NOW())
                    """);
            assertThat(legacy.queryForObject("SELECT COUNT(*) FROM notification WHERE type = 'WELCOME'",
                    Integer.class)).isEqualTo(1);
            withWelcome.validate();
            assertThat(withWelcome.migrate().migrationsExecuted).isZero();
        }
    }

    @Test
    void upgradesVersionNineteenPreservingNotificationsAndSupportsMatchingTargets() {
        try (MySQLContainer mysql = new MySQLContainer("mysql:8.4")
                .withCommand("--log-bin-trust-function-creators=1")) {
            mysql.start();
            DriverManagerDataSource dataSource = new DriverManagerDataSource(
                    mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword());
            Flyway.configure().dataSource(dataSource).target("19").load().migrate();
            JdbcTemplate legacy = new JdbcTemplate(dataSource);
            legacy.update("""
                    INSERT INTO users (id, nickname, role, created_at, updated_at)
                    VALUES (10, '기존 수신자', 'USER', NOW(), NOW())
                    """);
            legacy.update("""
                    INSERT INTO notification
                        (receiver_id, type, target_type, target_id, event_key, content, read_at, deleted_at, created_at, updated_at)
                    VALUES (10, 'TRIP_INVITE', 'TRIP', 42, 'trip-invite:42', '기존 알림', NOW(), NOW(), NOW(), NOW()),
                           (10, 'WELCOME', NULL, NULL, 'account-signup:10', '가입 축하', NULL, NULL, NOW(), NOW())
                    """);
            List<Map<String, Object>> original = legacy.queryForList("SELECT * FROM notification ORDER BY id");
            Flyway upgraded = Flyway.configure().dataSource(dataSource).target("20").load();
            assertThat(upgraded.migrate().migrationsExecuted).isEqualTo(1);
            assertThat(legacy.queryForList("SELECT * FROM notification ORDER BY id")).isEqualTo(original);
            legacy.update("""
                    INSERT INTO notification
                        (receiver_id, type, target_type, target_id, event_key, content, created_at, updated_at)
                    VALUES (10, 'COMPANION_MATCH_FOUND', 'COMPANION_SEARCH', 123, 'companion-discovery:123:124',
                            '새 동행 후보', NOW(), NOW())
                    """);
            assertThat(legacy.queryForObject("SELECT target_id FROM notification WHERE type = 'COMPANION_MATCH_FOUND'",
                    Long.class)).isEqualTo(123L);
            upgraded.validate();
            assertThat(upgraded.migrate().migrationsExecuted).isZero();
        }
    }

    @Test
    void upgradesVersionTwentyPreservingTripTitlesAndAllowsThirtyCharacterTitles() {
        try (MySQLContainer mysql = new MySQLContainer("mysql:8.4")
                .withCommand("--log-bin-trust-function-creators=1")) {
            mysql.start();
            DriverManagerDataSource dataSource = new DriverManagerDataSource(
                    mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword());
            Flyway.configure().dataSource(dataSource).target("20").load().migrate();
            JdbcTemplate legacy = new JdbcTemplate(dataSource);
            legacy.update("""
                    INSERT INTO users (id, nickname, role, created_at, updated_at)
                    VALUES (10, '기존 회원', 'USER', NOW(), NOW())
                    """);
            legacy.update("""
                    INSERT INTO trip (id, created_by, title, start_date, end_date, created_at, updated_at)
                    VALUES (1, 10, '제주 여행', '2026-12-01', '2026-12-04', NOW(), NOW()),
                           (2, 10, NULL, '2026-12-01', '2026-12-04', NOW(), NOW())
                    """);

            assertThat(Flyway.configure().dataSource(dataSource).target("21").load().migrate().migrationsExecuted).isEqualTo(1);
            assertThat(legacy.queryForObject("SELECT title FROM trip WHERE id = 1", String.class)).isEqualTo("제주 여행");
            assertThat(legacy.queryForObject("SELECT title FROM trip WHERE id = 2", String.class)).isNull();

            String thirtyCharacterTitle = "가".repeat(30);
            legacy.update("""
                    INSERT INTO trip (created_by, title, start_date, end_date, created_at, updated_at)
                    VALUES (10, ?, '2026-12-01', '2026-12-04', NOW(), NOW())
                    """, thirtyCharacterTitle);
            assertThat(legacy.queryForObject("SELECT COUNT(*) FROM trip WHERE title = ?",
                    Integer.class, thirtyCharacterTitle)).isEqualTo(1);

            Flyway.configure().dataSource(dataSource).target("22").load().migrate();
            assertThat(legacy.queryForObject("SELECT profile_setup_seen FROM users WHERE id = 10", Boolean.class)).isTrue();
            legacy.update("INSERT INTO users (id, nickname, role, created_at, updated_at) VALUES (11, '신규 회원', 'USER', NOW(), NOW())");
            assertThat(legacy.queryForObject("SELECT profile_setup_seen FROM users WHERE id = 11", Boolean.class)).isFalse();
        }
    }

    @Test
    void 버전_24는_채팅방_없는_여행에_채팅방을_만들어_만든_회원을_넣고_채팅방을_필수로_만들고_방_초대를_지운다() {
        try (MySQLContainer mysql = new MySQLContainer("mysql:8.4")
                .withCommand("--log-bin-trust-function-creators=1")) {
            // given
            mysql.start();
            DriverManagerDataSource dataSource = new DriverManagerDataSource(
                    mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword());
            Flyway.configure().dataSource(dataSource).target("23").load().migrate();
            JdbcTemplate legacy = new JdbcTemplate(dataSource);
            Long destinationId = legacy.queryForObject("SELECT MIN(id) FROM destination", Long.class);
            legacy.update("""
                    INSERT INTO users (id, nickname, role, created_at, updated_at)
                    VALUES (10, '혼자 회원', 'USER', NOW(), NOW()), (20, '함께 회원', 'USER', NOW(), NOW())
                    """);
            legacy.update("""
                    INSERT INTO room (id, name, created_at, updated_at) VALUES (5, '기존 방', NOW(), NOW())
                    """);
            legacy.update("""
                    INSERT INTO room_member (room_id, user_id, role, status, notification_muted, joined_at,
                        created_at, updated_at)
                    VALUES (5, 20, 'OWNER', 'JOINED', 0, NOW(), NOW(), NOW())
                    """);
            legacy.update("""
                    INSERT INTO trip (id, created_by, room_id, destination_id, title, start_date, end_date,
                        created_at, updated_at)
                    VALUES (1, 10, NULL, ?, '혼자 여행', '2026-12-01', '2026-12-04', NOW(), NOW()),
                           (2, 20, 5, ?, '함께 여행', '2026-12-01', '2026-12-04', NOW(), NOW())
                    """, destinationId, destinationId);

            // when
            int executed = Flyway.configure().dataSource(dataSource).target("24").load().migrate().migrationsExecuted;

            // then
            assertThat(executed).isEqualTo(1);
            Long soloRoomId = legacy.queryForObject("SELECT room_id FROM trip WHERE id = 1", Long.class);
            assertThat(soloRoomId).isNotNull().isNotEqualTo(5L);
            assertThat(legacy.queryForObject("""
                    SELECT COUNT(*) FROM room_member
                    WHERE room_id = ? AND user_id = 10 AND status = 'JOINED'
                    """, Integer.class, soloRoomId)).isEqualTo(1);
            assertThat(legacy.queryForObject("SELECT room_id FROM trip WHERE id = 2", Long.class)).isEqualTo(5L);
            assertThat(legacy.queryForObject("SELECT COUNT(*) FROM room_member WHERE room_id = 5", Integer.class))
                    .isEqualTo(1);
            assertThatThrownBy(() -> legacy.update("""
                    INSERT INTO trip (created_by, destination_id, title, start_date, end_date, created_at, updated_at)
                    VALUES (10, ?, '채팅방 없는 여행', '2026-12-01', '2026-12-04', NOW(), NOW())
                    """, destinationId)).isInstanceOf(DataAccessException.class);
            assertThat(legacy.queryForObject("""
                    SELECT COUNT(*) FROM information_schema.tables
                    WHERE table_schema = DATABASE() AND table_name = 'room_invitation'
                    """, Integer.class)).isZero();
        }
    }

    @Test
    void 버전_22의_여행과_딸린_데이터를_지우고_목적지를_필수로_만든다() {
        try (MySQLContainer mysql = new MySQLContainer("mysql:8.4")
                .withCommand("--log-bin-trust-function-creators=1")) {
            // given
            mysql.start();
            DriverManagerDataSource dataSource = new DriverManagerDataSource(
                    mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword());
            Flyway.configure().dataSource(dataSource).target("22").load().migrate();
            JdbcTemplate legacy = new JdbcTemplate(dataSource);
            legacy.update("""
                    INSERT INTO users (id, nickname, role, created_at, updated_at)
                    VALUES (10, '기존 회원', 'USER', NOW(), NOW())
                    """);
            legacy.update("""
                    INSERT INTO place (id, source, source_id, name, category, source_category_code, latitude, longitude,
                        created_at, updated_at)
                    VALUES (1, 'TOUR', '126081', '송정해수욕장', 'ATTRACTION', '12', 35.1786, 129.1997, NOW(), NOW())
                    """);
            legacy.update("""
                    INSERT INTO trip (id, created_by, title, start_date, end_date, created_at, updated_at)
                    VALUES (1, 10, '부산 여행', '2026-12-01', '2026-12-04', NOW(), NOW())
                    """);
            legacy.update("""
                    INSERT INTO trip_place (trip_id, place_id, added_by, created_at, updated_at)
                    VALUES (1, 1, 10, NOW(), NOW())
                    """);
            legacy.update("""
                    INSERT INTO expense (id, trip_id, payer_id, created_by, title, amount, split_type, spent_at,
                        created_at, updated_at)
                    VALUES (1, 1, 10, 10, '저녁', 30000, 'EQUAL', NOW(), NOW(), NOW())
                    """);
            legacy.update("""
                    INSERT INTO expense_share (expense_id, user_id, amount, created_at, updated_at)
                    VALUES (1, 10, 30000, NOW(), NOW())
                    """);

            // when
            int executed = Flyway.configure().dataSource(dataSource).target("23").load().migrate().migrationsExecuted;

            // then
            assertThat(executed).isEqualTo(1);
            for (String table : List.of("trip", "trip_place", "expense", "expense_share")) {
                assertThat(legacy.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class)).as(table).isZero();
            }
            assertThat(legacy.queryForObject("SELECT COUNT(*) FROM place", Integer.class)).isEqualTo(1);
            assertThatThrownBy(() -> legacy.update("""
                    INSERT INTO trip (created_by, title, start_date, end_date, created_at, updated_at)
                    VALUES (10, '목적지 없는 여행', '2026-12-01', '2026-12-04', NOW(), NOW())
                    """)).isInstanceOf(DataAccessException.class);
        }
    }

    @Test
    void upgradesSingleSearchRequestsWithoutGuessingTheSenderSearch() {
        try (MySQLContainer mysql = new MySQLContainer("mysql:8.4")
                .withCommand("--log-bin-trust-function-creators=1")) {
            mysql.start();
            DriverManagerDataSource source = new DriverManagerDataSource(
                    mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword());
            Flyway.configure().dataSource(source).target("24").load().migrate();
            JdbcTemplate db = new JdbcTemplate(source);
            db.update("INSERT INTO users (id, nickname, role, created_at, updated_at) VALUES "
                    + "(10, '회원 가', 'USER', NOW(), NOW()), (20, '회원 나', 'USER', NOW(), NOW())");
            db.update("INSERT INTO companion_search (id, user_id, region, start_date, end_date, status, created_at, updated_at) "
                    + "VALUES (1, 10, '서울', CURRENT_DATE, CURRENT_DATE, 'ACTIVE', NOW(), NOW()), "
                    + "(2, 20, '서울', CURRENT_DATE, CURRENT_DATE, 'ACTIVE', NOW(), NOW())");
            String insert = "INSERT INTO companion_request (sender_id, receiver_id, companion_search_id, status, created_at, updated_at";
            db.update(insert + ") VALUES (10, 20, 2, 'ACCEPTED', NOW(), NOW())");
            Flyway.configure().dataSource(source).target("25").load().migrate();
            assertThat(db.queryForObject("SELECT COUNT(*) FROM companion_request WHERE sender_search_id IS NULL "
                    + "AND status = 'ACCEPTED' AND companion_search_id = 2", Long.class)).isOne();
            String paired = insert + ", sender_search_id) VALUES (?, ?, ?, 'PENDING', NOW(), NOW(), ?)";
            db.update(paired, 10, 20, 2, 1);
            assertThatThrownBy(() -> db.update(paired, 20, 10, 1, 2)).isInstanceOf(DataAccessException.class);
        }
    }

    @Test
    void upgradesVersionEighteenPreservingTourPlacesAndAllowsCustomPlaces() {
        try (MySQLContainer mysql = new MySQLContainer("mysql:8.4")
                .withCommand("--log-bin-trust-function-creators=1")) {
            mysql.start();
            DriverManagerDataSource dataSource = new DriverManagerDataSource(
                    mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword());
            Flyway.configure().dataSource(dataSource).target("18").load().migrate();
            JdbcTemplate legacy = new JdbcTemplate(dataSource);
            legacy.update("""
                    INSERT INTO place (id, source, source_id, name, category, source_category_code, latitude, longitude,
                        created_at, updated_at)
                    VALUES (1, 'TOUR', '126081', '송정해수욕장', 'ATTRACTION', '12', 35.1786, 129.1997, NOW(), NOW())
                    """);

            assertThat(Flyway.configure().dataSource(dataSource).target("19").load().migrate().migrationsExecuted).isEqualTo(1);
            assertThat(legacy.queryForMap("SELECT source, source_id, category, source_category_code FROM place WHERE id = 1"))
                    .containsEntry("source", "TOUR")
                    .containsEntry("source_id", "126081")
                    .containsEntry("category", "ATTRACTION")
                    .containsEntry("source_category_code", "12");

            String insertCustomPlace = """
                    INSERT INTO place (source, name, latitude, longitude, created_at, updated_at)
                    VALUES ('CUSTOM', '숙소', 33.4996213, 126.5311884, NOW(), NOW())
                    """;
            legacy.update(insertCustomPlace);
            legacy.update(insertCustomPlace);
            assertThat(legacy.queryForObject("SELECT COUNT(*) FROM place WHERE source = 'CUSTOM'", Integer.class))
                    .isEqualTo(2);
            assertThatThrownBy(() -> legacy.update("""
                    INSERT INTO place (source, name, latitude, longitude, created_at, updated_at)
                    VALUES ('TOUR', '원본 ID 없는 장소', 35.1786, 129.1997, NOW(), NOW())
                    """)).isInstanceOf(DataAccessException.class).hasMessageContaining("ck_place_source_reference");
            assertThatThrownBy(() -> legacy.update("""
                    INSERT INTO place (source, source_id, name, latitude, longitude, created_at, updated_at)
                    VALUES ('CUSTOM', '126082', '원본 ID가 있는 직접 추가 장소', 35.1786, 129.1997, NOW(), NOW())
                    """)).isInstanceOf(DataAccessException.class).hasMessageContaining("ck_place_source_reference");
        }
    }

    @Test
    @Transactional
    void jpaPersistsAuditingEnumsAndTravelPeriod() {
        User user = User.builder().nickname("테스트 회원").role(Role.USER).build();
        entityManager.persist(user);
        TravelPeriod period = new TravelPeriod(LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 4));
        Long destinationId = entityManager.createQuery(
                "select d.id from Destination d where d.name = :name", Long.class)
                .setParameter("name", "제주").getSingleResult();
        Room room = Room.createForTrip();
        entityManager.persist(room);
        Trip trip = new Trip(user.getId(), destinationId, period, "제주 여행");
        trip.linkRoom(room.getId());
        entityManager.persist(trip);
        entityManager.flush();
        Object tripId = entityManagerFactory.getPersistenceUnitUtil().getIdentifier(trip);
        entityManager.clear();
        assertThat(entityManager.find(Trip.class, tripId)).isNotNull();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE created_at IS NOT NULL AND updated_at IS NOT NULL",
                Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM trip WHERE start_date = ? AND end_date = ?",
                Integer.class, period.startDate(), period.endDate())).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM trip WHERE room_id = ? AND created_by = ?",
                Integer.class, room.getId(), user.getId())).isEqualTo(1);
    }

    @Test
    @Transactional
    void databaseEnforcesUniqueAndForeignKeysWhileAllowingNullableEmail() {
        String insertUser = "INSERT INTO users (email, nickname, role, created_at, updated_at) VALUES (?, 'member', 'USER', NOW(), NOW())";
        jdbc.update(insertUser, "member@example.test");
        assertThatThrownBy(() -> jdbc.update(insertUser, "member@example.test"))
                .isInstanceOf(DataIntegrityViolationException.class);
        jdbc.update(insertUser, (Object) null);
        jdbc.update(insertUser, (Object) null);

        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO user_credential (user_id, password_hash, created_at, updated_at)
                VALUES (-1, 'hash', NOW(), NOW())
                """)).isInstanceOf(DataIntegrityViolationException.class);

        Long userId = jdbc.queryForObject("SELECT id FROM users WHERE email = 'member@example.test'", Long.class);
        jdbc.update("INSERT INTO room (room_type, created_at, updated_at) VALUES ('TRIP', NOW(), NOW())");
        Long roomId = jdbc.queryForObject("SELECT id FROM room", Long.class);
        String insertMember = """
                INSERT INTO room_member (room_id, user_id, status, notification_muted, joined_at, created_at, updated_at)
                VALUES (?, ?, 'JOINED', 0, NOW(), NOW(), NOW())
                """;
        jdbc.update(insertMember, roomId, userId);
        assertThatThrownBy(() -> jdbc.update(insertMember, roomId, userId))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
