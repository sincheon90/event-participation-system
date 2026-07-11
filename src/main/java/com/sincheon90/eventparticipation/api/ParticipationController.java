package com.sincheon90.eventparticipation.api;

import com.sincheon90.eventparticipation.api.dto.ParticipationRequest;
import com.sincheon90.eventparticipation.api.dto.ParticipationResponse;
import com.sincheon90.eventparticipation.service.ParticipationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Participation", description = "イベントミッション参加API")
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/events/{eventId}/missions/{missionId}/participate")
public class ParticipationController {

    private final ParticipationService participationService;

    @PostMapping
    public ResponseEntity<ParticipationResponse> participate(
            @PathVariable Long eventId,
            @PathVariable Long missionId,
            @Valid @RequestBody ParticipationRequest request
    ) {
        ParticipationResponse response = participationService.participate(eventId, missionId, request);

        HttpStatus httpStatus = switch (response.getStatus()) {
            case SUCCESS -> HttpStatus.CREATED;
            case DUPLICATE -> HttpStatus.CONFLICT;
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };

        return ResponseEntity.status(httpStatus).body(response);
    }
}
