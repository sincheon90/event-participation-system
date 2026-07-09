package com.sincheon90.eventparticipation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ParticipationRequest {

    @Schema(description = "ユーザーID", example = "1001")
    @NotNull
    private Long userId;
}
