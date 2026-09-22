# 0002. 로그인 세션 종료는 data 계층에서 한 번에 commit하고 root가 관찰한다

## 상태

채택 (2026-09-22)

## 배경

#166은 재발급 결과를 세션 소유권과 대조해 요청·credential 수준의 경합을 막았다. 그 뒤 세션 종료
workflow 전체를 다시 추적해 다음을 확인했다.

- `MemberRepositoryImpl.withdraw/hardDelete`가 소유권 확인 없이 세션 변경 락 밖에서
  `tokenStore.clear()`를 불렀다. 서버 응답 전에 새 로그인이 끝나면 새 세션을 지웠다(제어된
  interleaving 테스트로 재현).
- 로그아웃·탈퇴 후 온보딩/entry 정리는 UseCase가 repository 호출 **뒤에** 락 없이 수행했다.
  로컬 토큰은 이미 지워졌는데 이 정리가 실패하면 `Result.failure`가 되어 화면이 오류만 띄우고
  로그인 화면으로 가지 않았다.
- Root의 로그인 전환은 화면 ViewModel의 일회성 Effect(`SessionEnded`, `HardDeleteCompleted`)가
  콜백 체인을 거쳐 `RodiAppViewModel.onSessionEnded()`에 도달해야 일어났다. 계정 설정은 확인 직후
  다이얼로그를 닫으므로, 요청 중 뒤로 가면 Effect 수집자가 사라지고 VM은 계속 실행되어 세션은
  지워졌는데 인증 화면에 남을 수 있었다.
- 사용자가 직접 로그아웃해도 root는 항상 "로그인 정보가 만료되어…" 안내를 띄웠다. 종료 이유가
  하나의 `NavigateToLogin`으로 뭉개져 있었다.

## 결정

`core:data`의 `AuthSessionCoordinator`가 세션의 로컬 시작·재발급 반영·종료를 한 락 아래에서 commit한다.
`AuthRepositoryImpl`과 `MemberRepositoryImpl`은 네트워크 요청을 락 밖에서 끝낸 뒤 이 객체를 부른다.

- 종료는 요청 시작 시 캡처한 세션이 아직 현재 세션일 때만 적용한다. 바뀌었으면 새 세션을 건드리지
  않고 `NotAuthenticated`로 실패한다(무시를 성공으로 보고하지 않는다).
- 사용자 종료(로그아웃·탈퇴·삭제)는 토큰 → 연습 세션 → 코스 등록 → 캐시 → 온보딩 → entry 순서로
  **모두 정리한 뒤** 한 번 알린다. 이 구간은 `NonCancellable`이라 화면이 떠나며 호출이 취소돼도
  끝까지 수행하고, 취소는 그다음 호출자에게 그대로 전파된다.
- 토큰 영구 삭제가 실패해도 메모리 세션은 이미 비워지므로 종료로 본다. 부가 저장소 정리 실패는
  종료를 막지 않고 `HardDeleteResult.localCleanupSucceeded` 같은 결과로 남긴다.
- 서버 거부로 인한 만료는 온보딩/entry를 지우지 않는다(같은 사용자의 재로그인이 일반적이다).

Root(`RodiAppViewModel`)는 `observeSessionExpiration()`(보관되는 상태)과 `observeSignOut()`(보관하지
않는 알림)을 관찰해 로그인 화면으로 전환하고, 만료일 때만 만료 안내를 띄운다. 화면은 세션 종료를
root에 전달하지 않는다.

통합 `AuthSession` 상태 SSOT는 도입하지 않았다. 세션 존재 여부의 원천은 이미 `AuthTokenStore`이고,
root는 앱 시작 시 저장된 토큰으로 판단한다. 상태를 하나 더 만들면 원천이 둘이 된다.

## 결과

- 로그아웃·탈퇴·삭제와 새 로그인이 같은 락을 쓰므로 종료 정리가 끝날 때까지 새 로그인의 로컬
  저장이 기다린다. 락 안에는 로컬 I/O만 있어 대기 시간은 DataStore 쓰기 수준이다.
- 사용자 종료 알림은 보관하지 않는다. 알림 시점에 root VM이 없으면(Activity 종료 중) 잃지만,
  다음 실행은 저장된 토큰으로 다시 판단한다. 만료는 기존대로 새 로그인 전까지 보관된다 — 두 신호의
  보관 정책이 다른 이유는 만료가 화면 없이 백그라운드 요청에서도 생기기 때문이다.
- 토큰 영구 삭제 실패 후 앱을 다시 열면 디스크의 이전 토큰으로 인증 상태가 되고, 서버가 폐기한
  refresh token 때문에 만료 경로로 로그인 화면에 간다.
- 로그아웃 온보딩/entry 정리가 UseCase에서 data 계층으로 옮겨 가 `ClearOnboardingDataUseCase`를
  삭제했다. 이 정리를 바꾸려면 `AuthSessionCoordinator`를 봐야 한다.

## 무효화하는 규범 항목

- [x] `docs/conventions/error-handling.md` "인증 재발급의 현재 경계" — #166 이후 후처리 원자성은
  미확인이라고 적혀 있었다 → 세션 종료 commit 경계와 root 관찰을 반영했다.
- [x] `docs/BACKLOG.md` "인증 workflow 후처리 ownership 추가 검토" → 완료로 옮기고 남은 위험만 적었다.
- [x] `docs/PROJECT.md`, `docs/TESTING.md`, `docs/ARCHITECTURE_TARGET.md`,
  `.github/scripts/check-conventions.sh` — 확인했고 이 결정과 충돌하는 서술은 없다.

## 하지 않은 것

- 요청 생성 시점의 세션 고정. 이전 세션 workflow가 새 로그인 뒤 **처음** 요청을 보내면 새 credential이
  붙을 수 있다. 현재 세션 종료 workflow는 모두 화면 VM 범위라 root가 로그인 화면으로 전환하면서
  취소되고, 새 로그인은 그 뒤 사용자 조작이 필요해 재현 경로를 찾지 못했다.
- Root back stack의 상태 기반 재구성. 전환은 여전히 root Effect 한 곳에서 수행한다.
- 이전 계정 온보딩 초안이 정리 실패로 남았을 때 다음 로그인에서 보내지지 않게 하는 방어.
