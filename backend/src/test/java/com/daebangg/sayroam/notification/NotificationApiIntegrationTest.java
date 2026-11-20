package com.daebangg.sayroam.notification;

import com.daebangg.sayroam.auth.application.port.out.AccessTokenIssuer;
import com.daebangg.sayroam.notification.domain.Notification;
import com.daebangg.sayroam.notification.domain.NotificationTargetType;
import com.daebangg.sayroam.notification.domain.NotificationType;
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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Transactional
class NotificationApiIntegrationTest extends DatabaseIntegrationTest {
    private static final String PATH = "/api/notifications";

    @Autowired MockMvc mvc;
    @Autowired EntityManager entityManager;
    @Autowired UserRepository users;
    @Autowired AccessTokenIssuer tokens;
    @Autowired JdbcTemplate jdbc;
    @Autowired JsonMapper json;
    @LocalServerPort int port;

    private User owner;
    private String authorization;

    @BeforeEach
    void createMember() {
        owner = createUser("알림 수신자");
        authorization = authorization(owner);
    }

    @Test
    void 인증되지_않은_조회와_상태_변경과_삭제를_거부한다() throws Exception {
        mvc.perform(get(PATH).servletPath(PATH)).andExpect(status().isUnauthorized());
        mvc.perform(get(PATH + "/summary").servletPath(PATH + "/summary")).andExpect(status().isUnauthorized());
        mvc.perform(patch(PATH).servletPath(PATH).contentType(MediaType.APPLICATION_JSON)
                .content("{\"isRead\":true}")).andExpect(status().isUnauthorized());
        String resource = PATH + "/1";
        mvc.perform(patch(resource).servletPath(resource).contentType(MediaType.APPLICATION_JSON)
                .content("{\"isRead\":true}")).andExpect(status().isUnauthorized());
        mvc.perform(delete(resource).servletPath(resource)).andExpect(status().isUnauthorized());
    }

