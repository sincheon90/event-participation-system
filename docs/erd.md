# ERD

## 概要

本システムでは、イベント参加処理のために以下のテーブルを使用します。

```text
users
events
missions
participations
notification_logs
event_statistics
point_histories
user_points
```

中核となるテーブルは `participations` です。

`participations` は、ユーザーがどのイベントのどのミッションに参加したかを保存するテーブルであり、参加状態の最終的な正本として扱います。

また、参加完了後の付随処理として、通知処理履歴、統計集計、ポイント履歴、ユーザー別ポイント残高を管理します。


---

## Mermaid ERD

```mermaid
erDiagram

    users ||--o{ participations : ""
    users ||--o{ notification_logs : ""
    users ||--o{ point_histories : ""
    users ||--o| user_points : ""

    events ||--o{ missions : ""
    events ||--o{ participations : ""
    events ||--o{ notification_logs : ""
    events ||--|| event_statistics : ""
    events ||--o{ point_histories : ""

    missions ||--o{ participations : ""
    missions ||--o{ notification_logs : ""
    missions ||--|| event_statistics : ""
    missions ||--o{ point_histories : ""

    participations ||--o{ notification_logs : ""


    users {
        bigint id PK
        varchar name
        datetime created_at
    }

    events {
        bigint id PK
        varchar title
        text description
        datetime start_at
        datetime end_at
        datetime created_at
    }

    missions {
        bigint id PK
        bigint event_id FK
        varchar title
        varchar mission_type
        datetime created_at
    }

    participations {
        bigint id PK
        bigint user_id FK
        bigint event_id FK
        bigint mission_id FK
        datetime created_at
    }

    notification_logs {
        bigint id PK
        bigint participation_id FK
        bigint user_id
        bigint event_id
        bigint mission_id
        varchar notification_type
        varchar status
        varchar message
        datetime processed_at
    }

    event_statistics {
        bigint id PK
        bigint event_id FK
        bigint mission_id FK
        bigint participation_count
        datetime updated_at
    }


    point_histories {
        bigint id PK
        bigint user_id FK
        bigint event_id FK
        bigint mission_id FK
        int point
        varchar reason
        datetime created_at
    }

    user_points {
        bigint id PK
        bigint user_id FK
        int point
        datetime updated_at
    }
```
