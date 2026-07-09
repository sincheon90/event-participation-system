package com.sincheon90.eventparticipation.dto;

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
    private String status;

    @Schema(description = "メッセージ", example = "Participation completed")
    private String message;

    public static ParticipationResponse success(Long participationId) {
        return new ParticipationResponse(
                participationId,
                "SUCCESS",
                "Participation completed"
        );

    }
}
