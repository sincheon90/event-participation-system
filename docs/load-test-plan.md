# 負荷テスト計画

## 1. 目的

イベント開始直後に多数のユーザーが参加ボタンを押し、短時間にリクエストが集中する状況を想定する。本テストでは、イベント参加 API が負荷の変化に対してどのような性能特性を示すかを測定し、重複参加防止と非同期結果保存が期待どおりに動作することを確認する。

主な確認項目は以下のとおりである。

- 通常負荷、高負荷、スパイク負荷における API の成功率と応答時間
- 継続的なリクエスト処理における Throughput と安定性
- 同一ユーザーからの同時リクエストに対する重複参加防止
- Kafka 経由で保存される参加結果ログの件数と処理遅延
- 高負荷時に API と非同期 Consumer のどちらで処理待ちが発生するか

本テストは性能ベースラインの取得を目的とする。応答時間および Throughput の正式な SLO は未定義であるため、性能値による合否判定は行わず、測定結果を今後の目標値設定に利用する。

---

## 2. テスト対象

対象 API:

```http
POST /api/events/{eventId}/missions/{missionId}/participate
```

リクエスト例:

```json
{
  "userId": 1
}
```

主な処理フロー:

```text
JMeter
  → Spring Boot API
  → Event / Mission / User の存在確認
  → Redis SETNX による重複確認
  → participations へ同期保存
  → Kafka へ参加結果イベントを送信
  → API レスポンス

Kafka Consumer
  → participation_result_logs へ非同期保存
```

---

## 3. テスト環境

| 項目 | 使用技術 |
|---|---|
| Application Framework | Spring Boot 3.5 |
| Language | Java 21 |
| Database | MySQL 8.4 |
| Cache | Redis 7 |
| Message Broker | Apache Kafka 4.1 |
| Load Test Tool | Apache JMeter 5.6.3 |
| Container Environment | Docker Compose |
| Host OS | macOS |

再現性を確保するため、実行時にはホストの CPU、メモリ、Docker Desktop のリソース上限、API インスタンス数、Worker 数を併せて記録する。

---

## 4. テストデータ

| 項目 | 値 |
|---|---:|
| Event ID | 1 |
| Mission ID | 1 |
| 登録ユーザー数 | 200,000 |
| 1 シナリオあたりの最大リクエスト数 | 100,000 |

通常、高負荷、スパイク、継続負荷では、1 リクエストごとに異なる `userId` を使用する。重複参加テストでは、全 Thread が同じ `userId` を使用する。

シナリオ間で同じユーザーを再利用する場合は、次のテスト開始前に `participations` と Redis の参加キーを初期化する。`participation_result_logs` は測定結果として保持し、テスト実行時刻でシナリオを区別する。

### 4.1 テストデータの登録

`users.csv` を MySQL コンテナの `secure-file-priv` 対象ディレクトリへコピーし、データ登録用 SQL を実行する。

```bash
# CSV を MySQL コンテナへコピー
docker compose cp \
  load-test/data/users.csv \
  mysql:/var/lib/mysql-files/users.csv

# データ登録用 SQL を実行
docker compose exec -T mysql \
  mysql -u root -proot event_participation \
  < load-test/load-test-data.sql
```

### 4.2 テストデータの確認

登録後、ユーザー総数が計画値の 200,000 件であることと、先頭データが正常に登録されていることを確認する。

```bash
# ユーザー総数を確認
docker compose exec mysql \
  mysql -u root -proot event_participation \
  -e "SELECT COUNT(*) FROM users;"

# 先頭 10 件を確認
docker compose exec mysql \
  mysql -u root -proot event_participation \
  -e "SELECT * FROM users ORDER BY id LIMIT 10;"
```

### 4.3 参加データの初期化

各シナリオの実行前に、前回のテストで登録された参加データを削除する。

```bash
# 参加データを削除
docker compose exec mysql \
  mysql -u root -proot event_participation \
  -e "DELETE FROM participations;"
```

同じユーザーを再利用する場合は、上記の DB 初期化に加えて Redis の参加キーも初期化する。

---

## 5. テストシナリオ

| 実行順 | シナリオ | Threads | Ramp-up | Loop Count | 総リクエスト数 | ユーザー条件 |
|---:|---|---:|---:|---:|---:|---|
| 1 | 通常負荷 | 100 | 30 秒 | 100 | 10,000 | リクエストごとに一意 |
| 2 | 高負荷 | 250 | 10 秒 | 200 | 50,000 | リクエストごとに一意 |
| 3 | スパイク | 500 | 1 秒 | 20 | 10,000 | リクエストごとに一意 |
| 4 | 継続負荷 | 100 | 30 秒 | 1,000 | 100,000 | リクエストごとに一意 |
| 5 | 重複参加 | 1,000 | 1 秒 | 1 | 1,000 | 全リクエストで同一 |

