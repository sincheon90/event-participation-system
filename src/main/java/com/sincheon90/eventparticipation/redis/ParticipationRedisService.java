package com.sincheon90.eventparticipation.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class ParticipationRedisService {

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * Redisに重複参加防止用のキーを登録します。
     * @param eventId
     * @param missionId
     * @param userId
     * @return 登録成功時はtrue、キーがすでに存在する場合はfalse
     */
    public boolean tryLock(Long eventId, Long missionId, Long userId) {
        String key = createKey(eventId, missionId, userId);

        Boolean success = stringRedisTemplate.opsForValue()
                .setIfAbsent(key, "LOCKED", Duration.ofSeconds(10));

        return Boolean.TRUE.equals(success);
    }

    public void unlock(Long eventId, Long missionId, Long userId) {
        stringRedisTemplate.delete(createKey(eventId, missionId, userId));
    }

    private String createKey(Long eventId, Long missionId, Long userId) {
        return "participation:%d:%d:%d".formatted(eventId, missionId, userId);
    }

}
