package com.daebangg.sayroam.companion.presentation.api;

import com.daebangg.sayroam.auth.domain.AuthenticatedAccount;
import com.daebangg.sayroam.companion.domain.CompanionRequestStatus;
import com.daebangg.sayroam.companion.presentation.dto.ChangeCompanionSearchStatusRequest;
import com.daebangg.sayroam.companion.presentation.dto.CompanionCandidateListResponse;
import com.daebangg.sayroam.companion.presentation.dto.CompanionMatchingStatusResponse;
import com.daebangg.sayroam.companion.presentation.dto.CompanionRequestResponse;
import com.daebangg.sayroam.companion.presentation.dto.CompanionSearchListResponse;
import com.daebangg.sayroam.companion.presentation.dto.CompanionSearchResponse;
import com.daebangg.sayroam.companion.presentation.dto.FindCompanionCandidatesRequest;
import com.daebangg.sayroam.companion.presentation.dto.PreviewCompanionCandidatesRequest;
import com.daebangg.sayroam.companion.presentation.dto.RespondCompanionRequest;
import com.daebangg.sayroam.companion.presentation.dto.SaveCompanionSearchRequest;
import com.daebangg.sayroam.companion.presentation.dto.SendCompanionRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;

import java.util.List;

@SecurityRequirement(name = "bearerAuth")
public interface CompanionApi {
    @Tag(name = "CompanionSearch")
    @Operation(operationId = "createCompanionSearch", summary = "동행 매칭 등록",
            description = "활성 여행지와 여행 기간, 선택한 상대 조건, 제목, 소개글과 내 정보 공개 설정을 저장합니다. "
                    + "회원당 활성 모집은 최대 3개입니다. 종료한 모집은 개수에서 제외합니다. 모집에 저장할 여행 스타일 4문항 응답이 필요하며 프로필과 별도로 보관합니다.")
    @ApiResponse(responseCode = "201", description = "등록 완료")
    @ApiResponse(responseCode = "409", ref = "#/components/responses/CompanionSearchCreateConflict")
    @ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest")
    @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized")
    ResponseEntity<CompanionSearchResponse> create(@Parameter(hidden = true) AuthenticatedAccount account,
                                                   SaveCompanionSearchRequest request);

    @Tag(name = "CompanionSearch")
    @Operation(operationId = "updateCompanionSearch", summary = "동행 매칭 조건 수정",
            description = "등록의 조건을 교체하며 종료 상태는 유지합니다. 종료한 기존 모집도 여행 스타일을 보완한 뒤 재개할 수 있습니다. 희망 성별과 나이 조건, 제목은 생략하면 해제합니다. "
                    + "소개글과 내 정보 공개 설정, 여행 스타일은 생략하거나 null이면 유지합니다. 기존 모집에 여행 스타일 저장값이 없으면 4문항을 보내야 합니다. "
                    + "동행 확정(CONFIRMED)된 모집은 수정할 수 없습니다.")
    @ApiResponse(responseCode = "200", description = "수정 완료")
    @ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest")
    @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized")
    @ApiResponse(responseCode = "404", ref = "#/components/responses/CompanionSearchNotFound")
    @ApiResponse(responseCode = "409", ref = "#/components/responses/CompanionSearchUpdateConflict")
    ResponseEntity<CompanionSearchResponse> update(@Parameter(hidden = true) AuthenticatedAccount account,
                                                   long searchId, SaveCompanionSearchRequest request);

