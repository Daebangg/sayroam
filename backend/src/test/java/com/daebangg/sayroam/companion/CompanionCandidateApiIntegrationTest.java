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

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
        style(owner, 2, 4, 2, 5);
        authorization = "Bearer " + tokens.issue(owner, user.getAuthVersion()).accessToken();
        seoul = jdbc.queryForObject("SELECT id FROM destination WHERE name = '서울'", Long.class);
    }

    @Test
    void sortsRegisteredPeriodOverlapsAndDeduplicatesBeforePaging() throws Exception {
        long own = search(owner, -4, 4);
        long traveler = member("여행자", null, null);
        search(traveler, 4, 6);
        long first = search(traveler, -1, 2);
        jdbc.update("UPDATE companion_search SET title = ?, updated_at = ? WHERE id = ?",
                "카페 투어", "2026-11-24 15:30:00", first);
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
                .andExpect(jsonPath("$.candidates[0].updatedAt").value("2026-11-24T15:30:00Z"))
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
    void usesTheFirstTwentyOverlapsWhenPagingParametersAreOmittedOrEmpty() throws Exception {
        long own = search(owner, 0, 3);
        for (int index = 0; index < 21; index++) {
            search(member("기본값 후보 " + index, null, null), 0, 3);
        }

        for (String path : List.of(path(own), "/api/companion-candidates")) {
            mvc.perform(get(path).servletPath(path).header("Authorization", authorization)
                            .param("destinationId", Long.toString(seoul)).param("startDate", today.toString())
                            .param("endDate", today.plusDays(3).toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.candidates.length()").value(20))
                    .andExpect(jsonPath("$.hasNext").value(true));
            mvc.perform(get(path).servletPath(path).header("Authorization", authorization)
                            .param("destinationId", Long.toString(seoul)).param("startDate", today.toString())
                            .param("endDate", today.plusDays(3).toString())
                            .param("page", "").param("size", "").param("sort", ""))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.candidates.length()").value(20))
                    .andExpect(jsonPath("$.hasNext").value(true));
        }
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
        style(traveler.getId(), 2, 4, 2, 5);
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
    void ranksAllStyleCandidatesBeforePagingAndExplainsEachQuestion() throws Exception {
        long own = search(owner, 0, 6);
        long nearUser = member("비슷한 여행자", null, null);
        style(nearUser, 2, 5, 2, 4);
        long near = search(nearUser, 0, 1);
        search(nearUser, 0, 0);
        long discussUser = member("협의할 여행자", null, null);
        style(discussUser, 2, 5, 4, 2);
        long discuss = search(discussUser, 0, 6);
        search(member("응답 없는 여행자", null, null), 0, 6);

        String path = path(own);
        mvc.perform(get(path).servletPath(path).header("Authorization", authorization)
                        .param("sort", "STYLE").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.candidates[0].searchId").value(near))
                .andExpect(jsonPath("$.candidates[0].travelStyleMatch.score").value(87.5))
                .andExpect(jsonPath("$.candidates[0].travelStyleMatch.category").value("SIMILAR"))
                .andExpect(jsonPath("$.candidates[0].travelStyleMatch.items[0].code").value("planning"))
                .andExpect(jsonPath("$.candidates[0].travelStyleMatch.items[0].myScore").value(2))
                .andExpect(jsonPath("$.candidates[0].travelStyleMatch.items[1].candidateScore").value(5))
                .andExpect(jsonPath("$.candidates[0].travelStyleMatch.items[1].similarity").value("SIMILAR"))
                .andExpect(jsonPath("$.hasNext").value(true));
        mvc.perform(get(path).servletPath(path).header("Authorization", authorization)
                        .param("sort", "STYLE").param("size", "1").param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.candidates[0].searchId").value(discuss))
                .andExpect(jsonPath("$.candidates[0].travelStyleMatch.score").value(62.5))
                .andExpect(jsonPath("$.candidates[0].travelStyleMatch.category").value("NEEDS_DISCUSSION"))
                .andExpect(jsonPath("$.candidates[0].travelStyleMatch.items[2].similarity").value("SOMEWHAT_DIFFERENT"))
                .andExpect(jsonPath("$.candidates[0].travelStyleMatch.items[3].similarity").value("DIFFERENT"))
                .andExpect(jsonPath("$.candidates[0].travelStyleMatch.items[3].difference").value(3))
                .andExpect(jsonPath("$.hasNext").value(false));

        String preview = "/api/companion-candidates";
        mvc.perform(get(preview).servletPath(preview).header("Authorization", authorization)
                        .param("destinationId", Long.toString(seoul)).param("startDate", today.toString())
                        .param("endDate", today.plusDays(6).toString()).param("sort", "STYLE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.candidates[*].searchId").value(contains((int) near, (int) discuss)));
        // 기존 날짜순 조회에서는 부분 응답 모집도 계속 조회할 수 있다.
        candidates(own, 0, 20).andExpect(status().isOk())
                .andExpect(jsonPath("$.candidates.length()").value(3))
                .andExpect(jsonPath("$.candidates[0].searchId").value(discuss));
    }

    @Test
    void requiresAllFourAnswersForStyleMatchingAndNewRegistrations() throws Exception {
        jdbc.update("DELETE FROM user_travel_style WHERE user_id = ?", owner);
        String preview = "/api/companion-candidates";
        mvc.perform(get(preview).servletPath(preview).header("Authorization", authorization)
                        .param("destinationId", Long.toString(seoul)).param("startDate", today.toString())
                        .param("endDate", today.toString()).param("sort", "STYLE"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("TRAVEL_STYLE_REQUIRED"));
        String registration = "/api/companion-searches";
        mvc.perform(post(registration).servletPath(registration).header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"destinationId": %d, "startDate": "%s", "endDate": "%s"}
                                """.formatted(seoul, today, today)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("TRAVEL_STYLE_REQUIRED"));
    }

    @Test
    void comparesEachRecruitmentSnapshotAndAcceptsDraftScoresWithoutAProfile() throws Exception {
        long own = search(owner, 0, 6);
        long traveler = member("여행별 다른 스타일", null, null);
        style(traveler, 2, 4, 2, 5);
        long different = search(traveler, 0, 6);
        long similar = search(traveler, 0, 1);
        jdbc.update("UPDATE companion_search SET style_planning = 5, style_daily_pace = 1, style_time_together = 5, style_spending = 1 WHERE id = ?", different);
        jdbc.update("UPDATE user_travel_style SET score = 1 WHERE user_id = ?", traveler);
        jdbc.update("DELETE FROM user_travel_style WHERE user_id = ?", owner);

        String path = path(own);
        mvc.perform(get(path).servletPath(path).header("Authorization", authorization).param("sort", "STYLE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.candidates[*].searchId").value(contains((int) similar)))
                .andExpect(jsonPath("$.candidates[0].travelStyleMatch.score").value(100.0));
        // 겹치는 기간순에서는 같은 회원의 다른 모집을 선택하고 그 모집의 스타일을 비교한다.
        candidates(own, 0, 20).andExpect(status().isOk())
                .andExpect(jsonPath("$.candidates[0].searchId").value(different))
                .andExpect(jsonPath("$.candidates[0].travelStyleMatch.score").value(18.75));

        String preview = "/api/companion-candidates";
        mvc.perform(get(preview).servletPath(preview).header("Authorization", authorization)
                        .param("destinationId", Long.toString(seoul)).param("startDate", today.toString())
                        .param("endDate", today.plusDays(6).toString()).param("sort", "STYLE")
                        .param("planning", "2").param("dailyPace", "4").param("timeTogether", "2").param("spending", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.candidates[0].searchId").value(similar))
                .andExpect(jsonPath("$.candidates[0].travelStyleMatch.score").value(100.0));
        mvc.perform(get(preview).servletPath(preview).header("Authorization", authorization)
                        .param("destinationId", Long.toString(seoul)).param("startDate", today.toString())
                        .param("endDate", today.toString()).param("planning", "2"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("TRAVEL_STYLE_REQUIRED"));
    }

    @Test
    void rejectsInvalidPreviewConditions() throws Exception {
        String path = "/api/companion-candidates";
        for (String missing : List.of("destinationId", "startDate", "endDate")) {
            MockHttpServletRequestBuilder request = get(path).servletPath(path).header("Authorization", authorization);
            if (!missing.equals("destinationId")) request.param("destinationId", Long.toString(seoul));
            if (!missing.equals("startDate")) request.param("startDate", today.toString());
            if (!missing.equals("endDate")) request.param("endDate", today.toString());
            mvc.perform(request).andExpect(status().isBadRequest());
        }
        mvc.perform(get(path).servletPath(path).header("Authorization", authorization)
                        .param("destinationId", Long.toString(seoul)).param("startDate", today.toString()))
                .andExpect(status().isBadRequest());
        mvc.perform(get(path).servletPath(path).header("Authorization", authorization)
                        .param("destinationId", Long.toString(seoul)).param("startDate", "invalid-date")
                        .param("endDate", today.toString()))
                .andExpect(status().isBadRequest());
        mvc.perform(get(path).servletPath(path).header("Authorization", authorization)
                        .param("destinationId", Long.toString(seoul)).param("startDate", today.toString())
                        .param("endDate", today.toString()).param("sort", "UNKNOWN"))
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

    private void style(long userId, int planning, int pace, int together, int spending) {
        String[] codes = {"planning", "daily-pace", "time-together", "spending"};
        int[] scores = {planning, pace, together, spending};
        for (int index = 0; index < codes.length; index++) {
            jdbc.update("""
                    INSERT INTO user_travel_style (user_id, item_id, score, created_at, updated_at)
                    SELECT ?, id, ?, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)
                    FROM travel_style_item WHERE code = ?
                    """, userId, scores[index], codes[index]);
        }
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
        long id = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        snapshotStyle(id, memberId);
        return id;
    }

    private void snapshotStyle(long searchId, long memberId) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM user_travel_style WHERE user_id = ?", Integer.class, memberId);
        if (count != 4) return;
        jdbc.update("""
                UPDATE companion_search SET
                    style_planning = (SELECT s.score FROM user_travel_style s JOIN travel_style_item i ON i.id = s.item_id WHERE s.user_id = ? AND i.code = 'planning'),
                    style_daily_pace = (SELECT s.score FROM user_travel_style s JOIN travel_style_item i ON i.id = s.item_id WHERE s.user_id = ? AND i.code = 'daily-pace'),
                    style_time_together = (SELECT s.score FROM user_travel_style s JOIN travel_style_item i ON i.id = s.item_id WHERE s.user_id = ? AND i.code = 'time-together'),
                    style_spending = (SELECT s.score FROM user_travel_style s JOIN travel_style_item i ON i.id = s.item_id WHERE s.user_id = ? AND i.code = 'spending')
                WHERE id = ?
                """, memberId, memberId, memberId, memberId, searchId);
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
