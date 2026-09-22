# 화면 상태와 입력 계약

> Rodi가 의도적으로 유지하는 **프로젝트 고유 규칙**이다. Global 판단 절차는 전역 Skill
> `android-development`, 코드 위생은 `android-code-standard`를 참고한다. API 사실·프로젝트 결정·
> 현재 구현·작업 범위의 authority 구분은 `README.md`를 따른다.
> 수치는 적지 않는다 — 재검증 명령으로 대체한다(→ `README.md`). 시점별 조사 수치는 `../audits/`.

## 화면 패턴 선택

새 화면은 explicit ViewModel methods + UiState로 시작할 수 있다. state-dependent 전이·경쟁 입력·
ordering·invariant를 typed Intent가 더 명확하게 설명하고 검증할 때 MVI를 선택한다.
화면 크기·Intent 수·wizard 여부만으로 승격하지 않는다. onIntent는 직렬화나 race 방어가 아니다.
기존 화면은 이 결정만으로 일괄 재작성하지 않는다. 아래 Intent/Effect 규칙은 해당 타입을 채택했을 때 적용한다.

## 일반 화면 상태는 불변 `data class *UiState`

모든 필드를 `val`로 둔 단일 `data class`가 기본이다. **동시에 존재할 수 없는 화면 모드**일
때만 `sealed interface`를 쓴다 — Rodi에서는 `LoginUiState`(`Idle`/`LoggingIn`/
`RecoveryRequired`) 하나뿐이다.

**왜**: 대부분의 화면은 "로딩 중이면서 목록도 있고 에러 배너도 떠 있는" 조합 상태를 가진다.
sealed로 쪼개면 조합마다 타입이 폭발한다. 반대로 진짜 배타적인 모드를 data class로 두면
불가능한 조합(`isLoggingIn && isRecoveryRequired`)이 표현 가능해져 버그가 들어온다.

**정본**: `feature/home/.../home/HomeContract.kt`(data class),
`feature/auth/.../LoginContract.kt`(sealed) — 앵커 `sealed interface LoginUiState`

## 채택한 Intent와 Effect는 `sealed interface`, payload 유무로 `data object`/`data class`

**왜**: 입력과 일회성 출력을 닫힌 집합으로 만들면 `when`이 전수 분기를 강제한다. payload 없는
것을 `data object`로 두면 인스턴스가 하나뿐임이 타입에 드러난다.

**정본**: `feature/home/.../home/HomeContract.kt` — 앵커 `sealed interface HomeEffect`

**Intent 자식 이름은 이벤트형**이다 — 일어난 일을 과거형으로 적는다.

- 사용자 조작: `대상 + 과거분사` (`PlaceClicked`, `FilterCategorySelected`, `QueryChanged`)
- 시스템·외부 이벤트: `주체 + 일어난 일` (`AppResumed`, `KakaoLoginFailed`, `MapGestured`)
- 금지: `On` 접두사(`OnPlaceClick`), 명령형(`SelectWaypoint`, `Retry`)

**왜**: 화면은 일어난 일만 전하고 무엇을 할지는 ViewModel이 정한다. 명령형으로 적으면 화면이
처리를 지시하는 모양이 되고, 제스처·복귀·실패처럼 "일어난 일"은 명령으로 부를 이름이 없어
매번 고민하게 된다. 이벤트형은 규칙이 기계적이라 새 Intent를 지을 때 흔들리지 않는다.
`On` 접두사는 UI 콜백 파라미터의 `onXxx`와 겹쳐 헷갈리기도 한다.

**정본**: `feature/home/.../home/HomeContract.kt` — 앵커 `data object MapGestured`

**재검증** (CI BLOCK — `On` 접두사):
```bash
rg -n -g '*Contract.kt' '^\s+data (object|class) On[A-Z]' .
```

## Contract는 필요한 타입만 보조 타입 → UiState → Intent → Effect 순서

화면 전용 enum이나 작은 보조 모델은 UiState 앞에 둔다.

**왜**: 화면 데이터 → 사용자 입력 → 일회성 출력 순서로 읽힌다. 파일을 위에서 아래로 읽으면
그 화면의 계약이 그 순서로 이해된다.

**정본**: `feature/home/.../home/HomeContract.kt`

**배치**: 화면마다 ViewModel 옆 `XxxContract.kt` → `structure.md`

## 상태는 private Mutable + public read-only 쌍으로 노출

부분 갱신은 `update { it.copy(...) }`, 초기화나 전체 교체는 `.value =`를 쓴다.

**왜**: 상태 변경 권한을 ViewModel 안으로 제한한다. `update`는 읽기-수정-쓰기가 원자적이라
동시 갱신에서 값이 유실되지 않는다 — `.value = _state.value.copy(...)`로 쓰면 그 보장이 없다.