    @Tag(name = "CompanionSearch")
    @Operation(operationId = "closeCompanionSearch", summary = "동행 모집 종료 또는 재개",
            description = "CLOSED는 모집 종료, ACTIVE는 모집 재개입니다. 이미 같은 상태인 등록에도 요청할 수 있습니다. "
                    + "재개하려면 활성 여행지와 해당 모집에 저장된 여행 스타일 4문항 응답이 필요하며, 활성 모집이 3개 미만이어야 합니다. "
                    + "이미 ACTIVE인 등록에 같은 요청을 반복하면 한도와 무관하게 성공합니다. "
                    + "CONFIRMED는 수락 처리로만 전환되며 직접 지정할 수 없고, 확정된 모집은 종료·재개할 수 없습니다.")
    @ApiResponse(responseCode = "200", description = "상태 변경 완료")
    @ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest")
    @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized")
    @ApiResponse(responseCode = "404", ref = "#/components/responses/CompanionSearchNotFound")
    @ApiResponse(responseCode = "409", ref = "#/components/responses/CompanionSearchStatusConflict")
    ResponseEntity<CompanionSearchResponse> changeStatus(@Parameter(hidden = true) AuthenticatedAccount account,
                                                         long searchId, ChangeCompanionSearchStatusRequest request);

    @Tag(name = "CompanionRequest")
    @ApiResponse(responseCode = "200", description = "처리 결과")
    @ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest")
    @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized")
    @ApiResponse(responseCode = "404", ref = "#/components/responses/CompanionSearchNotFound")
    @ApiResponse(responseCode = "409", ref = "#/components/responses/CompanionCandidateUnavailable")
    @Operation(operationId = "sendCompanionRequest", summary = "두 모집 사이의 롬 메이트 요청 생성 또는 기존 요청 확인")
    ResponseEntity<CompanionRequestResponse> send(@Parameter(hidden = true) AuthenticatedAccount account, long searchId, SendCompanionRequest body);

    @Tag(name = "CompanionRequest")
    @ApiResponse(responseCode = "200", description = "처리 결과")
    @ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest")
    @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized")
    @ApiResponse(responseCode = "403", ref = "#/components/responses/Forbidden")
    @ApiResponse(responseCode = "404", ref = "#/components/responses/CompanionRequestNotFound")
    @ApiResponse(responseCode = "409", ref = "#/components/responses/CompanionRequestAlreadyAnswered")
    @Operation(operationId = "respondToCompanionRequest", summary = "받은 요청 수락 또는 거절",
            description = "수락 시 양쪽 모집을 동행 확정으로 종료하고 두 사람이 참여하는 채팅방을 생성합니다. "
                    + "나머지 대기 요청은 만료됩니다. 이미 만료된 요청에는 EXPIRED를 반환하며, "
                    + "같은 응답을 반복해도 같은 결과와 채팅방을 반환합니다.")
    ResponseEntity<CompanionRequestResponse> respond(@Parameter(hidden = true) AuthenticatedAccount account, long requestId, RespondCompanionRequest body);

    @Tag(name = "CompanionSearch")
    @Operation(operationId = "listMyCompanionSearches", summary = "본인의 동행 매칭 등록 목록",
            description = "종료한 등록을 포함해 최신 등록순으로 조회합니다. travelStyle은 모집 저장값이며 기존 미설정 모집은 빈 객체입니다.")
    @ApiResponse(responseCode = "200", description = "본인 등록 목록")
    @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized")
    ResponseEntity<CompanionSearchListResponse> findMine(@Parameter(hidden = true) AuthenticatedAccount account);

    @Tag(name = "CompanionSearch")
    @Operation(operationId = "getCompanionMatchingStatus", summary = "본인 모집의 후보 탐색 상태 조회",
            description = "현재 모집 조건 버전의 최초 탐색 상태를 조회하며 작업을 새로 실행하지 않습니다. "
                    + "SUCCEEDED는 탐색 완료로 후보가 없을 수도 있습니다. 현재 후보와 공개 프로필은 후보 조회 API로 확인합니다. "
                    + "종료·만료·기존 자유 입력 지역은 SKIPPED, 현재 버전의 작업이 없으면 NOT_REQUESTED입니다.")
    @ApiResponse(responseCode = "200", description = "현재 조건 버전과 탐색 상태")
    @ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest")
    @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized")
    @ApiResponse(responseCode = "404", ref = "#/components/responses/CompanionSearchNotFound")
    ResponseEntity<CompanionMatchingStatusResponse> findMatchingStatus(
            @Parameter(hidden = true) AuthenticatedAccount account, long searchId);

