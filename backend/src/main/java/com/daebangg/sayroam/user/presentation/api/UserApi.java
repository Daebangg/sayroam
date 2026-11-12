package com.daebangg.sayroam.user.presentation.api;

import com.daebangg.sayroam.auth.domain.AuthenticatedAccount;
import com.daebangg.sayroam.user.presentation.dto.CurrentUserResponse;
import com.daebangg.sayroam.user.presentation.dto.UploadProfileImageRequest;
import com.daebangg.sayroam.user.presentation.dto.UserLoginMethodsResponse;
import com.daebangg.sayroam.user.presentation.dto.UpdateUserProfileRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "Users")
public interface UserApi {

    @Operation(summary = "내 로그인 수단 조회", description = "비밀번호 로그인 가능 여부와 실제로 연결된 소셜 공급자만 반환합니다. 공급자 회원 ID·토큰·해시는 노출하지 않습니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "현재 회원의 로그인 수단. Cache-Control: no-store"),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized"),
            @ApiResponse(responseCode = "503", ref = "#/components/responses/AuthenticationUnavailable"),
            @ApiResponse(responseCode = "500", ref = "#/components/responses/InternalServerError")
    })
    ResponseEntity<UserLoginMethodsResponse> loginMethods(
            @Parameter(hidden = true) AuthenticatedAccount account);

    @Operation(summary = "내 회원 정보 조회",
            description = "현재 로그인한 회원의 정보를 조회합니다. 가입 시각은 UTC로 반환하고 성별·생일·출생연도·프로필 이미지가 미설정이면 null을 반환합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "현재 회원 정보. Cache-Control: no-store"),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized"),
            @ApiResponse(responseCode = "503", ref = "#/components/responses/AuthenticationUnavailable"),
            @ApiResponse(responseCode = "500", ref = "#/components/responses/InternalServerError")
    })
    ResponseEntity<CurrentUserResponse> me(
            @Parameter(hidden = true) AuthenticatedAccount account);

    @Operation(summary = "내 프로필 부분 수정",
            description = "닉네임·프로필 이미지·성별·생일·출생연도만 변경할 수 있습니다. 생략한 필드는 유지하고, "
                    + "선택 정보에 null을 전달하면 제거합니다. 닉네임은 null로 변경할 수 없습니다. "
                    + "회원 ID·이메일·권한 등 허용하지 않은 필드를 포함하면 요청을 거절합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "변경된 회원 정보. Cache-Control: no-store"),
            @ApiResponse(responseCode = "400", ref = "#/components/responses/UserProfileBadRequest"),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized"),
            @ApiResponse(responseCode = "503", ref = "#/components/responses/AuthenticationUnavailable"),
            @ApiResponse(responseCode = "500", ref = "#/components/responses/InternalServerError")
    })
    ResponseEntity<CurrentUserResponse> updateProfile(
            @Parameter(hidden = true) AuthenticatedAccount account, UpdateUserProfileRequest request);

    @Operation(summary = "내 프로필 사진 업로드", description = "JPEG·PNG를 512px 정사각형 JPEG로 변환해 저장하고 변경된 회원 정보를 반환합니다. 이전 업로드 사진은 교체합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "사진이 변경된 회원 정보. Cache-Control: no-store"),
            @ApiResponse(responseCode = "400", ref = "#/components/responses/UserProfileBadRequest"),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized"),
            @ApiResponse(responseCode = "500", ref = "#/components/responses/InternalServerError")
    })
    ResponseEntity<CurrentUserResponse> uploadProfileImage(
            @Parameter(hidden = true) AuthenticatedAccount account, UploadProfileImageRequest request);

    @Operation(hidden = true)
    ResponseEntity<byte[]> profileImage(String imageId);
}
