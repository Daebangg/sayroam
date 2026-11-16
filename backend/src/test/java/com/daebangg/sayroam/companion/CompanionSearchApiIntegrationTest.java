package com.daebangg.sayroam.companion;

import com.daebangg.sayroam.auth.application.port.out.AccessTokenIssuer;
import com.daebangg.sayroam.support.DatabaseIntegrationTest;
import com.daebangg.sayroam.user.domain.Role;
import com.daebangg.sayroam.user.domain.User;
import com.daebangg.sayroam.user.domain.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@Transactional
class CompanionSearchApiIntegrationTest extends DatabaseIntegrationTest {
    private static final String PATH = "/api/companion-searches";
    private static final String MINE = PATH + "/me";

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager entityManager;
    @Autowired UserRepository users;
    @Autowired AccessTokenIssuer tokens;

    private String authorization;
    private long seoulId;

    @BeforeEach
    void createMember() {
        authorization = createAuthorization("등록자");
        seoulId = destinationId("서울");
    }

    @Test
    void registersListsUpdatesAndClosesOnlyTheOwnersSearch() throws Exception {
        String conditions = conditions(seoulId, "2030-04-21", "2030-04-23",
                """
                , "preferredGender": "FEMALE", "preferredMinAge": 25, "title": "  카페 투어  "
                """);
        mvc.perform(post(PATH).servletPath(PATH).contentType(MediaType.APPLICATION_JSON).content(conditions))
                .andExpect(status().isUnauthorized());

        submit(post(PATH), PATH, authorization, conditions).andExpect(status().isCreated())
                .andExpect(jsonPath("$.destinationId").value(seoulId))
                .andExpect(jsonPath("$.title").value("카페 투어"))
                .andExpect(jsonPath("$.preferredGender").value("FEMALE"))
                .andExpect(jsonPath("$.preferredMinAge").value(25))
                .andExpect(jsonPath("$.preferredMaxAge").value(nullValue()))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
        long id = jdbc.queryForObject("SELECT id FROM companion_search WHERE destination_id = ?", Long.class, seoulId);
        String resource = PATH + "/" + id;

        mvc.perform(get(MINE).servletPath(MINE).header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.searches.length()").value(1))
                .andExpect(jsonPath("$.searches[0].title").value("카페 투어"))
                .andExpect(jsonPath("$.searches[0].id").value(id));
        String other = createAuthorization("다른 회원");
        mvc.perform(get(MINE).servletPath(MINE).header("Authorization", other))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.searches").isEmpty());
        submit(put(resource), resource, other, conditions).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COMPANION_SEARCH_NOT_FOUND"));
        submit(patch(resource), resource, other, "{\"status\":\"CLOSED\"}").andExpect(status().isNotFound());

        long busanId = destinationId("부산");
        String updated = conditions(busanId, "2030-04-24", "2030-04-24", ", \"title\": \"해변 산책\"");
        submit(put(resource), resource, authorization, updated).andExpect(status().isOk())
                .andExpect(jsonPath("$.destinationId").value(busanId))
                .andExpect(jsonPath("$.title").value("해변 산책"))
                .andExpect(jsonPath("$.preferredGender").value(nullValue()))
                .andExpect(jsonPath("$.preferredMinAge").value(nullValue()));
        submit(put(resource), resource, authorization, conditions(busanId, "2030-04-24", "2030-04-24",
                ", \"title\": \"   \"")).andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value(nullValue()));
        entityManager.flush();
        entityManager.clear();

        submit(patch(resource), resource, authorization, "{\"status\":\"ACTIVE\"}")
                .andExpect(status().isBadRequest());
        submit(patch(resource), resource, authorization, "{\"status\":\"CLOSED\"}").andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));
        submit(patch(resource), resource, authorization, "{\"status\":\"CLOSED\"}").andExpect(status().isOk());
        submit(put(resource), resource, authorization, updated).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("COMPANION_SEARCH_CLOSED"));

        entityManager.flush();
        assertThat(jdbc.queryForMap("SELECT destination_id, status, title FROM companion_search WHERE id = ?", id))
                .containsEntry("destination_id", busanId)
                .containsEntry("status", "CLOSED")
                .containsEntry("title", null);
    }

    @Test
    void countsUnicodeCodePointsInTitle() throws Exception {
        String title = "😀".repeat(80);
        submit(post(PATH), PATH, authorization, conditions(seoulId, "2030-04-21", "2030-04-23",
                ", \"title\": \"  " + title + "  \"")).andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value(title));
        submit(post(PATH), PATH, authorization, conditions(seoulId, "2030-04-21", "2030-04-23",
                ", \"title\": \"" + title + "😀\"")).andExpect(status().isBadRequest());
    }

    @Test
    void rejectsInvalidDatesAgesAndUnavailableDestinations() throws Exception {
        submit(post(PATH), PATH, authorization, conditions(seoulId, "2030-04-21", "2030-04-23",
                ", \"title\": \"" + "가".repeat(81) + "\"")).andExpect(status().isBadRequest());
        submit(post(PATH), PATH, authorization, conditions(seoulId, "2030-04-21", "2030-04-20", ""))
                .andExpect(status().isBadRequest());
        submit(post(PATH), PATH, authorization, conditions(seoulId, "2030-04-21", "2030-04-23",
                ", \"preferredMinAge\": 40, \"preferredMaxAge\": 30"))
                .andExpect(status().isBadRequest());
        submit(post(PATH), PATH, authorization, conditions(Long.MAX_VALUE, "2030-04-21", "2030-04-23", ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("DESTINATION_UNAVAILABLE"));

        jdbc.update("UPDATE destination SET is_active = 0 WHERE id = ?", seoulId);
        submit(post(PATH), PATH, authorization, conditions(seoulId, "2030-04-21", "2030-04-23", ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("DESTINATION_UNAVAILABLE"));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM companion_search", Integer.class)).isZero();
    }

    private String createAuthorization(String nickname) {
        User user = users.saveNewAccount(User.builder().nickname(nickname).role(Role.USER).build());
        return "Bearer " + tokens.issue(user.getId(), user.getAuthVersion()).accessToken();
    }

    private long destinationId(String name) {
        return jdbc.queryForObject("SELECT id FROM destination WHERE name = ?", Long.class, name);
    }

    private ResultActions submit(MockHttpServletRequestBuilder request, String path, String token, String body)
            throws Exception {
        return mvc.perform(request.servletPath(path).header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private String conditions(long destinationId, String startDate, String endDate, String preferences) {
        return """
                {"destinationId": %d, "startDate": "%s", "endDate": "%s"%s}
                """.formatted(destinationId, startDate, endDate, preferences);
    }
}