    @Tag(name = "CompanionSearch")
    @Operation(operationId = "listCompanionCandidates", summary = "동행 매칭 후보 조회",
            description = "등록한 두 여행 기간이 하루 이상 겹치고 양쪽의 희망 나이·성별 조건을 충족하는 후보를 조회합니다. "
                    + "여행 기간의 시작일과 종료일을 포함해 겹치는 일수가 많은 순으로, 회원별 한 건을 반환합니다. "
                    + "만 나이와 성별은 해당 등록에서 공개한 값만 반환합니다. STYLE 정렬과 비교는 양쪽 모집에 저장된 4문항 점수를 사용합니다. STYLE 정렬에서는 회원별로 점수가 가장 비슷한 모집 한 건을 선택합니다.")
    @ApiResponse(responseCode = "200", description = "후보 목록. 조건에 맞는 후보가 없으면 빈 배열")
    @ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest")
    @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized")
    @ApiResponse(responseCode = "404", ref = "#/components/responses/CompanionSearchNotFound")
    @ApiResponse(responseCode = "409", ref = "#/components/responses/CompanionCandidateSearchUnavailable")
    ResponseEntity<CompanionCandidateListResponse> findCandidates(
            @Parameter(hidden = true) AuthenticatedAccount account,
            long searchId, @ParameterObject FindCompanionCandidatesRequest request);

    @Tag(name = "CompanionSearch")
    @Operation(operationId = "previewCompanionCandidates", summary = "동행 등록 전 후보 조회",
            description = "여행지와 기간, 선택한 상대 조건으로 동행 후보를 미리 조회합니다. "
                    + "상대의 희망 조건은 로그인 회원의 프로필과 비교합니다. "
                    + "만 나이와 성별은 해당 등록에서 공개한 값만 반환합니다. 전달한 4문항 점수를 상대 모집의 저장값과 비교합니다. 점수를 모두 생략하면 프로필을 초기값으로 사용합니다. STYLE 정렬은 저장값이 완성된 후보를 대상으로 합니다.")
    @ApiResponse(responseCode = "200", description = "후보 목록. 조건에 맞는 후보가 없으면 빈 배열")
    @ApiResponse(responseCode = "409", ref = "#/components/responses/TravelStyleRequired")
    @ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest")
    @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized")
    ResponseEntity<CompanionCandidateListResponse> preview(
            @Parameter(hidden = true) AuthenticatedAccount account,
            @ParameterObject PreviewCompanionCandidatesRequest request);

    @Tag(name = "CompanionRequest")
    @ApiResponse(responseCode = "200", description = "처리 결과")
    @ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest")
    @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized")
    @ApiResponse(responseCode = "404", ref = "#/components/responses/CompanionRequestNotFound")
    @Operation(operationId = "getCompanionRequest", summary = "당사자의 롬 메이트 요청 상세")
    ResponseEntity<CompanionRequestResponse> find(@Parameter(hidden = true) AuthenticatedAccount account, long requestId);

    @Tag(name = "CompanionRequest")
    @ApiResponse(responseCode = "200", description = "처리 결과")
    @ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest")
    @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized")
    @ApiResponse(responseCode = "404", ref = "#/components/responses/CompanionSearchNotFound")
    @Operation(operationId = "listCompanionRequests", summary = "내 모집의 보낸·받은 요청 조회",
            description = "최신 요청 순서로 반환합니다. 거절된 매칭 목록은 status=REJECTED로 조회합니다.")
    ResponseEntity<List<CompanionRequestResponse>> list(@Parameter(hidden = true) AuthenticatedAccount account, long searchId, CompanionRequestStatus status);
}