    @Test
    void 본인의_삭제되지_않은_알림만_최신순으로_조회하고_커서로_이어_조회한다() throws Exception {
        Notification first = saveNotification(owner.getId(), "trip-invite:1");
        Notification second = saveNotification(owner.getId(), "trip-invite:2");
        Notification deleted = saveNotification(owner.getId(), "trip-invite:3");
        deleted.delete(LocalDateTime.now());
        User other = createUser("다른 회원");
        saveNotification(other.getId(), "trip-invite:4");
        flushAndClear();

        mvc.perform(get(PATH).servletPath(PATH).header("Authorization", authorization)
                        .param("size", "1").param("receiverId", other.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notifications.length()").value(1))
                .andExpect(jsonPath("$.notifications[0].id").value(second.getId()))
                .andExpect(jsonPath("$.notifications[0].type").value("TRIP_INVITE"))
                .andExpect(jsonPath("$.notifications[0].content").value("여행에 초대되었습니다."))
                .andExpect(jsonPath("$.notifications[0].targetType").value("TRIP"))
                .andExpect(jsonPath("$.notifications[0].targetId").value(42))
                .andExpect(jsonPath("$.notifications[0].isRead").value(false))
                .andExpect(jsonPath("$.notifications[0].createdAt").isString())
                .andExpect(jsonPath("$.notifications[0].eventKey").doesNotExist())
                .andExpect(jsonPath("$.notifications[0].receiverId").doesNotExist());

        mvc.perform(get(PATH).servletPath(PATH).header("Authorization", authorization)
                        .param("beforeId", second.getId().toString()).param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notifications.length()").value(1))
                .andExpect(jsonPath("$.notifications[0].id").value(first.getId()));
        assertThat(jdbc.queryForObject("SELECT read_at FROM notification WHERE id = ?",
                LocalDateTime.class, second.getId())).isNull();
    }

    @Test
    void 미읽음_필터는_소유권과_삭제를_적용하고_커서로_이어_조회한다() throws Exception {
        Notification first = saveNotification(owner.getId(), "filter:first");
        Notification read = saveNotification(owner.getId(), "filter:read");
        read.read(LocalDateTime.now());
        Notification last = saveNotification(owner.getId(), "filter:last");
        Notification deleted = saveNotification(owner.getId(), "filter:deleted");
        deleted.delete(LocalDateTime.now());
        saveNotification(createUser("다른 회원").getId(), "filter:other");
        flushAndClear();

        mvc.perform(get(PATH).servletPath(PATH).header("Authorization", authorization)
                        .param("unreadOnly", "true").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notifications.length()").value(1))
                .andExpect(jsonPath("$.notifications[0].id").value(last.getId()))
                .andExpect(jsonPath("$.notifications[0].isRead").value(false));
        mvc.perform(get(PATH).servletPath(PATH).header("Authorization", authorization)
                        .param("unreadOnly", "true").param("beforeId", last.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notifications.length()").value(1))
                .andExpect(jsonPath("$.notifications[0].id").value(first.getId()));
        mvc.perform(get(PATH).servletPath(PATH).header("Authorization", authorization).param("unreadOnly", "false"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.notifications.length()").value(3));
    }

    @Test
    void 전체_읽음은_본인_미삭제_미읽음만_변경하고_반복해도_읽음_시각을_유지한다() throws Exception {
        Notification unread = saveNotification(owner.getId(), "read-all:unread");
        for (int index = 0; index < 20; index++) {
            saveNotification(owner.getId(), "read-all:page:" + index);
        }
        Notification read = saveNotification(owner.getId(), "read-all:read");
        LocalDateTime originalReadAt = LocalDateTime.of(2026, 11, 1, 9, 0);
        read.read(originalReadAt);
        Notification deleted = saveNotification(owner.getId(), "read-all:deleted");
        deleted.delete(originalReadAt);
        Notification other = saveNotification(createUser("다른 회원").getId(), "read-all:other");
        flushAndClear();

        updateReadStatus(PATH, authorization, "{\"isRead\":true}").andExpect(status().isNoContent());
        LocalDateTime firstReadAt = jdbc.queryForObject("SELECT read_at FROM notification WHERE id = ?",
                LocalDateTime.class, unread.getId());
        assertThat(firstReadAt).isNotNull();
        assertThat(jdbc.queryForObject("SELECT updated_at FROM notification WHERE id = ?",
                LocalDateTime.class, unread.getId())).isEqualTo(firstReadAt);
        updateReadStatus(PATH, authorization, "{\"isRead\":true}").andExpect(status().isNoContent());
        assertThat(jdbc.queryForObject("SELECT read_at FROM notification WHERE id = ?", LocalDateTime.class,
                unread.getId())).isEqualTo(firstReadAt);
        assertThat(jdbc.queryForObject("SELECT read_at FROM notification WHERE id = ?", LocalDateTime.class,
                read.getId())).isEqualTo(originalReadAt);
        for (Long id : List.of(deleted.getId(), other.getId())) {
            assertThat(jdbc.queryForObject("SELECT read_at FROM notification WHERE id = ?",
                    LocalDateTime.class, id)).isNull();
        }
        mvc.perform(get(PATH + "/summary").servletPath(PATH + "/summary").header("Authorization", authorization))
                .andExpect(status().isOk()).andExpect(jsonPath("$.unreadCount").value(0));
        mvc.perform(get(PATH).servletPath(PATH).header("Authorization", authorization).param("unreadOnly", "true"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.notifications").isEmpty());

        saveNotification(owner.getId(), "read-all:later");
        flushAndClear();
        mvc.perform(get(PATH).servletPath(PATH).header("Authorization", authorization).param("unreadOnly", "true"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.notifications.length()").value(1));
    }

    @Test
    void 알림이_없는_회원도_전체_읽음을_반복할_수_있다() throws Exception {
        updateReadStatus(PATH, authorization, "{\"isRead\":true}").andExpect(status().isNoContent());
        updateReadStatus(PATH, authorization, "{\"isRead\":true}").andExpect(status().isNoContent());
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"isRead\":null}", "{\"isRead\":false}", "{\"isRead\":{}}"})
    void 전체_읽음은_true가_아닌_입력을_거부한다(String body) throws Exception {
        Notification unread = saveNotification(owner.getId(), "read-all:invalid");
        flushAndClear();
        updateReadStatus(PATH, authorization, body).andExpect(status().isBadRequest());
        assertThat(jdbc.queryForObject("SELECT read_at FROM notification WHERE id = ?",
                LocalDateTime.class, unread.getId())).isNull();
    }

    @Test
    void 기본_조회_개수는_20개다() throws Exception {
        for (int index = 0; index < 21; index++) {
            saveNotification(owner.getId(), "trip-invite:" + index);
        }
        flushAndClear();

        mvc.perform(get(PATH).servletPath(PATH).header("Authorization", authorization))
                .andExpect(status().isOk()).andExpect(jsonPath("$.notifications.length()").value(20));
        mvc.perform(get(PATH + "/summary").servletPath(PATH + "/summary").header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(21))
                .andExpect(jsonPath("$.unreadCount").value(21));
    }

    @Test
    void 알림이_없으면_요약_개수는_모두_0이다() throws Exception {
        mvc.perform(get(PATH + "/summary").servletPath(PATH + "/summary").header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(0))
                .andExpect(jsonPath("$.unreadCount").value(0));
    }

    @Test
    void 요약은_본인_미삭제_알림만_집계하고_읽음과_삭제_변경을_반영한다() throws Exception {
        Notification read = saveNotification(owner.getId(), "trip-invite:read");
        read.read(LocalDateTime.now());
        Notification unread = saveNotification(owner.getId(), "trip-invite:unread");
        Notification deleted = saveNotification(owner.getId(), "trip-invite:deleted");
        deleted.delete(LocalDateTime.now());
        User other = createUser("다른 회원");
        saveNotification(other.getId(), "trip-invite:other");
        flushAndClear();
        String summary = PATH + "/summary";
        String resource = PATH + "/" + unread.getId();

        mvc.perform(get(summary).servletPath(summary).header("Authorization", authorization)
                        .param("receiverId", other.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(2))
                .andExpect(jsonPath("$.unreadCount").value(1));
        assertThat(jdbc.queryForObject("SELECT read_at FROM notification WHERE id = ?",
                LocalDateTime.class, unread.getId())).isNull();

        updateReadStatus(resource, authorization, "{\"isRead\":true}").andExpect(status().isNoContent());
        flushAndClear();
        mvc.perform(get(summary).servletPath(summary).header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(2))
                .andExpect(jsonPath("$.unreadCount").value(0));

        updateReadStatus(resource, authorization, "{\"isRead\":false}").andExpect(status().isNoContent());
        flushAndClear();
        mvc.perform(get(summary).servletPath(summary).header("Authorization", authorization))
                .andExpect(status().isOk()).andExpect(jsonPath("$.unreadCount").value(1));

        mvc.perform(delete(resource).servletPath(resource).header("Authorization", authorization))
                .andExpect(status().isNoContent());
        flushAndClear();
        mvc.perform(get(summary).servletPath(summary).header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(1))
                .andExpect(jsonPath("$.unreadCount").value(0));
    }

    @Test
    void 연결_대상이_없으면_null을_반환하고_알림이_없으면_빈_배열을_반환한다() throws Exception {
        mvc.perform(get(PATH).servletPath(PATH).header("Authorization", authorization))
                .andExpect(status().isOk()).andExpect(jsonPath("$.notifications").isEmpty());
        Notification notification = Notification.create(owner.getId(), NotificationType.TRIP_INVITE,
                null, null, "trip-invite:no-target", "안내 알림입니다.");
        entityManager.persist(notification);
        flushAndClear();

        mvc.perform(get(PATH).servletPath(PATH).header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notifications[0].targetType").value(nullValue()))
                .andExpect(jsonPath("$.notifications[0].targetId").value(nullValue()));
    }

    @Test
    void 읽음과_읽지_않음으로_변경하고_반복_읽음은_최초_시각을_유지한다() throws Exception {
        Notification notification = saveNotification(owner.getId(), "trip-invite:read");
        flushAndClear();
        String resource = PATH + "/" + notification.getId();

        updateReadStatus(resource, authorization, "{\"isRead\":true}").andExpect(status().isNoContent());
        flushAndClear();
        LocalDateTime readAt = jdbc.queryForObject("SELECT read_at FROM notification WHERE id = ?",
                LocalDateTime.class, notification.getId());
        assertThat(readAt).isNotNull();
        updateReadStatus(resource, authorization, "{\"isRead\":true}").andExpect(status().isNoContent());
        flushAndClear();
        assertThat(jdbc.queryForObject("SELECT read_at FROM notification WHERE id = ?",
                LocalDateTime.class, notification.getId())).isEqualTo(readAt);
        mvc.perform(get(PATH).servletPath(PATH).header("Authorization", authorization))
                .andExpect(status().isOk()).andExpect(jsonPath("$.notifications[0].isRead").value(true));

        updateReadStatus(resource, authorization, "{\"isRead\":false}").andExpect(status().isNoContent());
        flushAndClear();
        assertThat(jdbc.queryForObject("SELECT read_at FROM notification WHERE id = ?",
                LocalDateTime.class, notification.getId())).isNull();
    }

    @Test
    void 삭제한_알림은_목록에서_제외하고_반복_삭제는_최초_시각을_유지한다() throws Exception {
        Notification notification = saveNotification(owner.getId(), "trip-invite:delete");
        flushAndClear();
        String resource = PATH + "/" + notification.getId();

        mvc.perform(delete(resource).servletPath(resource).header("Authorization", authorization))
                .andExpect(status().isNoContent());
        flushAndClear();
        LocalDateTime deletedAt = jdbc.queryForObject("SELECT deleted_at FROM notification WHERE id = ?",
                LocalDateTime.class, notification.getId());
        assertThat(deletedAt).isNotNull();
        mvc.perform(delete(resource).servletPath(resource).header("Authorization", authorization))
                .andExpect(status().isNoContent());
        flushAndClear();
        assertThat(jdbc.queryForObject("SELECT deleted_at FROM notification WHERE id = ?",
                LocalDateTime.class, notification.getId())).isEqualTo(deletedAt);
        mvc.perform(get(PATH).servletPath(PATH).header("Authorization", authorization))
                .andExpect(status().isOk()).andExpect(jsonPath("$.notifications").isEmpty());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void 삭제한_알림의_읽음_상태는_변경할_수_없다(boolean isRead) throws Exception {
        Notification notification = saveNotification(owner.getId(), "trip-invite:deleted");
        notification.delete(LocalDateTime.now());
        flushAndClear();

        updateReadStatus(PATH + "/" + notification.getId(), authorization, "{\"isRead\":" + isRead + "}")
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("NOTIFICATION_DELETED"));
    }

    @Test
    void 다른_회원의_알림과_없는_알림은_상태_변경과_삭제에서_동일하게_404를_반환한다() throws Exception {
        Notification notification = saveNotification(owner.getId(), "trip-invite:private");
        String otherAuthorization = authorization(createUser("다른 회원"));
        flushAndClear();
        String resource = PATH + "/" + notification.getId();

        updateReadStatus(resource, otherAuthorization, "{\"isRead\":true}")
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("NOTIFICATION_NOT_FOUND"));
        mvc.perform(delete(resource).servletPath(resource).header("Authorization", otherAuthorization))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("NOTIFICATION_NOT_FOUND"));
        String missing = PATH + "/" + Long.MAX_VALUE;
        updateReadStatus(missing, authorization, "{\"isRead\":false}")
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("NOTIFICATION_NOT_FOUND"));
        mvc.perform(delete(missing).servletPath(missing).header("Authorization", authorization))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("NOTIFICATION_NOT_FOUND"));
        assertThat(jdbc.queryForObject("SELECT read_at FROM notification WHERE id = ?",
                LocalDateTime.class, notification.getId())).isNull();
        assertThat(jdbc.queryForObject("SELECT deleted_at FROM notification WHERE id = ?",
                LocalDateTime.class, notification.getId())).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"isRead\":null}", "{\"isRead\":{}}", "{"})
    void 누락되거나_잘못된_읽음_상태_본문을_거부한다(String body) throws Exception {
        Notification notification = saveNotification(owner.getId(), "trip-invite:invalid-body");
        flushAndClear();

        updateReadStatus(PATH + "/" + notification.getId(), authorization, body)
                .andExpect(status().isBadRequest());
        assertThat(jdbc.queryForObject("SELECT read_at FROM notification WHERE id = ?",
                LocalDateTime.class, notification.getId())).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"size=0", "size=101", "size=abc", "beforeId=0", "beforeId=-1", "beforeId=abc"})
    void 잘못된_조회_조건을_거부한다(String condition) throws Exception {
        String[] query = condition.split("=");
        mvc.perform(get(PATH).servletPath(PATH).header("Authorization", authorization).param(query[0], query[1]))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "abc"})
    void 잘못된_알림_ID를_거부한다(String id) throws Exception {
        String resource = PATH + "/" + id;
        updateReadStatus(resource, authorization, "{\"isRead\":true}").andExpect(status().isBadRequest());
        mvc.perform(delete(resource).servletPath(resource).header("Authorization", authorization))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 실행_서버의_OpenAPI에_알림_계약과_오류를_포함하고_명세를_내보낸다() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/v3/api-docs"))
                .timeout(Duration.ofSeconds(30)).GET().build();
        HttpResponse<byte[]> response;
        try (HttpClient client = HttpClient.newHttpClient()) {
            response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
        }
        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode spec = json.readTree(response.body());
        JsonNode paths = spec.path("paths");
        assertThat(paths.path(PATH).path("get").path("operationId").asString()).isEqualTo("listMyNotifications");
        assertThat(paths.path(PATH).has("post")).isFalse();
        assertThat(paths.path(PATH).path("patch").path("operationId").asString()).isEqualTo("readAllMyNotifications");
        assertThat(paths.path(PATH).path("get").path("parameters").toString()).contains("unreadOnly");
        assertThat(paths.path(PATH + "/summary").path("get").path("operationId").asString())
                .isEqualTo("getMyNotificationSummary");
        assertThat(paths.path(PATH + "/{notificationId}").path("patch").path("operationId").asString())
                .isEqualTo("updateNotificationReadStatus");
        assertThat(paths.path(PATH + "/{notificationId}").path("patch").path("parameters")
                .get(0).path("schema").path("type").asString()).isEqualTo("integer");
        assertThat(paths.path(PATH + "/{notificationId}").path("delete").path("responses").has("204")).isTrue();
        assertThat(paths.path(PATH).path("get").path("security").toString()).contains("bearerAuth");
        JsonNode schemas = spec.path("components").path("schemas");
        assertThat(schemas.path("NotificationSummaryResponse").path("properties").path("totalCount")
                .path("type").asString()).isEqualTo("integer");
        assertThat(schemas.path("NotificationSummaryResponse").path("properties").path("unreadCount")
                .path("type").asString()).isEqualTo("integer");
        assertThat(schemas.path("NotificationResponse").path("properties").has("isRead")).isTrue();
        assertThat(schemas.path("UpdateNotificationReadRequest").path("required").toString()).contains("isRead");
        assertThat(schemas.path("ReadAllNotificationsRequest").path("required").toString()).contains("isRead");
        assertThat(schemas.path("ReadAllNotificationsRequest").path("properties").path("isRead")
                .path("type").asString()).isEqualTo("boolean");
        for (JsonNode value : schemas.path("ReadAllNotificationsRequest").path("properties").path("isRead").path("enum")) {
            assertThat(value.isBoolean()).isTrue();
        }
        assertThat(schemas.path("NotificationType").path("enum").toString()).contains("WELCOME");
        assertThat(schemas.path("ApiErrorCode").path("enum").toString())
                .contains("NOTIFICATION_NOT_FOUND", "NOTIFICATION_DELETED");
        Path output = Path.of("build/openapi/openapi.json");
        Files.createDirectories(output.getParent());
        Files.write(output, response.body());
    }

    private User createUser(String nickname) {
        return users.saveNewAccount(User.builder().nickname(nickname).role(Role.USER).build());
    }

    private String authorization(User user) {
        return "Bearer " + tokens.issue(user.getId(), user.getAuthVersion()).accessToken();
    }

    private Notification saveNotification(Long receiverId, String eventKey) {
        Notification notification = Notification.create(receiverId, NotificationType.TRIP_INVITE,
                NotificationTargetType.TRIP, 42L, eventKey, "여행에 초대되었습니다.");
        entityManager.persist(notification);
        return notification;
    }

    private ResultActions updateReadStatus(String resource, String token, String body) throws Exception {
        MockHttpServletRequestBuilder request = patch(resource).servletPath(resource)
                .header("Authorization", token).contentType(MediaType.APPLICATION_JSON).content(body);
        return mvc.perform(request);
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
