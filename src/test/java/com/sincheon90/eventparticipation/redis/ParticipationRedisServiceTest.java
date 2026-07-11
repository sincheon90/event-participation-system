package com.sincheon90.eventparticipation.redis;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ParticipationRedisServiceTest {

    private static final Long EVENT_ID = 1L;
    private static final Long MISSION_ID = 10L;
    private static final Long USER_ID = 100L;

    private static final String REDIS_KEY =
            "participation:1:10:100";

    private static final String LOCK_VALUE = "LOCKED";

    private static final Duration TTL =
            Duration.ofSeconds(10);

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private ParticipationRedisService participationRedisService;

    @BeforeEach
    void setUp() {
        participationRedisService =
                new ParticipationRedisService(stringRedisTemplate);
    }

    @Test
    @DisplayName("Redisキーが存在しない場合、参加ロックの取得に成功する")
    void tryLock_success() {
        // given
        when(stringRedisTemplate.opsForValue())
                .thenReturn(valueOperations);

        when(valueOperations.setIfAbsent(
                REDIS_KEY,
                LOCK_VALUE,
                TTL
        )).thenReturn(true);

        // when
        boolean result = participationRedisService.tryLock(
                EVENT_ID,
                MISSION_ID,
                USER_ID
        );

        // then
        assertThat(result).isTrue();

        verify(valueOperations).setIfAbsent(
                REDIS_KEY,
                LOCK_VALUE,
                TTL
        );
    }

    @Test
    @DisplayName("同一のRedisキーが既に存在する場合、参加ロックの取得に失敗する")
    void tryLock_duplicate() {
        // given
        when(stringRedisTemplate.opsForValue())
                .thenReturn(valueOperations);

        when(valueOperations.setIfAbsent(
                REDIS_KEY,
                LOCK_VALUE,
                TTL
        )).thenReturn(false);

        // when
        boolean result = participationRedisService.tryLock(
                EVENT_ID,
                MISSION_ID,
                USER_ID
        );

        // then
        assertThat(result).isFalse();

        verify(valueOperations).setIfAbsent(
                REDIS_KEY,
                LOCK_VALUE,
                TTL
        );
    }

    @Test
    @DisplayName("Redisの応答がnullの場合、参加ロックの取得に失敗したものとして処理する")
    void tryLock_nullResponse() {
        // given
        when(stringRedisTemplate.opsForValue())
                .thenReturn(valueOperations);

        when(valueOperations.setIfAbsent(
                REDIS_KEY,
                LOCK_VALUE,
                TTL
        )).thenReturn(null);

        // when
        boolean result = participationRedisService.tryLock(
                EVENT_ID,
                MISSION_ID,
                USER_ID
        );

        // then
        assertThat(result).isFalse();

        verify(valueOperations).setIfAbsent(
                REDIS_KEY,
                LOCK_VALUE,
                TTL
        );
    }

    @Test
    @DisplayName("参加ロックを解除する場合、Redisキーを削除する")
    void unlock_success() {
        // when
        participationRedisService.unlock(
                EVENT_ID,
                MISSION_ID,
                USER_ID
        );

        // then
        verify(stringRedisTemplate).delete(REDIS_KEY);
    }
}