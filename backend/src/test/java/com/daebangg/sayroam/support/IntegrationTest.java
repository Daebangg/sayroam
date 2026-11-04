package com.daebangg.sayroam.support;

import org.flywaydb.core.Flyway;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.mysql.MySQLContainer;

/**
 * 실제 MySQL로 검증하는 통합 테스트의 공통 설정.
 * 컨테이너는 테스트 실행 동안 한 번만 띄워 모든 통합 테스트가 공유하고, 종료는 Testcontainers가 맡는다.
 * 테스트에 @Transactional을 붙이지 않고 커밋된 결과를 검증하므로, 데이터는 각 테스트에서 정리한다.
 */
@SpringBootTest
public abstract class IntegrationTest {

    static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.4");

    static {
        MYSQL.start();
        // 앱 설정에서 Flyway가 꺼져 있으므로 컨텍스트 시작 전에 직접 스키마를 적용한다.
        Flyway.configure()
                .dataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword())
                .cleanDisabled(true)
                .validateMigrationNaming(true)
                .load()
                .migrate();
    }

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }
}
