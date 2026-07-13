package com.sincheon90.eventparticipation.kafka.consumer;

import com.sincheon90.eventparticipation.domain.participation.ParticipationResultLog;
import com.sincheon90.eventparticipation.domain.participation.ParticipationResultLogRepository;
import com.sincheon90.eventparticipation.domain.participation.ParticipationResultStatus;
import com.sincheon90.eventparticipation.kafka.ParticipationResultEvent;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ParticipationResultEventHandler {

    private final ParticipationResultLogRepository resultLogRepository;

    @Transactional
    public void handle(ParticipationResultEvent event) {
        saveResultLog(event);

        if (ParticipationResultStatus.SUCCESS.equals(event.resultStatus())) {
            processSuccess(event);
        }
    }

    private void saveResultLog(ParticipationResultEvent event) {
        ParticipationResultLog resultLog = ParticipationResultLog.builder()
                .participationId(event.participationId())
                .eventId(event.eventId())
                .missionId(event.missionId())
                .userId(event.userId())
                .status(event.resultStatus())
                .message(event.message())
                .build();


        resultLogRepository.save(resultLog);
    }

    private void processSuccess(ParticipationResultEvent event) {
        // event_statistics の更新
        // point_histories の保存
    }
}
