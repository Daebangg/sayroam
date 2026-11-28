package com.daebangg.sayroam.companion.domain;

import com.daebangg.sayroam.common.domain.TravelPeriod;
import com.daebangg.sayroam.global.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CompanionMatchingJobTest {
    @Test
    void rejectsInvalidClaimTokensBeforeChangingTheJob() {
        LocalDateTime now = LocalDateTime.of(2030, 4, 1, 0, 0);
        CompanionSearch search = CompanionSearch.builder().userId(1L).destinationId(1L)
                .period(new TravelPeriod(LocalDate.of(2030, 4, 1), LocalDate.of(2030, 4, 3))).build();
        ReflectionTestUtils.setField(search, "id", 1L);
        CompanionMatchingJob job = new CompanionMatchingJob(search, now);

        assertThatThrownBy(() -> job.claim("a".repeat(CompanionMatchingJob.MAX_CLAIM_TOKEN_LENGTH + 1), now))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> job.claim(" ", now)).isInstanceOf(BusinessException.class);
        assertThat(job.getStatus()).isEqualTo(CompanionMatchingJobStatus.PENDING);

        String token = "a".repeat(CompanionMatchingJob.MAX_CLAIM_TOKEN_LENGTH);
        job.claim(token, now);
        assertThat(job.isClaimedBy(token)).isTrue();
    }
}
