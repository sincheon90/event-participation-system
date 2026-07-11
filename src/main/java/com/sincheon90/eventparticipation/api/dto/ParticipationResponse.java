package com.sincheon90.eventparticipation.api.dto;

import com.sincheon90.eventparticipation.domain.participation.ParticipationResultStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class ParticipationResponse {

    @Schema(description = "参加ID", example = "1")
    private Long participationId;

    @Schema(description = "処理結果", example = "SUCCESS")
    private ParticipationResultStatus status;

    @Schema(description = "メッセージ", example = "Participation completed")
    private String message;

    public static ParticipationResponse success(Long participationId) {
        return new ParticipationResponse(
                participationId,
                ParticipationResultStatus.SUCCESS,
                "Participation completed"
        );
    }

    public static ParticipationResponse duplicate() {
        return new ParticipationResponse(
                null,
                ParticipationResultStatus.DUPLICATE,
                "User already participated in this mission"
        );
    }

    public static ParticipationResponse eventNotFound() {
        return new ParticipationResponse(
                null,
                ParticipationResultStatus.NOT_FOUND,
                "Event not found"
        );
    }

    public static ParticipationResponse missionNotFound() {
        return new ParticipationResponse(
                null,
                ParticipationResultStatus.NOT_FOUND,
                "Mission not found"
        );
    }

    public static ParticipationResponse userNotFound() {
        return new ParticipationResponse(
                null,
                ParticipationResultStatus.NOT_FOUND,
                "User not found"
        );
    }
}
