package com.daebangg.sayroam;

import com.daebangg.sayroam.common.domain.TravelPeriod;
import com.daebangg.sayroam.support.DatabaseIntegrationTest;
import com.daebangg.sayroam.trip.domain.Trip;
import com.daebangg.sayroam.user.domain.entity.User;
import com.daebangg.sayroam.user.domain.type.Role;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

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
        assertThat(entityManagerFactory.getMetamodel().getEntities()).hasSize(18);
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM information_schema.tables
                WHERE table_schema = DATABASE() AND table_type = 'BASE TABLE'
                """, Integer.class)).isEqualTo(19);
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("8");
        flyway.validate();
        assertThat(flyway.migrate().migrationsExecuted).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM flyway_schema_history WHERE success = 1",
                Integer.class)).isEqualTo(8);
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
