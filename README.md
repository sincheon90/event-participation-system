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

API の詳細なリクエスト・レスポンス仕様は [docs/api-spec.md](docs/api-spec.md) を参照してください。

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

Kafka は、イベント参加 API の処理結果を API のメイン処理から分離し、非同期で保存・処理するために使用します。

Consumer は API とは独立した Worker プロセスとして実行でき、参加結果履歴の保存や後続処理を担当します。

### Topic

```text
participation-result
```

Topic は Consumer の並列処理を考慮して 3 Partition で構成します。

### Producer

Spring Boot API が Producer として動作します。生成された `ParticipationResponse` を基に `ParticipationResultEvent` を作成し、成功・失敗にかかわらず発行します。

発行対象:

```text
参加成功
Redis による重複検出
DB Unique 制約による重複検出
Event 不存在
Mission 不存在
User 不存在
```

基本フロー:

```text
参加処理実行
  ↓
ParticipationResponse 生成
  ↓
ParticipationResultEvent 生成・発行
  ↓
ParticipationResponse 返却
```

### エラーイベントの扱い

初期実装では、参加成功、重複参加、対象データ不存在など、API が処理結果として返却できるケースを Kafka への発行対象とします。

予期しないエラーのイベント発行については、DB トランザクションのロールバックと Kafka 発行の整合性を考慮する必要があるため、今後の拡張として扱います。

### Consumer

Consumer は `ParticipationResultEvent` を受信し、初期実装ではすべての結果を `participation_result_logs` に保存します。

```text
SUCCESS
DUPLICATE
EVENT_NOT_FOUND
MISSION_NOT_FOUND
USER_NOT_FOUND
```

成功イベントに対する `event_statistics` の更新と `point_histories` の保存は今後の拡張とします。

### Consumer Group と水平スケーリング

すべての Worker は同じ Consumer Group ID を使用します。

```text
participation-worker-group
```

```text
Partition 0 → Worker 1
Partition 1 → Worker 2
Partition 2 → Worker 3
```

```bash
docker compose up -d --scale app-worker=3
```

Topic は 3 Partition で構成し、同一 Consumer Group の最大 3 台の Worker に処理を分散できます。

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

#### Event 不存在時

```json
{
  "eventType": "PARTICIPATION_FAILED",
  "participationId": null,
  "eventId": 1,
  "missionId": 1,
  "userId": 100,
  "resultStatus": "EVENT_NOT_FOUND",
  "message": "Event not found",
  "occurredAt": "2026-07-05T12:00:05"
}
```


---

## ローカル環境の起動

本プロジェクトでは、MySQL、Redis、Kafka、API、WorkerをDocker Composeで起動します。

### アプリケーションJARのビルド

```bash
./gradlew clean bootJar
```

Dockerfileは `build/libs/*.jar` をイメージへコピーするため、初回起動時またはJavaコードを変更した場合は、先にJARをビルドします。

### Docker Composeの起動

```bash
docker compose up -d --build --scale app-worker=3
```

`--build` は、コンテナを起動する前にDockerイメージを再ビルドするオプションです。新しく生成したJARをイメージへ反映する場合に使用します。

`--scale app-worker=3` は、同一Consumer GroupのWorkerを3台起動し、3つのKafka Partitionを並列処理するためのオプションです。

コンテナの起動状態を確認します。

```bash
docker compose ps
```

Javaコードに変更がなく、イメージの再ビルドが不要な場合は次のように起動できます。

```bash
docker compose up -d
```

### 環境の停止

```bash
docker compose down
```

データを含むVolumeも削除する場合は、以下を実行します。

```bash
docker compose down -v
```
