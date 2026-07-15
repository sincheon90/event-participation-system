DELETE FROM participation_result_logs;

DELETE FROM point_histories;

DELETE FROM participations;


INSERT
IGNORE INTO users (id, name, created_at)
VALUES
(1001, 'Test User', CURRENT_TIMESTAMP),
(1002, 'Test User', CURRENT_TIMESTAMP),
(1003, 'Test User', CURRENT_TIMESTAMP),
(1004, 'Test User', CURRENT_TIMESTAMP),
(1005, 'Test User', CURRENT_TIMESTAMP);

INSERT
IGNORE INTO events (id,
                    title,
                    description,
                    start_at,
                    end_at,
                    created_at)
VALUES (1,
        'Test Event',
        '動作確認および負荷試験用のイベント',
        '2026-01-01 00:00:00',
        '2027-12-31 23:59:59',
        CURRENT_TIMESTAMP);


INSERT
IGNORE INTO missions (id,
                      event_id,
                      title,
                      mission_type,
                      created_at)
VALUES (1,
        1,
        'Test Mission',
        'PARTICIPATION',
        CURRENT_TIMESTAMP);
