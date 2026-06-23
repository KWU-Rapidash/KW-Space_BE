# 강의실 예약 가능 시간 조회

## Request

### Header

```json
{
  "Authorization": "Bearer {accessToken}"
}
```

### Path variable

| Name | Description |
| --- | --- |
| classroomId | 강의실 식별자. 현재는 `saebit-{호실}` 형식 |

### Query parameter

| Name | Required | Description |
| --- | --- | --- |
| date | true | 조회 날짜 |

### Example

```http
GET /api/v1/classrooms/saebit-101/times?date=2024-04-01
```

## Response

### 200

```json
[
  {
    "time": "09:00",
    "available": true
  },
  {
    "time": "10:30",
    "available": false
  }
]
```

### 400

```json
{
  "message": "필수 입력값이 누락되었습니다."
}
```

### 401

```json
{
  "message": "인증이 필요합니다."
}
```

### 404

```json
{
  "message": "강의실 정보를 찾을 수 없습니다."
}
```
