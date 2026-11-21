package com.daebangg.sayroam.companion;

import com.daebangg.sayroam.auth.application.port.out.AccessTokenIssuer;
import com.daebangg.sayroam.companion.application.service.CompanionMatchingService;
import com.daebangg.sayroam.companion.application.dto.CompanionMatchingClaim;
import com.daebangg.sayroam.support.DatabaseIntegrationTest;
import com.daebangg.sayroam.user.domain.Role;
import com.daebangg.sayroam.user.domain.User;
import com.daebangg.sayroam.user.domain.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.contains;
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
    @Autowired CompanionMatchingService matching;

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
                .andExpect(jsonPath("$.showAge").value(false))
                .andExpect(jsonPath("$.showGender").value(false))
                .andExpect(jsonPath("$.introduction").value(nullValue()))
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
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
        submit(patch(resource), resource, authorization, "{\"status\":\"CLOSED\"}").andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));
        submit(patch(resource), resource, authorization, "{\"status\":\"CLOSED\"}").andExpect(status().isOk());
        submit(put(resource), resource, authorization, updated).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));

        entityManager.flush();
        assertThat(jdbc.queryForMap("SELECT destination_id, status, title FROM companion_search WHERE id = ?", id))
                .containsEntry("destination_id", busanId)
                .containsEntry("status", "CLOSED")
                .containsEntry("title", "해변 산책");
    }

    @Test
    void savesRecruitmentStyleWithoutProfileAndKeepsBothRecruitmentsIndependent() throws Exception {
        jdbc.update("DELETE FROM user_travel_style");
        String style = ", \"travelStyle\": {\"planning\":1,\"daily-pace\":2,\"time-together\":4,\"spending\":5}";
        submit(post(PATH), PATH, authorization, conditions(seoulId, "2030-04-21", "2030-04-23", style))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.travelStyle.planning").value(1))
                .andExpect(jsonPath("$['travelStyle']['daily-pace']").value(2));
        long id = jdbc.queryForObject("SELECT MAX(id) FROM companion_search", Long.class);
        String resource = PATH + "/" + id;
        submit(post(PATH), PATH, authorization, conditions(seoulId, "2030-04-21", "2030-04-23",
                style.replace("planning\":1", "planning\":5"))).andExpect(status().isCreated());
        submit(put(resource), resource, authorization, conditions(seoulId, "2030-04-21", "2030-04-23", ""))
                .andExpect(status().isOk()).andExpect(jsonPath("$.travelStyle.planning").value(1));
        submit(put(resource), resource, authorization, conditions(seoulId, "2030-04-21", "2030-04-23",
                style.replace("planning\":1", "planning\":3")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.travelStyle.planning").value(3));
        entityManager.flush();
        entityManager.clear();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM user_travel_style", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT style_planning FROM companion_search WHERE id <> ?", Integer.class, id)).isEqualTo(5);
        submit(patch(resource), resource, authorization, "{\"status\":\"CLOSED\"}").andExpect(status().isOk());
        submit(patch(resource), resource, authorization, "{\"status\":\"ACTIVE\"}").andExpect(status().isOk());
    }

    @Test
    void snapshotsProfileOnlyAtCreationAndLetsLegacyClosedSearchCompleteItsStyle() throws Exception {
        submit(post(PATH), PATH, authorization, conditions(seoulId, "2030-04-21", "2030-04-23", ""))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.travelStyle.planning").value(3));
        long id = jdbc.queryForObject("SELECT MAX(id) FROM companion_search", Long.class);
        String resource = PATH + "/" + id;
        jdbc.update("UPDATE user_travel_style SET score = 5");
        entityManager.clear();
        mvc.perform(get(MINE).servletPath(MINE).header("Authorization", authorization))
                .andExpect(status().isOk()).andExpect(jsonPath("$.searches[0].travelStyle.planning").value(3));
        jdbc.update("UPDATE companion_search SET status = 'CLOSED', style_planning = NULL, style_daily_pace = NULL, style_time_together = NULL, style_spending = NULL WHERE id = ?", id);
        entityManager.clear();
        submit(patch(resource), resource, authorization, "{\"status\":\"ACTIVE\"}")
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("TRAVEL_STYLE_REQUIRED"));
        submit(put(resource), resource, authorization, conditions(seoulId, "2030-04-21", "2030-04-23", ""))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("TRAVEL_STYLE_REQUIRED"));
        submit(put(resource), resource, authorization, conditions(seoulId, "2030-04-21", "2030-04-23",
                ", \"travelStyle\": {\"planning\":1,\"daily-pace\":2,\"time-together\":3,\"spending\":4}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CLOSED"));
        submit(patch(resource), resource, authorization, "{\"status\":\"ACTIVE\"}").andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT MIN(score) FROM user_travel_style", Integer.class)).isEqualTo(5);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "6", "1.5", "\"3\"", "true", "2147483648"})
    void rejectsInvalidRecruitmentStyleScoresWithoutSaving(String score) throws Exception {
        submit(post(PATH), PATH, authorization, conditions(seoulId, "2030-04-21", "2030-04-23",
                ", \"travelStyle\": {\"planning\":" + score + ",\"daily-pace\":2,\"time-together\":3,\"spending\":4}"))
                .andExpect(status().isBadRequest());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM companion_search", Integer.class)).isZero();
    }

    @Test
    void rejectsIncompleteAndUnknownRecruitmentStyleWithoutFallingBackToProfile() throws Exception {
        submit(post(PATH), PATH, authorization, conditions(seoulId, "2030-04-21", "2030-04-23",
                ", \"travelStyle\": {\"planning\":3}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("TRAVEL_STYLE_REQUIRED"));
        submit(post(PATH), PATH, authorization, conditions(seoulId, "2030-04-21", "2030-04-23",
                ", \"travelStyle\": {\"planning\":1,\"daily-pace\":2,\"time-together\":3,\"spending\":4,\"unknown\":5}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void publishesTheRecruitmentStyleContractWithItsQuestionCodes() throws Exception {
        ResultActions contract = mvc.perform(get("/v3/api-docs").servletPath("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.schemas.RecruitmentTravelStyleRequest.properties.planning").exists())
                .andExpect(jsonPath("$.components.schemas.RecruitmentTravelStyleRequest.properties['daily-pace']").exists())
                .andExpect(jsonPath("$.components.schemas.RecruitmentTravelStyleRequest.properties['time-together']").exists())
                .andExpect(jsonPath("$.components.schemas.RecruitmentTravelStyleRequest.properties.spending").exists())
                .andExpect(jsonPath("$.components.schemas.CompanionSearchResponse.properties.travelStyle").exists());
        for (String question : List.of("planning", "dailyPace", "timeTogether", "spending")) {
            contract.andExpect(jsonPath("$.paths['/api/companion-candidates'].get.parameters[?(@.name == '"
                    + question + "')].schema.type").value(contains("integer")));
        }
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
    void savesAndEditsVisibilityIndependentlyAndValidatesIntroductionByCodePoint() throws Exception {
        String introduction = "😀".repeat(200);
        submit(post(PATH), PATH, authorization, conditions(seoulId, "2030-04-21", "2030-04-23",
                ", \"introduction\": \"  " + introduction + "  \", \"showAge\": true, \"showGender\": true"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.introduction").value(introduction))
                .andExpect(jsonPath("$.showAge").value(true))
                .andExpect(jsonPath("$.showGender").value(true));
        long id = jdbc.queryForObject("SELECT id FROM companion_search WHERE destination_id = ?", Long.class, seoulId);
        String resource = PATH + "/" + id;

        submit(put(resource), resource, authorization, conditions(seoulId, "2030-04-21", "2030-04-23",
                ", \"introduction\": \"" + introduction + "😀\", \"showAge\": false"))
                .andExpect(status().isBadRequest());
        submit(put(resource), resource, authorization, conditions(seoulId, "2030-04-21", "2030-04-23", ""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.introduction").value(introduction))
                .andExpect(jsonPath("$.showAge").value(true))
                .andExpect(jsonPath("$.showGender").value(true));
        submit(put(resource), resource, authorization, conditions(seoulId, "2030-04-21", "2030-04-23",
                ", \"introduction\": \" 새 소개글 \", \"showAge\": false"))
                .andExpect(status().isOk());
        mvc.perform(get(MINE).servletPath(MINE).header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.searches[0].introduction").value("새 소개글"))
                .andExpect(jsonPath("$.searches[0].showAge").value(false))
                .andExpect(jsonPath("$.searches[0].showGender").value(true));
        submit(put(resource), resource, authorization, conditions(seoulId, "2030-04-21", "2030-04-23",
                ", \"introduction\": \"   \"")).andExpect(status().isOk())
                .andExpect(jsonPath("$.introduction").value(nullValue()));
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

    @Test
    void exposesPendingProcessingRetryAndCompletedWithoutCandidates() throws Exception {
        jdbc.update("DELETE FROM companion_matching_job");
        submit(post(PATH), PATH, authorization, conditions(seoulId, "2030-04-21", "2030-04-23", ""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.matchingRevision").value(2))
                .andExpect(jsonPath("$.matchingStatus").value("PENDING"));
        long id = jdbc.queryForObject("SELECT MAX(id) FROM companion_search", Long.class);
        String path = PATH + "/" + id + "/matching-status";
        matchingState(path, "PENDING", 2);
        long jobs = jdbc.queryForObject("SELECT COUNT(*) FROM companion_matching_job", Long.class);
        matchingState(path, "PENDING", 2);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM companion_matching_job", Long.class)).isEqualTo(jobs);
        CompanionMatchingClaim claim = matching.claim().orElseThrow();
        matchingState(path, "PROCESSING", 2);
        matching.retry(claim);
        matchingState(path, "RETRY", 2);
        entityManager.flush();
        jdbc.update("UPDATE companion_matching_job SET available_at = UTC_TIMESTAMP(6) - INTERVAL 1 SECOND WHERE id = ?", claim.jobId());
        entityManager.clear();
        matching.process(matching.claim().orElseThrow());
        matchingState(path, "SUCCEEDED", 2);
        String candidates = PATH + "/" + id + "/candidates";
        mvc.perform(get(candidates).servletPath(candidates).header("Authorization", authorization))
                .andExpect(status().isOk()).andExpect(jsonPath("$.candidates").isEmpty());
        mvc.perform(get(MINE).servletPath(MINE).header("Authorization", authorization))
                .andExpect(status().isOk()).andExpect(jsonPath("$.searches[0].matchingStatus").value("SUCCEEDED"));
    }

    @Test
    void matchingStateIsOwnerScopedAndOnlyUsesTheCurrentRevision() throws Exception {
        jdbc.update("DELETE FROM companion_matching_job");
        String body = conditions(seoulId, "2030-04-21", "2030-04-23", "");
        submit(post(PATH), PATH, authorization, body).andExpect(status().isCreated());
        long id = jdbc.queryForObject("SELECT MAX(id) FROM companion_search", Long.class);
        String resource = PATH + "/" + id;
        String path = resource + "/matching-status";
        mvc.perform(get(path).servletPath(path)).andExpect(status().isUnauthorized());
        mvc.perform(get(path).servletPath(path).header("Authorization", createAuthorization("상태 조회 타인")))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("COMPANION_SEARCH_NOT_FOUND"));
        String missing = PATH + "/" + Long.MAX_VALUE + "/matching-status";
        mvc.perform(get(missing).servletPath(missing).header("Authorization", authorization))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("COMPANION_SEARCH_NOT_FOUND"));
        CompanionMatchingClaim old = matching.claim().orElseThrow();
        submit(put(resource), resource, authorization, body).andExpect(status().isOk())
                .andExpect(jsonPath("$.matchingRevision").value(3))
                .andExpect(jsonPath("$.matchingStatus").value("PENDING"));
        matching.process(old);
        matchingState(path, "PENDING", 3);
        matching.process(matching.claim().orElseThrow());
        matchingState(path, "SUCCEEDED", 3);
        submit(patch(resource), resource, authorization, "{\"status\":\"CLOSED\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.matchingStatus").value("SKIPPED"));
        matchingState(path, "SKIPPED", 4);
        submit(patch(resource), resource, authorization, "{\"status\":\"ACTIVE\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.matchingStatus").value("PENDING"));
        matchingState(path, "PENDING", 5);
    }

    @Test
    void distinguishesMissingJobsExpiredAndLegacySearches() throws Exception {
        submit(post(PATH), PATH, authorization, conditions(seoulId, "2030-04-21", "2030-04-23", ""))
                .andExpect(status().isCreated());
        long id = jdbc.queryForObject("SELECT MAX(id) FROM companion_search", Long.class);
        String path = PATH + "/" + id + "/matching-status";
        jdbc.update("DELETE FROM companion_matching_job WHERE search_id = ?", id);
        entityManager.clear();
        matchingState(path, "NOT_REQUESTED", 2);
        jdbc.update("UPDATE companion_search SET start_date = '2000-01-01', end_date = '2000-01-02' WHERE id = ?", id);
        entityManager.clear();
        matchingState(path, "SKIPPED", 2);
        jdbc.update("UPDATE companion_search SET start_date = '2030-04-21', end_date = '2030-04-23', destination_id = NULL, region = '기존 지역' WHERE id = ?", id);
        entityManager.clear();
        matchingState(path, "SKIPPED", 2);
    }

    private void matchingState(String path, String state, long revision) throws Exception {
        mvc.perform(get(path).servletPath(path).header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matchingRevision").value(revision))
                .andExpect(jsonPath("$.status").value(state));
    }

    private String createAuthorization(String nickname) {
        User user = users.saveNewAccount(User.builder().nickname(nickname).role(Role.USER).build());
        jdbc.update("""
                INSERT INTO user_travel_style (user_id, item_id, score, created_at, updated_at)
                SELECT ?, id, 3, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)
                FROM travel_style_item WHERE is_active = TRUE
                  AND code IN ('planning', 'daily-pace', 'time-together', 'spending')
                """, user.getId());
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
