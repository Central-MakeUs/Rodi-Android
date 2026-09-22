# 에러 처리

> Rodi가 의도적으로 유지하는 **프로젝트 고유 규칙**이다. 프로젝트 무관 규범은 전역 스킬
> `/android-code-standard`에 있고, 여기 규칙이 그것과 충돌하면 **이 문서가 우선**한다.
> 수치는 적지 않는다 — 재검증 명령으로 대체한다(→ `README.md`). 시점별 조사 수치는 `../audits/`.

## 계층별 책임

| 계층 | 하는 일 |
|---|---|
| Data(Repository) | 전송 예외·API 응답 계약 → **도메인 예외**로 변환 |
| Domain(UseCase) | 도메인 예외 → `Result`로 감싸기 (`runSuspendCatching`) |
| Presentation(ViewModel) | `Result` 실패 → **사용자 문구**로 변환 (`userMessage()`) |

각 변환이 정확히 한 계층에서만 일어난다. 두 계층에서 하면 같은 예외가 두 번 감싸이거나,
어느 쪽이 최종 문구를 정하는지 알 수 없게 된다.

## `Result`를 반환하는 UseCase는 `runSuspendCatching`으로 감싼다

Repository와 ViewModel에서는 쓰지 않는다.

**왜**: 실패를 `Result`로 바꾸는 지점을 한 계층에 고정한다. 그리고 이 헬퍼가 취소 재전파를
품고 있어서, 각 UseCase가 `CancellationException` 처리를 기억하지 않아도 된다.

**정본**: `core/common/.../RunSuspendCatching.kt`,
`core/domain/.../usecase/place/GetPlacesUseCase.kt` — 앵커 `runSuspendCatching {`

**재검증** (UseCase 밖에서의 사용):
```bash
rg -ln 'runSuspendCatching' -g '*.kt' . | grep -v -E 'UseCase|RunSuspendCatching'
```

## `CancellationException`은 포괄 catch보다 **먼저** 같은 인스턴스로 재전파

```kotlin
catch (e: CancellationException) {
    throw e
} catch (e: Throwable) {
    Result.failure(e)
}
```

**왜**: 코루틴 취소는 실패가 아니라 정상적인 흐름 종료다. `catch (Throwable)`로 삼키면 이미
취소된 스코프에서 후속 코드가 계속 돌고, 상태가 "빈 결과"로 덮인다. Rodi에서 실제로 취소가
`emptySet()`으로 바뀌어 저장된 적이 있다(`dd545485`에서 수정). 초안 삭제와 인증 세션 정리에서도
같은 문제가 반복 검토됐다. **취소를 실패 값이나 기본값으로 변환하지 않는다.**

새 인스턴스를 만들어 던지지 말고 받은 것을 그대로 던진다 — 구조적 동시성이 취소 원인을
추적하는 데 그 인스턴스를 쓴다.

**정본**: `core/common/.../RunSuspendCatching.kt`,
`core/data/.../repository/PracticeRepositoryImpl.kt`

**재검증** (재전파가 catch의 **첫 문장이 아닌** 곳 — 그 사이에 뭘 하는지 봐야 한다):
```bash
rg -U -P -n 'catch \([^)]*CancellationException\)\s*\{\s*\n(?!\s*throw)' -g '*.kt' . \
| grep -v -E '/src/(test|androidTest)/'
```
> "재전파가 아예 없는 곳"이 아니라 "재전파 전에 뭔가 하는 곳"을 찾는다. 버그는 대개 후자에
> 있다 — Rodi의 `ReviewWriteViewModel`은 `throw`는 하지만 그 전에 취소를 실패로 간주해
> 사용자용 에러 문구를 상태에 쓴다(2026-09-06 이 명령으로 발견). 테스트 코드는 취소를 잡아
> 단언하는 게 정상이라 제외한다.

## API 응답 계약과 전송 예외는 Repository에서 도메인 결과로 변환

`ApiEnvelope`의 성공 여부와 필수 데이터를 Repository 안에서 `requireData()`/`requireSuccess()`로
검증하고, 실패는 도메인 예외로 바꿔 던진다.

**왜**: Retrofit 타입, HTTP 상태 코드, 서버 응답 코드가 Presentation까지 새어 나가면
ViewModel이 네트워크 세부사항을 알게 되고, 서버 스펙이 바뀔 때 화면까지 고쳐야 한다.

**정본**: `core/data/.../source/remote/network/ApiEnvelope.kt`,
`core/data/.../repository/PlaceRepositoryImpl.kt` — 앵커 `requireData()`

## 사용자 문구는 승인된 도메인 예외만 노출한다

`UserMessageProvider`를 구현한 예외만 고유 문구를 갖고, 나머지는 공통 `Throwable.userMessage()`가
안전한 기본 문구로 덮는다.

```kotlin
fun Throwable.userMessage(): String =
    (this as? UserMessageProvider)?.userMessage ?: "요청을 처리하지 못했어요..."
```

**왜**: `error.message`를 그대로 쓰면 서버 JSON, 직렬화 예외 원문, 스택 정보가 사용자 화면에
뜬다. Rodi에서 실제로 `Field 'totalCount' is required for type with serial name ...`가 스낵바에
그대로 노출됐다(2026-09-01 수정). **화면 단에서 문구를 고르는 게 아니라, 노출해도 되는
예외만 스스로 문구를 갖게 하는 구조**여야 한 곳만 지키면 된다.

**정본**: `core/common/.../UserMessage.kt` — 앵커 `UserMessageProvider`

**재검증** (예외 원문을 화면에 그대로 쓰는 ViewModel):
```bash
rg -n '\.message\b' -g '*ViewModel.kt' . | grep -v '/src/test/'
```
> 검색 결과가 있으면 실제 예외 원문인지 다른 모델의 message 필드인지 확인한다.
> 과거 조사 수치는 현재 위반 수가 아니며 legacy를 따라 쓰지 않는다 → `../BACKLOG.md`.

## 인증 재발급의 현재 경계

현재 `NetworkModule`은 인증 client에 `AuthHeaderInterceptor`와 `TokenAuthenticator`를 연결한다.
공개 Auth API와 Kakao client는 분리되어 있다. 기존 `authenticatedRequest` 이름의 helper는
일부 Repository에 남아 로그인 확인·도메인 예외 mapping을 수행하지만 401 refresh/retry를 직접
소유하지 않는다. 이름 검색 건수를 중복 재발급 구현 수로 해석하지 않는다. 새 보호 API에 이전
재발급 로직을 복사하지 않는다. single-flight refresh와 session-bound commit/retry 방어는
`AuthRepositoryImpl`, `AuthTokenStore`, `TokenAuthenticator`의 계약·테스트로 확인한다.

#166은 새 login/logout 뒤 늦은 refresh 결과와 이전 요청의 새 세션 credential retry를 방어했다.
이는 모든 login/profile/onboarding 후처리의 원자성이나 process 복원을 입증하지 않는다.
2026-09-17 auth audit은 중앙화 이전 snapshot으로 보존한다.
