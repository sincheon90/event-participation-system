# API Specification

## 概要

本ドキュメントでは、Event Participation System の主要 API を定義します。

本システムでは、イベント参加 API の処理結果をユーザーへ即時返却しつつ、参加成功・重複参加・失敗を含む処理結果を Kafka 経由で非同期に記録します。

対象 API は以下です。

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/events` | イベント作成 |
| GET | `/api/events` | イベント一覧照会 |
| GET | `/api/events/{eventId}` | イベント詳細照会 |
| POST | `/api/events/{eventId}/missions` | ミッション作成 |
| GET | `/api/events/{eventId}/missions` | ミッション一覧照会 |
| GET | `/api/events/{eventId}/missions/{missionId}` | ミッション詳細照会|
| POST | `/api/events/{eventId}/missions/{missionId}/participate` | イベント参加 |
| GET | `/api/events/{eventId}/stats` | イベント統計照会 |
| GET | `/api/events/{eventId}/notification-logs` | 通知処理履歴照会 |
| GET | `/api/users/{userId}/points` | ユーザー現在ポイント照会 |
| GET | `/api/users/{userId}/point-histories` | ユーザーポイント履歴照会 |

---

## Participate

指定したイベント・ミッションに参加します。

```http
POST /api/events/{eventId}/missions/{missionId}/participate
```

### Path Parameter

| Name | Description |
|---|---|
| eventId | Event ID |
| missionId | Mission ID |

### Request

```json
{
  "userId": 100
}
```

### Success Response

```http
HTTP/1.1 201 Created

{
  "participationId": 1,
  "status": "SUCCESS",
  "message": "Participation completed"
}
```

### Duplicate Response

```http
HTTP/1.1 409 Conflict

{
  "status": "DUPLICATE",
  "message": "User already participated in this mission"
}
```

### Error Response: Event Not Found

```http
HTTP/1.1 404 Not Found

{
  "status": "NOT_FOUND",
  "message": "Event not found"
}
```

### Error Response: Mission Not Found

```http
HTTP/1.1 404 Not Found

{
  "status": "NOT_FOUND",
  "message": "Mission not found"
}
```

### Error Response: Unexpected Error

```http
HTTP/1.1 500 Internal Server Error

{
  "status": "ERROR",
  "message": "Unexpected error occurred"
}
```
