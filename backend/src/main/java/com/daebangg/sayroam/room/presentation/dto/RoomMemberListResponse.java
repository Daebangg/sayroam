package com.daebangg.sayroam.room.presentation.dto;

import com.daebangg.sayroam.room.application.dto.RoomMemberProfileResult;
import com.daebangg.sayroam.room.domain.RoomMemberStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(requiredProperties = {"members"})
public record RoomMemberListResponse(
        @Schema(description = "방 멤버. 나간 멤버도 포함하며 순서는 보장하지 않음")
        List<RoomMemberProfileResponse> members) {

    public static RoomMemberListResponse from(List<RoomMemberProfileResult> results) {
        return new RoomMemberListResponse(results.stream()
                .map(RoomMemberProfileResponse::from)
                .toList());
    }

    @Schema(name = "RoomMemberProfileResponse",
            requiredProperties = {"userId", "nickname", "profileImageUrl", "status"})
    public record RoomMemberProfileResponse(
            @Schema(description = "회원 ID. 메시지의 senderId와 같은 값", example = "1")
            Long userId,
            @Schema(description = "닉네임. 탈퇴한 회원은 '탈퇴한 회원'", example = "민수")
            String nickname,
            @Schema(description = "프로필 이미지 주소. 없으면 null", example = "/brand/avatars/traveler.svg",
                    types = {"string", "null"})
            String profileImageUrl,
            @Schema(description = "참여 상태. JOINED는 참여 중, LEFT는 나감, KICKED는 내보내짐", enumAsRef = true)
            RoomMemberStatus status) {

        static RoomMemberProfileResponse from(RoomMemberProfileResult result) {
            return new RoomMemberProfileResponse(result.userId(), result.nickname(), result.profileImageUrl(),
                    result.status());
        }
    }
}
