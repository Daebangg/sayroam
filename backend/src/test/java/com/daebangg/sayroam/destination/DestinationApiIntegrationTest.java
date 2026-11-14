package com.daebangg.sayroam.destination;

import com.daebangg.sayroam.auth.application.port.out.AccessTokenIssuer;
import com.daebangg.sayroam.support.DatabaseIntegrationTest;
import com.daebangg.sayroam.user.domain.Role;
import com.daebangg.sayroam.user.domain.User;
import com.daebangg.sayroam.user.domain.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@Transactional
class DestinationApiIntegrationTest extends DatabaseIntegrationTest {
    private static final String PATH = "/api/destinations";

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired UserRepository users;
    @Autowired AccessTokenIssuer tokens;

    private String authorization;

    @BeforeEach
    void createMember() {
        User user = users.saveNewAccount(User.builder().nickname("여행자").role(Role.USER).build());
        authorization = "Bearer " + tokens.issue(user.getId(), user.getAuthVersion()).accessToken();
    }

    @Test
    void listsAllDestinationsWithTheAgreedBoundaries() throws Exception {
        mvc.perform(get(PATH).servletPath(PATH)).andExpect(status().isUnauthorized());
        list(null).andExpect(status().isOk())
                .andExpect(jsonPath("$.destinations.length()").value(160));

        list("서울").andExpect(jsonPath("$.destinations[*].name").value(containsInAnyOrder("서울")));
        list("제주").andExpect(jsonPath("$.destinations[*].name").value(containsInAnyOrder("제주")));
        list("종로구").andExpect(jsonPath("$.destinations").isEmpty());
        list("서귀포").andExpect(jsonPath("$.destinations").isEmpty());

        assertThat(jdbc.queryForObject("SELECT COUNT(legal_region_code) FROM destination", Integer.class)).isEqualTo(159);
        assertThat(jdbc.queryForObject("SELECT legal_region_code FROM destination WHERE name = '광주'", String.class))
                .isNull();
        assertThat(jdbc.queryForObject("SELECT legal_region_code FROM destination WHERE name = '세종'", String.class))
                .isEqualTo("3611000000");
        assertThat(jdbc.queryForObject("SELECT legal_region_code FROM destination WHERE name = '여수'", String.class))
                .isEqualTo("1213000000");
    }

    @Test
    void searchesPartialNamesAndDistinguishesRegionsWithTheSameName() throws Exception {
        list("광주").andExpect(jsonPath("$.destinations[*].name").value(containsInAnyOrder("광주", "경기 광주")));
        list("고성").andExpect(jsonPath("$.destinations[*].name").value(containsInAnyOrder("강원 고성", "경남 고성")));
        list("  경기 광주  ").andExpect(jsonPath("$.destinations[*].name").value(containsInAnyOrder("경기 광주")));
        list("강원특별자치도고성군")
                .andExpect(jsonPath("$.destinations[*].name").value(containsInAnyOrder("강원 고성")));
        list("광주광역시").andExpect(jsonPath("$.destinations[*].name").value(containsInAnyOrder("광주")));
        list("여수시").andExpect(jsonPath("$.destinations[0].id").isNumber())
                .andExpect(jsonPath("$.destinations[0].name").value("여수"));
    }

    @Test
    void hidesInactiveDestinationsAndHandlesBlankOrInvalidSearch() throws Exception {
        jdbc.update("UPDATE destination SET is_active = 0 WHERE name = '강원 고성'");

        list("고성").andExpect(jsonPath("$.destinations[*].name").value(containsInAnyOrder("경남 고성")));
        list("  ").andExpect(jsonPath("$.destinations.length()").value(159));
        list("%_").andExpect(jsonPath("$.destinations").isEmpty());
        list("가".repeat(81)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    @Test
    void documentsTheSearchParameterAndBearerSecurity() throws Exception {
        mvc.perform(get("/v3/api-docs").servletPath("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/destinations'].get.operationId").value("listDestinations"))
                .andExpect(jsonPath("$.paths['/api/destinations'].get.parameters[0].name").value("q"))
                .andExpect(jsonPath("$.paths['/api/destinations'].get.parameters[0].schema.maxLength").value(80))
                .andExpect(jsonPath("$.paths['/api/destinations'].get.security[0].bearerAuth").isArray());
    }

    private ResultActions list(String query) throws Exception {
        MockHttpServletRequestBuilder request = get(PATH).servletPath(PATH).header("Authorization", authorization);
        if (query != null) {
            request.param("q", query);
        }
        return mvc.perform(request);
    }
}
