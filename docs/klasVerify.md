# KLAS 인증 검증 및 이름 조회 명세

## 목적

서비스 회원가입과 비밀번호 재설정에서 사용자가 입력한 `klasId`와 `klasPassword`로 KLAS 계정 소유 여부를 검증한다.
검증 성공 후에는 KLAS 세션으로 현재 로그인 사용자의 학번과 이름을 조회해 `KlasAuthResult`에 매핑한다.

이 문서는 실제 KLAS 연동 이슈(#17)의 구현 기준이다. #6의 `FakeKlasAuthClient`는 이 흐름을 외부 호출 없이 흉내 내는 테스트 대역이다.

## 전체 흐름

1. 로그인 보안 정보 조회
   - `POST https://klas.kw.ac.kr/usr/cmn/login/LoginSecurity.do`
   - 응답의 `publicKey`와 `Set-Cookie`의 세션 쿠키를 보관한다.
2. 로그인 토큰 생성
   - 아래 payload를 JSON 문자열로 만든다.
   - `LoginSecurity.do`의 `publicKey`를 PEM public key로 복원한다.
   - RSA PKCS#1 v1.5 padding으로 암호화한 뒤 Base64 인코딩한다.
3. 로그인 확인
   - `POST https://klas.kw.ac.kr/usr/cmn/login/LoginConfirm.do`
   - `loginToken`과 1단계 세션 쿠키를 함께 보낸다.
   - 성공 조건을 만족하면 KLAS 인증 성공으로 본다.
4. 사용자 학적/이름 조회
   - `POST https://klas.kw.ac.kr/std/cps/inqire/ToeicInfoStd.do`
   - 로그인 성공 세션 쿠키를 그대로 사용한다.
   - 응답 배열 첫 항목의 `hakbun`, `kname`을 `KlasAuthResult`의 `klasId`, `name`으로 매핑한다.

## 로그인 보안 정보 조회

```http
POST /usr/cmn/login/LoginSecurity.do
Content-Type: application/json;charset=utf-8
Accept: application/json, text/plain, */*
```

성공 응답에서 사용하는 값:

```json
{
  "publicKey": "<rsa-public-key-body>"
}
```

성공 조건:

- HTTP status가 `2xx`다.
- JSON body에 비어 있지 않은 `publicKey`가 있다.
- `Set-Cookie`에 이후 로그인 확인 요청에 사용할 KLAS 세션 쿠키가 있다.

실패 처리:

- `publicKey`가 없거나 비어 있으면 KLAS 서버 오류로 처리한다.
- JSON 대신 HTML이 내려오면 KLAS 서버 오류 또는 로그인 페이지 응답으로 처리한다.
- 세션 쿠키가 없으면 로그인 확인을 이어갈 수 없으므로 KLAS 서버 오류로 처리한다.

## 로그인 토큰 생성

토큰 원본 payload:

```json
{
  "loginId": "<klasId>",
  "loginPwd": "<klasPassword>",
  "storeIdYn": "N"
}
```

처리 기준:

- `publicKey`는 아래 PEM 형식으로 감싼다.

```text
-----BEGIN PUBLIC KEY-----
<publicKey>
-----END PUBLIC KEY-----
```

- payload JSON 문자열을 RSA PKCS#1 v1.5 padding으로 암호화한다.
- payload는 공개키 modulus 기준 PKCS#1 v1.5 최대 평문 크기(`keyBytes - 11`) 이하여야 한다.
- 최대 크기를 초과하면 KLAS 요청을 계속하지 않고 인증 실패로 처리한다.
- 암호화 결과를 Base64 문자열로 만든다.
- `klasPassword`와 생성된 `loginToken`은 로그, 예외 메시지, 테스트 fixture, JWT claim, DB에 저장하지 않는다.

## 로그인 확인

```http
POST /usr/cmn/login/LoginConfirm.do
Content-Type: application/json;charset=utf-8
Accept: application/json, text/plain, */*
Cookie: <LoginSecurity.do에서 받은 KLAS 세션 쿠키>
```

요청 body:

```json
{
  "loginToken": "<rsa_pkcs1_base64>",
  "redirectUrl": "",
  "redirectTabUrl": ""
}
```

성공 응답 예:

```json
{
  "redirectUrl": "",
  "fieldErrors": [],
  "responseText": "",
  "response": {
    "frstPwdAt": null,
    "pushToken": null,
    "userId": "<student-or-user-id>"
  },
  "errorCount": 0,
  "redirect": false,
  "loginRequired": false
}
```

성공 조건:

- HTTP status가 `2xx`다.
- `loginRequired === false`다.
- `errorCount === 0`이다.
- `response.userId`가 존재한다.

인증 실패 처리:

- `errorCount > 0`이면 인증 실패로 처리하고 `KlasAuthResult.failure()`를 반환한다.
- `fieldErrors[0].message`가 있어도 외부 응답에는 그대로 노출하지 않는다.
- 학번 없음과 비밀번호 불일치는 같은 인증 실패로 취급한다.

서버/구조 오류 처리:

- HTTP `4xx`, `5xx`, JSON 파싱 실패, HTML 응답, `response.userId` 누락은 KLAS 서버 오류로 처리한다.
- 서버 오류는 `KlasAuthServerUnavailableException`으로 표현한다.

## 이름 조회

로그인 성공 후 같은 세션으로 아래 API를 호출한다.

```http
POST /std/cps/inqire/ToeicInfoStd.do
Content-Type: application/json;charset=utf-8
Accept: application/json, text/plain, */*
Origin: https://klas.kw.ac.kr
Referer: https://klas.kw.ac.kr/std/cps/inqire/StandStdPage.do
Cookie: <LoginConfirm.do 이후 유효한 KLAS 세션 쿠키>
```

요청 body:

```json
{
  "selectYearhakgi": "<year>,<semester>",
  "selectChangeYn": "Y"
}
```

`selectYearhakgi`는 현재 학년도/학기 값을 사용한다. 실제 구현에서 학기 값을 별도로 확정하지 못하면, KLAS에서 쓰는 현재 학기 기본값을 조회하거나 설정값으로 주입한다.

성공 응답 예:

```json
[
  {
    "hakbun": "<student-id>",
    "kname": "<student-name>",
    "codeName1": "<department-name>",
    "dayOpt": "주",
    "graduateDate": "<grade-or-status>",
    "daesangOpt": "대상",
    "passOpt": null,
    "langName": null,
    "langScore": null,
    "langDate": null
  }
]
```

매핑:

- `hakbun` -> `KlasAuthResult.klasId`
- `kname` -> `KlasAuthResult.name`

성공 조건:

- HTTP status가 `2xx`다.
- 응답 body가 배열이다.
- 첫 번째 항목에 비어 있지 않은 `hakbun`과 `kname`이 있다.
- `hakbun`이 로그인 요청의 `klasId`와 일치한다.

실패 처리:

- 배열이 비어 있거나 `hakbun`, `kname`이 없으면 KLAS 응답 구조 변경으로 보고 서버 오류 처리한다.
- `hakbun`이 요청 `klasId`와 다르면 세션 불일치로 보고 서버 오류 처리한다.
- `loginRequired === true` 형태의 공통 실패 응답이 내려오면 세션 만료로 보고 서버 오류 처리한다.

## `KlasAuthResult` 반환 규칙

성공:

```java
KlasAuthResult.success("<hakbun>", "<kname>")
```

실패:

```java
KlasAuthResult.failure()
```

규칙:

- 인증 실패는 `authenticated=false`, `name=null`, `klasId=null`로 표현한다.
- 서버 장애, timeout, 파싱 실패, 세션 불일치는 `KlasAuthServerUnavailableException`으로 표현한다.
- `klasPassword`, `loginToken`, 세션 쿠키는 어떤 반환 객체에도 포함하지 않는다.

## 보안 주의

- 실제 `Cookie`, `JSESSIONID`, `SESSION`, `WMONID`, 학번, 이름, 비밀번호, loginToken은 Git에 커밋하지 않는다.
- 테스트 fixture에는 `<student-id>`, `<student-name>`, `<session-cookie>` 같은 placeholder만 저장한다.
- 로그에는 요청 body 전체를 남기지 않는다.
- 실패 예외 메시지에는 `klasPassword`, `loginToken`, 세션 쿠키를 포함하지 않는다.
