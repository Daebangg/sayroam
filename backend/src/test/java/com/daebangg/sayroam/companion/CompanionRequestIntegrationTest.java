package com.daebangg.sayroam.companion;

import com.daebangg.sayroam.auth.application.port.out.AccessTokenIssuer;
import com.daebangg.sayroam.companion.application.dto.CompanionRequestResult;
import com.daebangg.sayroam.companion.application.dto.SaveCompanionSearchCommand;
import com.daebangg.sayroam.companion.application.service.CompanionRequestService;
import com.daebangg.sayroam.companion.application.service.CompanionSearchService;
import com.daebangg.sayroam.companion.domain.CompanionRequest;
import com.daebangg.sayroam.companion.domain.CompanionRequestStatus;
import com.daebangg.sayroam.companion.domain.CompanionSearchStatus;
import com.daebangg.sayroam.support.DatabaseIntegrationTest;
import com.daebangg.sayroam.user.domain.Role;
import com.daebangg.sayroam.user.domain.User;
import com.daebangg.sayroam.user.domain.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
class CompanionRequestIntegrationTest extends DatabaseIntegrationTest {
    @Autowired CompanionRequestService requests;
    @Autowired CompanionSearchService searches;
    @Autowired UserRepository users;
    @Autowired AccessTokenIssuer tokens;
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    private User sender;
    private User receiver;
    private User outsider;
    private long source;
    private long target;
    private long destination;
    private final LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));

    @BeforeEach
    void setUp() {
        sender = member("보낸 회원");
        receiver = member("받은 회원");
        outsider = member("다른 회원");
        destination = jdbc.queryForObject("SELECT id FROM destination WHERE name = '서울'", Long.class);
        source = searches.create(sender.getId(), conditions(today)).id();
        target = searches.create(receiver.getId(), conditions(today)).id();
    }

    private User member(String name) {
        return users.saveNewAccount(User.builder().nickname(name).role(Role.USER).build());
    }

    private SaveCompanionSearchCommand conditions(LocalDate start) {
        return new SaveCompanionSearchCommand(destination, start, start.plusDays(2), null, null, null,
                "동행 모집", null, false, false,
                Map.of("planning", 3, "daily-pace", 3, "time-together", 3, "spending", 3));
    }

    @AfterEach
    void cleanUp() {
        List<Long> roomIds = jdbc.queryForList("""
                SELECT room_id FROM companion_request
                WHERE sender_id IN (?, ?) AND room_id IS NOT NULL
                """, Long.class, sender.getId(), receiver.getId());
        jdbc.update("DELETE FROM companion_request WHERE sender_id IN (?, ?)", sender.getId(), receiver.getId());
        for (Long roomId : roomIds) {
            jdbc.update("DELETE FROM chat_message WHERE room_id = ?", roomId);
            jdbc.update("DELETE FROM room_member WHERE room_id = ?", roomId);
            jdbc.update("DELETE FROM room WHERE id = ?", roomId);
        }
        for (User member : List.of(sender, receiver, outsider)) {
            jdbc.update("DELETE FROM notification WHERE receiver_id = ?", member.getId());
            jdbc.update("DELETE FROM companion_search WHERE user_id = ?", member.getId());
            jdbc.update("DELETE FROM users WHERE id = ?", member.getId());
        }
    }

    @ParameterizedTest
    @EnumSource(CompanionRequest.Decision.class)
    void bothMembersSeeOneDecisionAndOneNotification(CompanionRequest.Decision decision) throws Exception {
        String collection = "/api/companion-searches/" + source + "/requests";
        String body = mvc.perform(post(collection).servletPath(collection).header("Authorization", bearer(sender))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"candidateSearchId\":" + target + "}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.direction").value("SENT"))
                .andReturn().getResponse().getContentAsString();
        long id = json.readTree(body).path("requestId").asLong();
        String detail = "/api/companion-requests/" + id;
        String reply = "{\"status\":\"" + decision.name() + "\"}";
        String candidates = "/api/companion-searches/" + target + "/candidates";
        mvc.perform(get(candidates).servletPath(candidates).param("q", "동행 모집").header("Authorization", bearer(receiver)))
                .andExpect(jsonPath("$.candidates[?(@.requestId == " + id + ")].direction")
                        .value(org.hamcrest.Matchers.contains("RECEIVED")));
        mvc.perform(get(detail).servletPath(detail).header("Authorization", bearer(outsider))).andExpect(status().isNotFound());
        mvc.perform(patch(detail).servletPath(detail).header("Authorization", bearer(sender))
                .contentType(MediaType.APPLICATION_JSON).content(reply)).andExpect(status().isForbidden());
        for (int attempt = 0; attempt < 2; attempt++) {
            mvc.perform(patch(detail).servletPath(detail).header("Authorization", bearer(receiver))
                    .contentType(MediaType.APPLICATION_JSON).content(reply))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.status").value(decision.name()));
        }
        String opposite = decision == CompanionRequest.Decision.ACCEPTED ? "REJECTED" : "ACCEPTED";
        mvc.perform(patch(detail).servletPath(detail).header("Authorization", bearer(receiver))
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"" + opposite + "\"}"))
                .andExpect(status().isConflict());
        mvc.perform(get(detail).servletPath(detail).header("Authorization", bearer(sender)))
                .andExpect(jsonPath("$.status").value(decision.name()))
                .andExpect(jsonPath("$.candidate.nickname").value(receiver.getNickname()))
                .andExpect(jsonPath("$.candidate.age").isEmpty());
        for (User viewer : List.of(sender, receiver)) {
            String path = "/api/companion-searches/" + (viewer == sender ? source : target) + "/requests";
            mvc.perform(get(path).servletPath(path).header("Authorization", bearer(viewer)).param("status", decision.name()))
                    .andExpect(jsonPath("$[0].requestId").value(id));
            String inbox = "/api/notifications";
            mvc.perform(get(inbox).servletPath(inbox).header("Authorization", bearer(viewer)))
                    .andExpect(jsonPath("$.notifications.length()").value(1))
                    .andExpect(jsonPath("$.notifications[0].targetId").value(id))
                    .andExpect(jsonPath("$.notifications[0].type").value(
                            viewer == sender ? "COMPANION_" + decision.name() : "COMPANION_REQUEST"));
        }
        if (decision == CompanionRequest.Decision.REJECTED) {
            assertThat(requests.send(receiver.getId(), target, source).status()).isEqualTo(CompanionRequestStatus.REJECTED);
            long alternative = searches.create(sender.getId(), conditions(today)).id();
            mvc.perform(get(candidates).servletPath(candidates).header("Authorization", bearer(receiver)))
                    .andExpect(jsonPath("$.candidates[?(@.searchId == " + source + ")]").isEmpty())
                    .andExpect(jsonPath("$.candidates[?(@.searchId == " + alternative + ")].canRequest")
                            .value(org.hamcrest.Matchers.contains(true)));
        }
    }

    private String bearer(User member) {
        return "Bearer " + tokens.issue(member.getId(), member.getAuthVersion()).accessToken();
    }

    @Test
    void oppositeConcurrentRequestsKeepTheOriginalDirection() throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService pool = Executors.newFixedThreadPool(2)) {
            Future<CompanionRequestResult> one = pool.submit(() -> {
                start.await();
                return requests.send(sender.getId(), source, target);
            });
            Future<CompanionRequestResult> two = pool.submit(() -> {
                start.await();
                return requests.send(receiver.getId(), target, source);
            });
            start.countDown();
            CompanionRequestResult first = one.get(15, TimeUnit.SECONDS);
            CompanionRequestResult second = two.get(15, TimeUnit.SECONDS);
            assertThat(first.requestId()).isEqualTo(second.requestId());
            assertThat(first.direction()).isNotEqualTo(second.direction());
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM notification WHERE target_type = 'COMPANION_REQUEST' AND target_id = ?",
                    Long.class, first.requestId())).isOne();
        }
    }

    @Test
    void elapsedOverlapExpiresAtReadTimeWithoutARecruitmentEdit() {
        long id = requests.send(sender.getId(), source, target).requestId();
        jdbc.update("UPDATE companion_search SET start_date = ?, end_date = ? WHERE id IN (?, ?)",
                today.minusDays(4), today.minusDays(1), source, target);
        assertThat(requests.list(receiver.getId(), target, CompanionRequestStatus.EXPIRED))
                .extracting(CompanionRequestResult::requestId).containsExactly(id);
        assertThat(requests.respond(receiver.getId(), id, CompanionRequest.Decision.ACCEPTED).status())
                .isEqualTo(CompanionRequestStatus.EXPIRED);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM notification WHERE target_type = 'COMPANION_REQUEST' AND target_id = ?",
                Long.class, id)).isOne();
    }

    @Test
    void invalidatedRequestsStayExpiredAfterConditionsAreRestored() {
        long id = requests.send(sender.getId(), source, target).requestId();
        searches.update(receiver.getId(), target, conditions(today.plusMonths(1)));
        searches.update(receiver.getId(), target, conditions(today));
        assertThat(requests.respond(receiver.getId(), id, CompanionRequest.Decision.ACCEPTED).status())
                .isEqualTo(CompanionRequestStatus.EXPIRED);

        long another = searches.create(sender.getId(), conditions(today)).id();
        long closed = requests.send(sender.getId(), another, target).requestId();
        searches.changeStatus(receiver.getId(), target, CompanionSearchStatus.CLOSED);
        searches.changeStatus(receiver.getId(), target, CompanionSearchStatus.ACTIVE);
        assertThat(requests.find(sender.getId(), closed).status()).isEqualTo(CompanionRequestStatus.EXPIRED);
    }
}