### 5.1 通常負荷

緩やかな Ramp-up で通常時の応答時間とエラー率を測定し、以降のシナリオと比較するためのベースラインとする。

### 5.2 高負荷

250 Threads を 10 秒で投入し、処理量の増加に伴う平均応答時間と p95・p99 の変化を測定する。

### 5.3 スパイク

500 Threads を 1 秒で投入し、急激な負荷上昇に対するエラー、タイムアウト、テールレイテンシを確認する。

### 5.4 継続負荷

100 Threads で 100,000 件を連続処理し、継続区間の Throughput、応答時間、非同期処理の滞留を確認する。

### 5.5 重複参加

同一ユーザーから 1,000 件を同時に送信し、最初の 1 件だけが成功し、残りが `409 Conflict` となることを確認する。

---

## 6. JMeter 実行設定

テストプラン:

```text
load-test/participation-load-test.jmx
```

主な JMeter Property:

| Property | 用途 |
|---|---|
| `threads` | Thread 数 |
| `rampUp` | Ramp-up 秒数 |
| `loopCount` | Thread ごとの繰り返し回数 |
| `startUserId` | 一意ユーザーの開始 ID |
| `fixedUserId` | 重複参加テストで使用する固定ユーザー ID |
| `host` / `port` | API 接続先 |

実行コマンド例:

```bash
jmeter -n \
  -t load-test/participation-load-test.jmx \
  -Jthreads=100 \
  -JrampUp=30 \
  -JloopCount=100 \
  -JstartUserId=1 \
  -l load-test/results/normal-result.jtl \
  -e \
  -o load-test/results/normal-report
```

各シナリオでは `threads`、`rampUp`、`loopCount` と出力ファイル名をシナリオ表に合わせて変更する。重複参加テストでは `startUserId` の代わりに `fixedUserId` を指定する。

---

## 7. 測定項目

### 7.1 API 処理

JTL および JMeter Dashboard から以下を取得する。

- 総リクエスト数
- HTTP ステータス別件数
- JMeter エラー率
- 平均、Median、p90、p95、p99、最大応答時間
- Throughput（requests/sec）
- テスト開始から最後のレスポンス完了までの実行時間
- タイムアウトおよび 5xx の件数

Throughput は Ramp-up を含むテスト全体を基準に算出されるため、シナリオ間の比較では Ramp-up と実行時間の違いを考慮する。

### 7.2 非同期処理

`participation_result_logs` の以下の時刻差をミリ秒単位で集計する。

```text
非同期処理時間
  = created_at - event_created_at
```

- `event_created_at`: 同期処理完了後に Kafka 送信用イベントを生成した時刻
- `created_at`: Consumer が参加結果ログを保存する際の `@PrePersist` 実行時刻

シナリオごとに以下を取得する。

- 保存件数および status 別件数
- 最小、平均、p50、p90、p95、p99、最大処理時間

---

## 8. 検証基準

### 8.1 共通

- JTL のリクエスト数が計画値と一致すること
- タイムアウトおよび予期しない 5xx が 0 件であること
- `participation_result_logs` の保存件数がリクエスト数と一致すること

### 8.2 通常・高負荷・スパイク・継続負荷

- 全リクエストが `201 Created` であること
- 非同期結果ログがすべて `SUCCESS` であること

### 8.3 重複参加

- `201 Created` が 1 件であること
- `409 Conflict` が 999 件であること
- `participation_result_logs` が `SUCCESS` 1 件、`DUPLICATE` 999 件であること
- `participations` の対象ユーザーのレコードが最終的に 1 件であること

`409 Conflict` は JMeter 上ではエラーとして集計されるが、本シナリオでは期待する業務結果として評価する。

---

## 9. 成果物

| 成果物 | パス | Git 管理 |
|---|---|---|
| JMeter Test Plan | `load-test/participation-load-test.jmx` | 対象 |
| API 集計 | `load-test/results/api-statistics.csv` | 対象 |
| 非同期処理集計 | `load-test/results/async-statistics.csv` | 対象 |
| JMeter Statistics | `load-test/results/{scenario}-report/statistics.json` | 対象 |
| Raw JTL | `load-test/results/{scenario}-result.jtl` | 対象外（ローカル保存） |
| HTML Dashboard | `load-test/results/{scenario}-report/index.html` | 対象外（ローカル保存） |
| テスト結果 | [load-test-result.md](load-test-result.md) | 対象 |

非同期処理時間、DB 件数、Consumer lag など JTL に含まれない結果は、テスト終了直後に CSV またはテキストとして追加保存する。
