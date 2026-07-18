# KW-Space
서울특별시에 위치한 대 광운대학교의 대 인공지능융합대학에서 사용중인 새빛관 대여 시스템 BE

## 배포

- 운영 주소: `https://kw-space.leehyowon14.dev`
- GitHub-hosted runner에서 테스트와 ARM64 Docker 이미지 빌드를 수행합니다.
- 빌드한 이미지는 `ghcr.io/kwu-rapidash/kw-space-be`에 commit SHA 태그로 게시합니다.
- `rpi4-kw-space` runner는 이미지를 빌드하지 않고 Docker Compose 배포만 수행합니다.
- 애플리케이션은 서버의 `127.0.0.1:3002`에 바인딩되며 nginx를 통해 공개됩니다.
- MySQL은 `shared-db` Compose 프로젝트에서 공용 인스턴스로 실행하고, KW-Space 전용 `kw_space` database와 계정을 사용합니다.
- MySQL 포트는 호스트에 공개하지 않으며 애플리케이션은 외부 Docker network `shared-db`로 접속합니다.

배포 workflow에는 다음 repository secret이 필요합니다.

- `KW_SPACE_AUTH_JWT_SECRET`
- `KW_SPACE_AUTH_JWT_ACCESS_TOKEN_TTL` (선택, 기본값 `1h`)
- `SHARED_DB_ROOT_PASSWORD`
- `DB_USERNAME`
- `DB_PASSWORD`
