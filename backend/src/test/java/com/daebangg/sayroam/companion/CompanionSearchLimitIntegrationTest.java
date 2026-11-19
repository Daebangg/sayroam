package com.daebangg.sayroam.companion;

import com.daebangg.sayroam.auth.application.port.out.AccessTokenIssuer;
import com.daebangg.sayroam.support.DatabaseIntegrationTest;
import com.daebangg.sayroam.user.domain.Role;
import com.daebangg.sayroam.user.domain.User;
import com.daebangg.sayroam.user.domain.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class CompanionSearchLimitIntegrationTest extends DatabaseIntegrationTest {
    private static final String PATH = "/api/companion-searches";
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired UserRepository users;
    @Autowired AccessTokenIssuer tokens;

    private long userId;
    private long destinationId;
    private String authorization;

    @BeforeEach
    void setUp() {
        User user = users.saveNewAccount(User.builder().nickname("모집 한도 검증").role(Role.USER).build());
        userId = user.getId();
        jdbc.update("""
                INSERT INTO user_travel_style (user_id, item_id, score, created_at, updated_at)
                SELECT ?, id, 3, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)
                FROM travel_style_item WHERE is_active = TRUE
                  AND code IN ('planning', 'daily-pace', 'time-together', 'spending')
                """, userId);
        destinationId = jdbc.queryForObject("SELECT id FROM destination WHERE name = '서울'", Long.class);
        authorization = "Bearer " + tokens.issue(userId, user.getAuthVersion()).accessToken();
    }

    @AfterEach
    void cleanUp() {
        jdbc.update("DELETE FROM companion_search WHERE user_id = ?", userId);
        jdbc.update("DELETE FROM user_travel_style WHERE user_id = ?", userId);
        jdbc.update("DELETE FROM users WHERE id = ?", userId);
    }

    @Test
    void allowsThreeActiveSearchesAndRejectsCreationAndReopeningAtTheLimit() throws Exception {
        for (int index = 0; index < 3; index++) {
            assertThat(create().getResponse().getStatus()).isEqualTo(201);
        }
        long closed = seed("CLOSED");
        long anotherClosed = seed("CLOSED");
        assertLimited(create());
        assertLimited(changeStatus(closed, "ACTIVE"));
        assertThat(activeCount()).isEqualTo(3);

        long active = jdbc.queryForObject("""
                SELECT MIN(id) FROM companion_search WHERE user_id = ? AND status = 'ACTIVE'
                """, Long.class, userId);
        assertThat(changeStatus(active, "ACTIVE").getResponse().getStatus()).isEqualTo(200);
        mvc.perform(get(PATH + "/me").servletPath(PATH + "/me").header("Authorization", authorization))
                .andExpect(status().isOk()).andExpect(jsonPath("$.searches.length()").value(5));
        String resource = PATH + "/" + active;
        mvc.perform(put(resource).servletPath(resource).header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON).content(conditions()))
                .andExpect(status().isOk());
        assertThat(changeStatus(active, "CLOSED").getResponse().getStatus()).isEqualTo(200);
        assertThat(changeStatus(closed, "ACTIVE").getResponse().getStatus()).isEqualTo(200);
        assertThat(changeStatus(closed, "ACTIVE").getResponse().getStatus()).isEqualTo(200);
        assertLimited(changeStatus(anotherClosed, "ACTIVE"));
        assertThat(activeCount()).isEqualTo(3);
    }

    @Test
    void permitsReducingLegacyExcessWithoutBlockingExistingActiveSearches() throws Exception {
        long first = seed("ACTIVE");
        long second = seed("ACTIVE");
        seed("ACTIVE");
        seed("ACTIVE");
        long closed = seed("CLOSED");
        assertLimited(create());
        assertLimited(changeStatus(closed, "ACTIVE"));
        assertThat(changeStatus(first, "ACTIVE").getResponse().getStatus()).isEqualTo(200);
        assertThat(changeStatus(first, "CLOSED").getResponse().getStatus()).isEqualTo(200);
        assertLimited(create());
        assertThat(changeStatus(second, "CLOSED").getResponse().getStatus()).isEqualTo(200);
        assertThat(create().getResponse().getStatus()).isEqualTo(201);
        assertThat(activeCount()).isEqualTo(3);
    }

    @ParameterizedTest
    @CsvSource({"true,true", "false,false", "true,false"})
    void serializesConcurrentCreatesAndReopens(boolean firstCreates, boolean secondCreates) throws Exception {
        seed("ACTIVE");
        seed("ACTIVE");
        long firstClosed = seed("CLOSED");
        long secondClosed = seed("CLOSED");
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<MvcResult> first = executor.submit(() -> {
                ready.countDown();
                if (!start.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Start timeout");
                return firstCreates ? create() : changeStatus(firstClosed, "ACTIVE");
            });
            Future<MvcResult> second = executor.submit(() -> {
                ready.countDown();
                if (!start.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Start timeout");
                return secondCreates ? create() : changeStatus(secondClosed, "ACTIVE");
            });
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            List<MvcResult> responses = List.of(first.get(20, TimeUnit.SECONDS), second.get(20, TimeUnit.SECONDS));
            assertThat(responses.stream().filter(response -> response.getResponse().getStatus() < 300).count())
                    .isEqualTo(1);
            for (MvcResult response : responses) {
                if (response.getResponse().getStatus() >= 300) assertLimited(response);
            }
        }
        assertThat(activeCount()).isEqualTo(3);
    }

    private MvcResult create() throws Exception {
        return mvc.perform(post(PATH).servletPath(PATH).header("Authorization", authorization)
                .contentType(MediaType.APPLICATION_JSON).content(conditions())).andReturn();
    }

    private MvcResult changeStatus(long id, String status) throws Exception {
        String resource = PATH + "/" + id;
        return mvc.perform(patch(resource).servletPath(resource).header("Authorization", authorization)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"" + status + "\"}")).andReturn();
    }

    private String conditions() {
        return """
                {"destinationId": %d, "startDate": "2030-04-21", "endDate": "2030-04-23"}
                """.formatted(destinationId);
    }

    private long seed(String status) {
        jdbc.update("""
                INSERT INTO companion_search
                    (user_id, destination_id, start_date, end_date, status, created_at, updated_at)
                VALUES (?, ?, '2030-04-21', '2030-04-23', ?, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6))
                """, userId, destinationId, status);
        return jdbc.queryForObject("SELECT MAX(id) FROM companion_search WHERE user_id = ?", Long.class, userId);
    }

    private long activeCount() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM companion_search WHERE user_id = ? AND status = 'ACTIVE'",
                Long.class, userId);
    }

    private void assertLimited(MvcResult response) throws Exception {
        assertThat(response.getResponse().getStatus()).isEqualTo(409);
        jsonPath("$.code").value("COMPANION_SEARCH_LIMIT_EXCEEDED").match(response);
    }
}