**정본**: `feature/mypage/.../mypage/MyPageViewModel.kt` —
앵커 `private val _uiState = MutableStateFlow(MyPageUiState())`

이름은 상태 타입과 맞춰 `_uiState`/`uiState`로 쓴다 — 타입이 `*UiState`이므로 property도 같은 말을 쓴다.
CI가 `*ViewModel.kt`의 `_state`·`val state:`를 막는다.

## Output은 의미·수명·소비 요구로 선택

Global `android-development`의 `state-and-events` 판단을 따른다. business 결과는 UiState 표현을
우선 검토하고, UI만 결정하는 이동은 callback/local state를 검토한다. launcher·지도 자원을 쓰는
명령은 executor·유실·중복·result 요구를 정한다. State 전환이나 Effect 제거를 일괄 강제하지 않는다.
UserMessage representation은 아직 전역·Rodi 기본값으로 확정하지 않는다. RodiSnackbarHost의
기존 queue와 VM queue가 같은 책임을 중복 소유하지 않는지 먼저 확인한다.

기존 Rodi Effect는 주로 `Channel<T>(Channel.BUFFERED)` + `receiveAsFlow()`다. 이 형태를
유지할 수 있지만 새로운 output의 필수 기본값은 아니다. 단일 consumer service 명령 queue도
Channel의 유효한 사용이다. SharedFlow는 실제 transient broadcast 요구에 따라 선택한다.
어떤 transport도 UI 처리 완료·정확히 한 번 실행·process 복원을 단독 보장하지 않는다.

**현재 프로젝트 집행**: Effect를 채택하면 노출명은 `effect`, 소비는 아래 `CollectEffect` 규칙을
따른다. ViewModel의 `MutableSharedFlow`는 선언 직전 `// SharedFlow 사용 이유: ...` 주석을
요구하는 기존 CI를 유지한다. 이 주석 정책은 SharedFlow 금지가 아니며 신규 정책을 이유로 CI를 우회하지 않는다.

**State-driven navigation**: owner/entry 수명, collector 재시작, 복귀, 중복 입력·result,
process recreation을 검토한다. 필요할 때 ack/operation identity/terminal state/idempotent 이동을
선택한다. `DrivingGoal`은 `saveSucceeded`와 오류 Effect가 공존하는 사례이며 단순화의 보편 증거가 아니다.

**Map command**: `HomeEffect.MoveToRegion`처럼 준비가 필요한 UI 자원의 명령은 현 Effect를
유지할 수 있다. 로컬 pending의 수명·재진입 동작을 확인한다. 증가 counter와 소비 flag만으로
명령 중복 방지가 해결된다고 가정하지 않는다.

**재검증**: `.github/scripts/check-conventions.sh`의 Effect 노출·수집·SharedFlow 사유 검사를
확인한다. 검사 통과는 delivery/lifecycle contract의 증명이 아니다.

## Effect는 lifecycle-aware 공통 수집기로 소비

`CollectEffect`가 `repeatOnLifecycle(STARTED)`로 Flow를 수집한다.

**왜**: 화면이 중지된 동안 네비게이션이나 스낵바를 실행하면 크래시하거나 엉뚱한 화면에 뜬다.
기존 Effect 소비의 공통 경계를 유지한다. 이 수집기는 exactly-once 보장이 아니며,
State-driven observer까지 Effect 수집기에 맞춰 변환하지 않는다. STARTED/RESUMED 요구가 다른
flow는 해당 lifecycle과 callback 최신성을 따로 검토한다.

**정본**: `core/ui/.../effect/CollectEffect.kt` — 앵커 `repeatOnLifecycle(lifecycleState)`

**재검증** (공통 수집기를 안 쓰고 직접 수집하는 화면):
```bash
rg -ln 'LaunchedEffect' -g '*Screen.kt' . \
| while read -r f; do grep -qE 'effect\.collect|effects\.collect' "$f" && echo "$f"; done
```

## ViewModel Flow는 `collectAsStateWithLifecycle()`로 수집

화면 전용 상태(메뉴 열림, 스크롤 위치처럼 도메인에 영향 없는 것)는 ViewModel이 아니라
Composable이 `remember`로 소유한다.

**왜**: `collectAsState()`는 화면이 백그라운드에 있어도 수집을 계속해 배터리와 네트워크를 쓴다.
그리고 상태 소유권을 나눠야 ViewModel이 UI 세부사항으로 오염되지 않는다.

**정본**: `feature/auth/.../LoginScreen.kt` — 앵커 `collectAsStateWithLifecycle()`

**재검증** (lifecycle 미인식 수집):
```bash
rg -n 'collectAsState\s*\(' -g '*.kt' .
```
