# Auth CSRF Policy

## Decision

Auth API는 선택지 A인 `SameSite=Lax + JSON API + CSRF 비활성화` 정책을 사용한다.

## Rationale

- `accessToken`은 `HttpOnly` Cookie로만 전달하고 JavaScript에서 직접 읽지 않는다.
- Cookie `SameSite=Lax`를 사용해 일반적인 cross-site subrequest의 자동 Cookie 전송을 제한한다.
- Auth API는 JSON request body만 받는다. 브라우저 form submit 기반의 `application/x-www-form-urlencoded`, `multipart/form-data`, `text/plain` state-changing 요청은 지원하지 않는다.
- Spring Security session은 `STATELESS`이며 서버 세션 기반 CSRF token을 사용하지 않는다.
- 상태 변경 API는 `GET`으로 제공하지 않는다.

## Security Notes

- CSRF를 비활성화하므로 cross-site credentialed request를 허용하는 CORS 정책을 추가할 때는 origin allowlist를 별도로 검토해야 한다.
- 프론트가 `X-XSRF-TOKEN` 헤더 처리를 지원하고 더 강한 브라우저 CSRF 방어가 필요해지면 `CookieCsrfTokenRepository` 기반 정책으로 재검토한다.
- Cookie의 `Secure`, `HttpOnly`, `SameSite`, `Path`, `Max-Age` 값은 auth Cookie 설정에서 관리한다.
