# Event Participation System Prototype

Spring Boot / Redis / Kafka / MySQL を用いたイベント参加処理プロトタイプ

## 概要
Event Participation System は、イベント参加リクエストが短時間に集中する状況を想定したバックエンドプロトタイプです。
本システムでは、同一ユーザーによる重複参加を防ぎながら、参加処理と参加完了後の付随処理を分離することを目的としています。

---

## 解決したい課題
イベント参加、キャンペーン申請のような機能では、特定時間帯にアクセスが集中する可能性があります。
その際、以下のような課題が発生します。

- 同一ユーザーの重複参加
- 短時間に集中する参加リクエスト
- DB への不要な重複 insert
- 参加処理後のログ保存・集計処理による API 応答遅延

本プロジェクトでは、これらの課題に対して以下の方針で対応します。

```text
Redis SETNX
  → 重複リクエストを高速にブロックする

DB Unique Constraint
  → 最終的な参加データの整合性を保証する

Kafka
  → ログ保存・統計集計を非同期処理に分離する
```

---

## 基本コンセプト

本システムの中心となる考え方は、以下の 3 点です。

### 1. 参加処理は同期的に行う

ユーザーに対して、参加が成功したかどうかをすぐに返す必要があるため、参加可否の判定と `participations` への保存は同期処理として実行します。

対象処理:

```text
Event / Mission の存在確認
Redis による重複チェック
participations への保存
API レスポンス返却
```

### 2. 参加完了後の付随処理は非同期に分離する

参加完了通知の送信履歴保存は、参加 API の応答前に必ず完了している必要はありません。

そのため、参加保存後に Kafka へイベントを発行し、Consumer 側で後続処理を実行します。

対象処理:

```text
participation_result_logs への保存
event_statistics の更新
point_histories へのポイント付与履歴保存
```

### 3. Redis と DB Unique 制約を組み合わせる

Redis は高速な重複チェックに適していますが、Redis のみで最終的な整合性を保証することはできません。

そのため、Redis を一次防御、MySQL の Unique 制約を最終防御として利用します。

```text
Redis
  → 高速な重複リクエストの遮断

MySQL Unique Constraint
  → 最終的なデータ整合性の保証
```

---

## アーキテクチャ

```text
[Client]
        |
        | POST /api/events/{eventId}/missions/{missionId}/participate
        v
[Spring Boot API]
        |
        | イベント参加処理
        | 1. Event / Mission 存在確認
        | 2. Redis SETNX による重複参加チェック
        | 3. participations へ保存
        | 4. Kafka へ ParticipationCreatedEvent を発行
        v
[Kafka Topic: participation-events]
        |
        v
[Kafka Consumer]
        |
        | 非同期後続処理
        | 1. participation_result_logs へ応答結果履歴を保存
        | 2. event_statistics 更新
        | 3. point_histories 保存
        v
[MySQL]
```


---

## 技術スタック

| 区分 | 技術 |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.5.16 |
| Web | Spring Web |
| ORM | Spring Data JPA |
| Database | MySQL |
| Cache / Duplicate Check | Redis |
| Message Queue | Apache Kafka |
| Local Environment | Docker Compose |
| Performance Test | JMeter |

---

## API 一覧

| Method | Endpoint                                                 | Description |
|---|----------------------------------------------------------|---|
| POST | `/api/events`                                            | イベント作成 |
| POST | `/api/events/{eventId}/missions`                         | ミッション作成 |
| POST | `/api/events/{eventId}/missions/{missionId}/participate` | イベント参加 |
| GET | `/api/events/{eventId}/stats`                            | イベント統計照会 |
| GET | `/api/events/{eventId}/participation-result-logs`        | 応答結果履歴照会 |

---

## データモデル

詳細は [docs/erd.md](docs/erd.md) を参照してください。

主なテーブルは以下です。

```text
users
events
missions
participations
participation_result_logs
event_statistics
point_histories
```

中核となるテーブルは `participations` です。

```sql
UNIQUE (user_id, event_id, mission_id)
```

この制約により、同一ユーザーは同一イベントの同一ミッションに一度だけ参加できます。

---

## Redis による重複参加防止

Redis は、同一ユーザーによる重複参加リクエストを高速に遮断するために使用します。

### Redis Key

```text
participation:{eventId}:{missionId}:{userId}
```

例:

```text
participation:1:1:100
```

### 処理方式

1. 参加リクエストを受信する
2. Redis SETNX を実行する
3. SETNX 成功時のみ DB insert へ進む
4. SETNX 失敗時は重複参加としてレスポンスを返す

DB の Unique 制約だけでも重複参加は防げます。

しかし、同一ユーザーから短時間に大量の重複リクエストが発生した場合、すべてのリクエストが DB insert まで到達してしまいます。

Redis を前段に置くことで、明らかな重複リクエストを DB に到達する前に遮断できます。


---

## Kafka による非同期処理

Kafka は、イベント参加 API の処理結果を API のメイン処理から分離して保存するために使用します。

本システムでは、参加成功だけでなく、重複参加やエラーなどを含む API の処理結果をイベントとして Kafka に発行し、Consumer 側で非同期に保存します。

これにより、ユーザーへのレスポンス返却を優先しながら、参加リクエストの結果を後続で追跡できるようにします。

### Topic

```text
participation-events
```

### Producer

Spring Boot API サーバーが Producer として動作します。

イベント参加 API の処理後、参加成功・重複参加・エラーなどの結果を `ParticipationResultEvent` として Kafka Topic に発行します。

### Consumer

Kafka Consumer は、`ParticipationResultEvent` を受信して以下の処理を行います。

初期実装:

```text
participation_result_logs への保存
```

保存対象の例:

```text
参加成功
重複参加
Event / Mission 不存在
DB Unique 制約による重複検出
予期しないエラー
```

今後の拡張:

```text
participation_result_logs への応答結果履歴保存
event_statistics の更新
point_histories への保存
```

### Message Example

#### 参加成功時

```json
{
  "eventType": "PARTICIPATION_SUCCESS",
  "participationId": 1,
  "eventId": 1,
  "missionId": 1,
  "userId": 100,
  "resultStatus": "SUCCESS",
  "message": "Participation completed",
  "occurredAt": "2026-07-05T12:00:00"
}
```

#### 重複参加時

```json
{
  "eventType": "PARTICIPATION_DUPLICATE",
  "participationId": null,
  "eventId": 1,
  "missionId": 1,
  "userId": 100,
  "resultStatus": "DUPLICATE",
  "message": "User already participated in this mission",
  "occurredAt": "2026-07-05T12:00:03"
}
```

#### エラー発生時

```json
{
  "eventType": "PARTICIPATION_FAILED",
  "participationId": null,
  "eventId": 1,
  "missionId": 1,
  "userId": 100,
  "resultStatus": "ERROR",
  "message": "Unexpected error occurred",
  "occurredAt": "2026-07-05T12:00:05"
}
```


---

## ローカル環境の起動

本プロジェクトでは、ローカル開発環境のMySQLをDocker Composeで構築します。

### MySQLの起動

```bash
docker compose up -d
```

コンテナの起動状態を確認します。

```bash
docker compose ps
```

### アプリケーションの起動

MySQLコンテナの起動後、Spring Bootアプリケーションを実行します。

```bash
./gradlew bootRun
```

### 環境の停止

```bash
docker compose down
```

データを含むVolumeも削除する場合は、以下を実行します。

```bash
docker compose down -v
```
