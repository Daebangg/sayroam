package com.daebangg.sayroam;

import com.daebangg.sayroam.common.domain.TravelPeriod;
import com.daebangg.sayroam.support.DatabaseIntegrationTest;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.mysql.MySQLContainer;

import java.time.LocalDate;

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
        assertThat(entityManagerFactory.getMetamodel().getEntities()).hasSize(19);
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM information_schema.tables
                WHERE table_schema = DATABASE() AND table_type = 'BASE TABLE'
                """, Integer.class)).isEqualTo(20);
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("10");
        flyway.validate();
        assertThat(flyway.migrate().migrationsExecuted).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM flyway_schema_history WHERE success = 1",
                Integer.class)).isEqualTo(10);
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
            assertThat(Flyway.configure().dataSource(dataSource).load().migrate().migrationsExecuted).isEqualTo(1);
            assertThat(legacy.queryForObject("SELECT COUNT(*) FROM destination", Integer.class)).isEqualTo(160);
            assertThat(legacy.queryForObject("SELECT score FROM user_travel_style WHERE user_id = 10 AND item_id = 20",
                    Integer.class)).isEqualTo(3);
        }
    }

    @Test
    @Transactional
    void jpaPersistsAuditingEnumsAndTravelPeriod() {
        User user = User.builder().nickname("테스트 회원").role(Role.USER).build();
        entityManager.persist(user);
        TravelPeriod period = new TravelPeriod(LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 4));
        Trip trip = new Trip(user.getId(), period);
        entityManager.persist(trip);
        entityManager.flush();
        Object tripId = entityManagerFactory.getPersistenceUnitUtil().getIdentifier(trip);
        entityManager.clear();
        assertThat(entityManager.find(Trip.class, tripId)).isNotNull();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE created_at IS NOT NULL AND updated_at IS NOT NULL",
                Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM trip WHERE start_date = ? AND end_date = ?",
                Integer.class, period.startDate(), period.endDate())).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM trip WHERE room_id IS NULL AND created_by = ?",
                Integer.class, user.getId())).isEqualTo(1);
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
        jdbc.update("INSERT INTO room (created_at, updated_at) VALUES (NOW(), NOW())");
        Long roomId = jdbc.queryForObject("SELECT id FROM room", Long.class);
        String insertMember = """
                INSERT INTO room_member (room_id, user_id, role, status, notification_muted, joined_at, created_at, updated_at)
                VALUES (?, ?, 'MEMBER', 'JOINED', 0, NOW(), NOW(), NOW())
                """;
        jdbc.update(insertMember, roomId, userId);
        assertThatThrownBy(() -> jdbc.update(insertMember, roomId, userId))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
