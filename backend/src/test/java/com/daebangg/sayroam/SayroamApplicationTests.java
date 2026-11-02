package com.daebangg.sayroam;

import com.daebangg.sayroam.room.domain.Room;
import com.daebangg.sayroam.room.domain.RoomType;
import com.daebangg.sayroam.trip.domain.Trip;
import com.daebangg.sayroam.user.domain.Role;
import com.daebangg.sayroam.user.domain.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceContext;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Testcontainers
class SayroamApplicationTests {

    @Container
    static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.4");

    static Flyway flyway;

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        // CLI와 동일하게 앱 컨텍스트 시작 전에 명시적으로 스키마를 적용한다.
        flyway = Flyway.configure()
                .dataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword())
                .cleanDisabled(true)
                .validateMigrationNaming(true)
                .load();
        flyway.migrate();
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }

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
        assertThat(entityManagerFactory.getMetamodel().getEntities()).hasSize(17);
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM information_schema.tables
                WHERE table_schema = DATABASE() AND table_type = 'BASE TABLE'
                """, Integer.class)).isEqualTo(18);
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("1");
        flyway.validate();
        assertThat(flyway.migrate().migrationsExecuted).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM flyway_schema_history WHERE success = 1",
                Integer.class)).isEqualTo(1);
    }

    @Test
    @Transactional
    void jpaPersistsAuditingEnumsAndOptionalTravelPeriod() {
        User user = User.builder().nickname("테스트 회원").role(Role.USER).build();
        Room room = Room.builder().type(RoomType.TRIP).name("날짜 미정 여행").build();
        entityManager.persist(user);
        entityManager.persist(room);
        Long roomId = (Long) entityManagerFactory.getPersistenceUnitUtil().getIdentifier(room);
        Trip trip = Trip.builder().roomId(roomId).build();
        entityManager.persist(trip);
        entityManager.flush();
        Object tripId = entityManagerFactory.getPersistenceUnitUtil().getIdentifier(trip);
        entityManager.clear();
        assertThat(entityManager.find(Trip.class, tripId)).isNotNull();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE created_at IS NOT NULL AND updated_at IS NOT NULL",
                Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM trip WHERE start_date IS NULL AND end_date IS NULL",
                Integer.class)).isEqualTo(1);
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
        jdbc.update("INSERT INTO room (type, created_at, updated_at) VALUES ('DIRECT', NOW(), NOW())");
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
