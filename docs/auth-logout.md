# Auth Logout Policy

## Endpoint

`POST /api/v1/auth/logout`

## Decision

- 로그아웃은 `accessToken` Cookie 삭제만 수행한다.
- `tokenVersion`은 증가시키지 않는다.
- 미인증 요청과 잘못된 `accessToken` Cookie가 포함된 요청도 idempotent 성공으로 처리한다.

## Response Cookie

삭제 Cookie는 발급 Cookie와 같은 보안 속성을 유지한다.

- `Name=accessToken`
- `HttpOnly=true`
- `SameSite=Lax`
- `Path=/`
- `Max-Age=0`
- `Secure`는 profile별 Cookie 정책을 따른다.

탈취 토큰까지 즉시 무효화해야 하는 정책이 필요해지면 별도 이슈에서 `tokenVersion` 증가 또는 refresh token rotation을 도입한다.
