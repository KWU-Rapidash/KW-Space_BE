# KLAS 로컬 E2E 테스트

실제 KLAS 계정으로 `LoginSecurity.do`부터 학적 정보 조회까지 전체 흐름을 검증하는 opt-in 테스트다. 외부 KLAS 상태와
개인 자격증명에 의존하므로 기본 `test`, `check`, CI에서는 실행하지 않는다.

## 실행

저장소 루트에서 다음 스크립트를 실행한다.

```bash
./scripts/run-klas-e2e.sh
```

스크립트가 학번과 KLAS 비밀번호를 입력받는다. 비밀번호 입력은 터미널에 표시되지 않으며 두 값은 Gradle 자식 프로세스의
환경변수로만 전달된다. credential 파일과 shell command argument를 사용하지 않는다.
Gradle daemon에 credential 환경변수가 남지 않도록 스크립트는 `--no-daemon`으로 실행한다.

## 검증 범위

- `LoginSecurity.do` 공개키 및 세션 Cookie 조회
- RSA `loginToken` 생성과 `LoginConfirm.do` 인증
- `StandStdPage.do` 현재 학기 해석
- `ToeicInfoStd.do` 학번·이름 조회
- 요청 학번과 KLAS 응답 학번 일치

테스트 이름, assertion 메시지, 저장소 파일에는 학번·비밀번호·이름을 기록하지 않는다. Gradle `build/` 결과는 Git에서
제외되지만, 테스트가 끝난 뒤 공유할 필요가 없다면 삭제한다. KLAS client 예외의 상세 원인도 테스트 리포트에 연결하지
않고 일반 실패 메시지만 기록한다.
