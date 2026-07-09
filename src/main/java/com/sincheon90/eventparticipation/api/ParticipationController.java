package com.sincheon90.eventparticipation.api;

import com.sincheon90.eventparticipation.dto.ParticipationRequest;
import com.sincheon90.eventparticipation.dto.ParticipationResponse;
import com.sincheon90.eventparticipation.service.ParticipationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Participation", description = "イベントミッション参加API")
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/events/{eventId}/missions/{missionId}/participate")
public class ParticipationController {

    private final ParticipationService participationService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ParticipationResponse participate(
            @PathVariable Long eventId,
            @PathVariable Long missionId,
            @Valid @RequestBody ParticipationRequest request
            ) {
        return participationService.participate(eventId, missionId, request);
    }
}
