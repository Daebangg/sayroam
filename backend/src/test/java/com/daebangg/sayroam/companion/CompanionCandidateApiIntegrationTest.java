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
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@Transactional
class CompanionCandidateApiIntegrationTest extends DatabaseIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager entityManager;
    @Autowired UserRepository users;
    @Autowired AccessTokenIssuer tokens;

    private LocalDate today;
    private long owner;
    private long seoul;
    private String authorization;

    @BeforeEach
    void setUp() {
        today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        User user = users.saveNewAccount(User.builder().nickname("등록자").role(Role.USER).build());
        owner = user.getId();
        authorization = "Bearer " + tokens.issue(owner, user.getAuthVersion()).accessToken();
        seoul = jdbc.queryForObject("SELECT id FROM destination WHERE name = '서울'", Long.class);
    }

    @Test
    void sortsRegisteredPeriodOverlapsAndDeduplicatesBeforePaging() throws Exception {
        long own = search(owner, -4, 4);
        long traveler = member("여행자", null, null);
        search(traveler, 4, 6);
        long first = search(traveler, -1, 2);
        jdbc.update("UPDATE companion_search SET title = ? WHERE id = ?", "카페 투어", first);
        long second = search(member("두 번째", null, null), 1, 3);
        long third = search(member("세 번째", null, null), 1, 3);
        long boundary = search(member("마지막 날", null, null), 4, 7);
        search(owner, 0, 4);
        long pastOverlap = search(member("지난 일정", null, null), -3, -1);
        search(member("지난 비겹침 일정", null, null), -7, -5);
        search(member("기간 불일치", null, null), 5, 7);

        long closed = search(member("종료한 등록", null, null), 0, 4);
        jdbc.update("UPDATE companion_search SET status = 'CLOSED' WHERE id = ?", closed);
        long differentDestination = search(member("부산 여행자", null, null), 0, 4);
        jdbc.update("""
                UPDATE companion_search SET destination_id = (SELECT id FROM destination WHERE name = '부산')
                WHERE id = ?
                """, differentDestination);
        long deleted = member("탈퇴한 회원", null, null);
        search(deleted, 0, 4);
        jdbc.update("UPDATE users SET deleted_at = UTC_TIMESTAMP(6) WHERE id = ?", deleted);

        candidates(own, 0, 3).andExpect(status().isOk())
                .andExpect(jsonPath("$.candidates[*].searchId").value(contains((int) first, (int) pastOverlap, (int) second)))
                .andExpect(jsonPath("$.candidates[0].overlapStartDate").value(today.minusDays(1).toString()))
                .andExpect(jsonPath("$.candidates[0].overlapDays").value(4))
                .andExpect(jsonPath("$.candidates[0].title").value("카페 투어"))
                .andExpect(jsonPath("$.candidates[1].overlapDays").value(3))
                .andExpect(jsonPath("$.candidates[1].title").value(nullValue()))
                .andExpect(jsonPath("$.candidates[0].profileImageUrl").value(nullValue()))
                .andExpect(jsonPath("$.candidates[0].birthDate").doesNotExist())
                .andExpect(jsonPath("$.hasNext").value(true));
        candidates(own, 1, 3).andExpect(status().isOk())
                .andExpect(jsonPath("$.candidates[*].searchId").value(contains((int) third, (int) boundary)))
                .andExpect(jsonPath("$.candidates[1].overlapDays").value(1))
                .andExpect(jsonPath("$.hasNext").value(false));
        candidates(own, Integer.MAX_VALUE, 100).andExpect(status().isOk())
                .andExpect(jsonPath("$.candidates").isEmpty())
                .andExpect(jsonPath("$.hasNext").value(false));
    }

    @Test
    void listsOverlapsWhenTheOwnersTravelPeriodHasPassed() throws Exception {
        long own = search(owner, -4, -1);
        long match = search(member("지난 기간 후보", null, null), -6, -3);

        candidates(own, 0, 20).andExpect(status().isOk())
                .andExpect(jsonPath("$.candidates[*].searchId").value(contains((int) match)))
                .andExpect(jsonPath("$.candidates[0].overlapStartDate").value(today.minusDays(4).toString()))
                .andExpect(jsonPath("$.candidates[0].overlapEndDate").value(today.minusDays(3).toString()))
                .andExpect(jsonPath("$.candidates[0].overlapDays").value(2));
    }

    @Test
    void appliesBothMembersConditionsAndTreatsMissingProfileAsUnknown() throws Exception {
        jdbc.update("UPDATE users SET gender = 'MALE', birth_date = ? WHERE id = ?", today.minusYears(30), owner);
        long own = search(owner, 1, 3);
        preferences(own, "FEMALE", 25, 25);

        long match = search(member("생일인 후보", "FEMALE", today.minusYears(25)), 1, 3);
        preferences(match, "MALE", 30, null);
        search(member("아직 생일 전", "FEMALE", today.minusYears(25).plusDays(1)), 1, 3);
        search(member("최대 나이 초과", "FEMALE", today.minusYears(26)), 1, 3);
        search(member("성별 미입력", null, today.minusYears(25)), 1, 3);
        long yearOnly = member("연도만 입력", "FEMALE", null);
        jdbc.update("UPDATE users SET birth_year = ? WHERE id = ?", today.getYear() - 25, yearOnly);
        search(yearOnly, 1, 3);
        long reverseGender = search(member("상대 성별 조건", "FEMALE", today.minusYears(25)), 1, 3);
        preferences(reverseGender, "FEMALE", null, null);
        long reverseAge = search(member("상대 나이 조건", "FEMALE", today.minusYears(25)), 1, 3);
        preferences(reverseAge, null, null, 29);

        candidates(own, 0, 20).andExpect(status().isOk())
                .andExpect(jsonPath("$.candidates[*].searchId").value(contains((int) match)));

        jdbc.update("UPDATE users SET gender = NULL, birth_date = NULL WHERE id = ?", owner);
        candidates(own, 0, 20).andExpect(status().isOk()).andExpect(jsonPath("$.candidates").isEmpty());

        preferences(own, null, null, null);
        entityManager.clear();
        long noProfile = search(member("프로필 미입력", null, null), 1, 3);
        candidates(own, 0, 20).andExpect(status().isOk())
                .andExpect(jsonPath("$.candidates[?(@.searchId == " + noProfile + ")]").isNotEmpty());
    }

    @Test
    void checksOwnershipRegistrationStateAndPageBounds() throws Exception {
        long current = search(owner, 0, 1);
        String path = path(current);
        mvc.perform(get(path).servletPath(path)).andExpect(status().isUnauthorized());
        long other = search(member("다른 회원", null, null), 0, 1);
        candidates(other, 0, 20).andExpect(status().isNotFound());
        candidates(current, 0, 101).andExpect(status().isBadRequest());

        jdbc.update("UPDATE companion_search SET destination_id = NULL, region = '기존 지역' WHERE id = ?", current);
        candidates(current, 0, 20).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("COMPANION_SEARCH_DESTINATION_REQUIRED"));
        long closed = search(owner, 0, 1);
        jdbc.update("UPDATE companion_search SET status = 'CLOSED' WHERE id = ?", closed);
        candidates(closed, 0, 20).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("COMPANION_SEARCH_CLOSED"));

        String resource = "/api/companion-searches/" + current;
        mvc.perform(patch(resource).servletPath(resource).header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"CLOSED\"}"))
                .andExpect(status().isOk());
        mvc.perform(patch(resource).servletPath(resource).header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"ACTIVE\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("COMPANION_SEARCH_DESTINATION_REQUIRED"));
    }

    @Test
    void previewsWithOrWithoutPreferencesAndDoesNotRegisterTheViewer() throws Exception {
        jdbc.update("UPDATE users SET gender = 'MALE', birth_date = ? WHERE id = ?", today.minusYears(30), owner);
        long match = search(member("희망 조건 충족", "FEMALE", today.minusYears(25)), -1, 3);
        preferences(match, "MALE", 30, 30);
        long other = search(member("제한 없음", "MALE", today.minusYears(35)), 1, 3);
        long reverseMismatch = search(member("상대 나이 제한", "FEMALE", today.minusYears(25)), 1, 3);
        preferences(reverseMismatch, null, 31, null);

        String path = "/api/companion-candidates";
        mvc.perform(get(path).servletPath(path)).andExpect(status().isUnauthorized());
        String start = today.toString();
        String end = today.plusDays(3).toString();
        mvc.perform(get(path).servletPath(path).header("Authorization", authorization)
                        .param("destinationId", Long.toString(seoul)).param("startDate", start).param("endDate", end))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.candidates[*].searchId").value(contains((int) match, (int) other)));

        mvc.perform(get(path).servletPath(path).header("Authorization", authorization)
                        .param("destinationId", Long.toString(seoul)).param("startDate", start).param("endDate", end)
                        .param("preferredGender", "FEMALE").param("preferredMinAge", "25").param("preferredMaxAge", "25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.candidates[*].searchId").value(contains((int) match)));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM companion_search WHERE user_id = ?", Integer.class, owner))
                .isZero();

        long own = search(owner, 0, 3);
        preferences(own, "FEMALE", 25, 25);
        candidates(own, 0, 20).andExpect(status().isOk())
                .andExpect(jsonPath("$.candidates[*].searchId").value(contains((int) match)));
    }

    @Test
    void filtersByTitleBeforeChoosingOneRegistrationPerMember() throws Exception {
        long own = search(owner, 0, 4);
        long traveler = member("여행자", null, null);
        long longer = search(traveler, 0, 4);
        long titled = search(traveler, 1, 2);
        search(member("제목 없는 회원", null, null), 0, 4);
        jdbc.update("UPDATE companion_search SET title = ? WHERE id = ?", "제주 산책", longer);
        jdbc.update("UPDATE companion_search SET title = ? WHERE id = ?", "제주 카페 투어", titled);

        String registered = path(own);
        mvc.perform(get(registered).servletPath(registered).header("Authorization", authorization).param("q", " 카페 "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.candidates[*].searchId").value(contains((int) titled)))
                .andExpect(jsonPath("$.candidates[0].title").value("제주 카페 투어"));
        mvc.perform(get(registered).servletPath(registered).header("Authorization", authorization).param("q", "%_"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.candidates").isEmpty());

        String preview = "/api/companion-candidates";
        mvc.perform(get(preview).servletPath(preview).header("Authorization", authorization)
                        .param("destinationId", Long.toString(seoul)).param("startDate", today.toString())
                        .param("endDate", today.plusDays(4).toString()).param("q", "카페"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.candidates[*].searchId").value(contains((int) titled)));
    }

    @Test
    void validatesTitleQueryLengthByUnicodeCodePoints() throws Exception {
        long own = search(owner, 0, 4);
        long candidate = search(member("제목 검색 후보", null, null), 0, 4);
        String title = "😀".repeat(80);
        jdbc.update("UPDATE companion_search SET title = ? WHERE id = ?", title, candidate);

        String registered = path(own);
        mvc.perform(get(registered).servletPath(registered).header("Authorization", authorization)
                        .param("q", " " + title + " "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.candidates[*].searchId").value(contains((int) candidate)));

        String preview = "/api/companion-candidates";
        mvc.perform(get(preview).servletPath(preview).header("Authorization", authorization)
                        .param("destinationId", Long.toString(seoul)).param("startDate", today.toString())
                        .param("endDate", today.toString()).param("q", title + "😀"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    @Test
    void returnsOnlyTheSelectedRegistrationsPublicProfileInBothCandidateQueries() throws Exception {
        long own = search(owner, 0, 4);
        long traveler = member("여행자", "FEMALE", today.minusYears(28).minusDays(1));
        long selected = search(traveler, 0, 4);
        long duplicate = search(traveler, 0, 1);
        long missingProfile = search(member("프로필 미입력", null, null), 0, 0);
        jdbc.update("UPDATE companion_search SET show_age = TRUE, show_gender = TRUE WHERE id IN (?, ?)",
                duplicate, missingProfile);
        jdbc.update("UPDATE companion_search SET introduction = ? WHERE id = ?", "산책해요", selected);

        for (ResultActions response : List.of(candidates(own, 0, 20), preview(0, 4))) {
            response.andExpect(status().isOk())
                    .andExpect(jsonPath("$.candidates[*].searchId").value(contains((int) selected, (int) missingProfile)))
                    .andExpect(jsonPath("$.candidates[0].introduction").value("산책해요"))
                    .andExpect(jsonPath("$.candidates[0].age").value(nullValue()))
                    .andExpect(jsonPath("$.candidates[0].gender").value(nullValue()))
                    .andExpect(jsonPath("$.candidates[1].age").value(nullValue()))
                    .andExpect(jsonPath("$.candidates[1].gender").value(nullValue()));
        }

        jdbc.update("UPDATE companion_search SET show_age = TRUE WHERE id = ?", selected);
        candidates(own, 0, 20).andExpect(status().isOk())
                .andExpect(jsonPath("$.candidates[0].age").value(28))
                .andExpect(jsonPath("$.candidates[0].gender").value(nullValue()));
        jdbc.update("UPDATE companion_search SET show_age = FALSE, show_gender = TRUE WHERE id = ?", selected);
        preview(0, 4).andExpect(status().isOk())
                .andExpect(jsonPath("$.candidates[0].age").value(nullValue()))
                .andExpect(jsonPath("$.candidates[0].gender").value("FEMALE"))
                .andExpect(jsonPath("$.candidates[0].birthDate").doesNotHaveJsonPath());
    }

    @Test
    void reopensAClosedRegistrationAndReturnsItInBothCandidateQueries() throws Exception {
        long own = search(owner, 0, 2);
        User traveler = users.saveNewAccount(User.builder().nickname("재개할 회원").role(Role.USER).build());
        long candidate = search(traveler.getId(), 0, 2);
        String resource = "/api/companion-searches/" + candidate;
        jdbc.update("UPDATE companion_search SET status = 'CLOSED', introduction = ?, show_age = TRUE WHERE id = ?",
                "재개할 여행", candidate);
        candidates(own, 0, 20).andExpect(jsonPath("$.candidates").isEmpty());
        preview(0, 2).andExpect(jsonPath("$.candidates").isEmpty());

        String body = "{\"status\":\"ACTIVE\"}";
        mvc.perform(patch(resource).servletPath(resource).header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNotFound());
        String travelerAuthorization = "Bearer " + tokens.issue(traveler.getId(), traveler.getAuthVersion()).accessToken();
        mvc.perform(patch(resource).servletPath(resource).header("Authorization", travelerAuthorization)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.introduction").value("재개할 여행"))
                .andExpect(jsonPath("$.showAge").value(true));
        for (ResultActions response : List.of(candidates(own, 0, 20), preview(0, 2))) {
            response.andExpect(status().isOk())
                    .andExpect(jsonPath("$.candidates[*].searchId").value(contains((int) candidate)));
        }
    }

    @Test
    void rejectsInvalidPreviewConditions() throws Exception {
        String path = "/api/companion-candidates";
        mvc.perform(get(path).servletPath(path).header("Authorization", authorization)
                        .param("destinationId", Long.toString(seoul)).param("startDate", today.toString()))
                .andExpect(status().isBadRequest());
        mvc.perform(get(path).servletPath(path).header("Authorization", authorization)
                        .param("destinationId", Long.toString(seoul)).param("startDate", today.toString())
                        .param("endDate", today.minusDays(1).toString()))
                .andExpect(status().isBadRequest());
        mvc.perform(get(path).servletPath(path).header("Authorization", authorization)
                        .param("destinationId", Long.toString(seoul)).param("startDate", today.toString())
                        .param("endDate", today.toString()).param("preferredMinAge", "30").param("preferredMaxAge", "29"))
                .andExpect(status().isBadRequest());
        mvc.perform(get(path).servletPath(path).header("Authorization", authorization)
                        .param("destinationId", Long.toString(Long.MAX_VALUE)).param("startDate", today.toString())
                        .param("endDate", today.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("DESTINATION_UNAVAILABLE"));
    }

    private long member(String nickname, String gender, LocalDate birthDate) {
        User user = users.saveNewAccount(User.builder().nickname(nickname).role(Role.USER).build());
        jdbc.update("UPDATE users SET gender = ?, birth_date = ? WHERE id = ?", gender, birthDate, user.getId());
        return user.getId();
    }

    private long search(long memberId, int startOffset, int endOffset) {
        jdbc.update("""
                INSERT INTO companion_search (user_id, destination_id, start_date, end_date, status, created_at, updated_at)
                VALUES (?, ?, ?, ?, 'ACTIVE', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6))
                """, memberId, seoul, today.plusDays(startOffset), today.plusDays(endOffset));
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private void preferences(long searchId, String gender, Integer minAge, Integer maxAge) {
        jdbc.update("""
                UPDATE companion_search SET preferred_gender = ?, preferred_min_age = ?, preferred_max_age = ?
                WHERE id = ?
                """, gender, minAge, maxAge, searchId);
    }

    private ResultActions candidates(long searchId, int page, int size) throws Exception {
        String path = path(searchId);
        return mvc.perform(get(path).servletPath(path).header("Authorization", authorization)
                .param("page", Integer.toString(page)).param("size", Integer.toString(size)));
    }

    private ResultActions preview(int startOffset, int endOffset) throws Exception {
        String path = "/api/companion-candidates";
        return mvc.perform(get(path).servletPath(path).header("Authorization", authorization)
                .param("destinationId", Long.toString(seoul))
                .param("startDate", today.plusDays(startOffset).toString())
                .param("endDate", today.plusDays(endOffset).toString()));
    }

    private String path(long searchId) {
        return "/api/companion-searches/" + searchId + "/candidates";
    }
}
