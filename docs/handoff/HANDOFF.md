# HANDOFF — 계정 API 및 세션 보안 강화

Status: IMPL_DONE
Branch: feat/account-api

## Context

UI가 없는 상태에서도 탈퇴, 토큰 재발급, 계정 복구, 로그아웃 API를 domain/data 경계까지 제공한다.
refresh token 회전 중 재사용 탐지로 전체 세션이 폐기될 수 있으므로 저장과 재발급 동시성을 함께 보강한다.

## Spec

- AuthRepository: 재발급, 카카오 복구, 로그아웃 / MemberRepository: 탈퇴 계약 추가
- 네트워크 UseCase는 `runSuspendCatching`으로 `Result` 반환, 취소 예외는 전파
- refresh token은 AuthTokenStore 내부에서만 사용하고 Mutex와 요청 시점 토큰 비교로 중복 제출 방지
- 복구 응답은 성공과 탈퇴 유예 상태를 sealed domain 모델로 구분
- 탈퇴는 access token Bearer 헤더를 사용하며, 자동 재발급 Authenticator는 범위 밖
- 토큰 저장은 Android Keystore AES-GCM과 암호문 전용 DataStore로 분리하고, AAD·원자적 snapshot·안전한 손상 복구를 적용
- 이전 EncryptedSharedPreferences 세션은 deprecated API를 유지하지 않도록 첫 실행에 삭제하며 재로그인 요구
- HTTP debug 로그는 BODY를 사용하지 않아 credential/refresh token 본문을 기록하지 않음

## Files

- `core/domain`: auth/member repository 계약, account restore domain model, auth/member usecase, usecase tests
- `core/data`: auth/member API, DTO, mapper, repository, DI, token security store, repository/mapper tests
- `app/src/main/res/xml`: token store 백업/기기 이전 제외 규칙
- `docs/PROJECT.md`, `docs/handoff/HANDOFF.md`: 구현 결과와 프로젝트 모듈 맵 갱신

## Acceptance

- 탈퇴, 재발급, 복구, 로그아웃 API가 UI 없이 domain/data 경계에서 호출 가능하다.
- refresh token은 호출자에게 노출하지 않고 보안 저장소 내부 snapshot으로만 사용한다.
- 재발급은 동일 refresh token 동시 제출을 막고, 서버 재사용 탐지 시 로컬 세션을 폐기한다.
- 복구 응답은 `Restored`와 `WithdrawalPending` domain 모델로 구분하고, 성공 시에만 토큰을 저장한다.
- 손상되거나 일부만 남은 세션, Keystore 키 불일치, 저장 실패는 안전하게 미인증 상태로 수렴한다.
- debug HTTP 로그에는 credential, refresh token 본문이 남지 않는다.

## Verification

- `./gradlew :core:domain:test`
- `./gradlew :core:data:test`
- `./gradlew assembleDebug`

## Out of scope

- UI 연동
- 자동 토큰 갱신 Authenticator
- 원격 브랜치 삭제
- 생체인증 필수 Keystore 키 적용

## Codex Result

- Changed files: `core/domain` auth/member model·repository·usecase, `core/data` auth/member API·DTO·mapper·repository·DI·security store, `core/data`/`core/domain` tests, `docs/PROJECT.md`, `docs/handoff/HANDOFF.md`
- Build/test: `./gradlew :core:domain:test :core:data:test` GREEN; `./gradlew assembleDebug` GREEN
- Open questions: 이전 앱 버전에서 로그인된 사용자는 첫 실행 후 재로그인이 필요하다. deprecated Security Crypto API와 의존성은 제거됐다.

## Claude Review

- Blocking: none
- Nits: PR #35 CodeRabbit/GitHub review에서 지적한 세션 저장소 I/O 처리, Keystore 키 관리, 재발급 세션 폐기 처리, mapper/usecase/repository 테스트 보강 항목을 follow-up commit에서 반영했다.
- Verdict: APPROVE
