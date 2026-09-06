# MVI 계약

> Rodi가 의도적으로 유지하는 **프로젝트 고유 규칙**이다. 프로젝트 무관 규범은 전역 스킬
> `/android-code-standard`에 있고, 여기 규칙이 그것과 충돌하면 **이 문서가 우선**한다.
> 수치는 적지 않는다 — 재검증 명령으로 대체한다(→ `README.md`). 시점별 조사 수치는 `../audits/`.

## 일반 화면 상태는 불변 `data class *UiState`

모든 필드를 `val`로 둔 단일 `data class`가 기본이다. **동시에 존재할 수 없는 화면 모드**일
때만 `sealed interface`를 쓴다 — Rodi에서는 `LoginUiState`(`Idle`/`LoggingIn`/
`RecoveryRequired`) 하나뿐이다.

**왜**: 대부분의 화면은 "로딩 중이면서 목록도 있고 에러 배너도 떠 있는" 조합 상태를 가진다.
sealed로 쪼개면 조합마다 타입이 폭발한다. 반대로 진짜 배타적인 모드를 data class로 두면
불가능한 조합(`isLoggingIn && isRecoveryRequired`)이 표현 가능해져 버그가 들어온다.

**정본**: `feature/home/.../home/HomeContract.kt`(data class),
`feature/auth/.../LoginContract.kt`(sealed) — 앵커 `sealed interface LoginUiState`

## Intent와 Effect는 `sealed interface`, payload 유무로 `data object`/`data class`

**왜**: 입력과 일회성 출력을 닫힌 집합으로 만들면 `when`이 전수 분기를 강제한다. payload 없는
것을 `data object`로 두면 인스턴스가 하나뿐임이 타입에 드러난다.

**정본**: `feature/home/.../home/HomeContract.kt` — 앵커 `sealed interface HomeEffect`

**Intent 자식 이름은 동작형**(`Retry`, `Submit`, `SelectWaypoint`)을 쓴다. Contract 타입 자체가
이미 "사용자 입력"을 뜻하므로 `On` 접두사는 정보를 더하지 않고, UI 콜백 파라미터의 `onXxx`와
이름이 겹쳐 헷갈린다. Rodi에 `OnXxx`형이 남아 있는 건 수정 대상이다 → `../BACKLOG.md`.

## Contract는 보조 타입 → UiState → Intent → Effect 순서

화면 전용 enum이나 작은 보조 모델은 UiState 앞에 둔다.

**왜**: 화면 데이터 → 사용자 입력 → 일회성 출력 순서로 읽힌다. 파일을 위에서 아래로 읽으면
그 화면의 계약이 그 순서로 이해된다.

**정본**: `feature/home/.../home/HomeContract.kt`

**재검증** (Contract가 아니라 ViewModel 파일에 UiState가 들어간 곳):
```bash
rg -l 'data class \w+UiState' -g '*ViewModel.kt' .
```

## 상태는 private Mutable + public read-only 쌍으로 노출

부분 갱신은 `update { it.copy(...) }`, 초기화나 전체 교체는 `.value =`를 쓴다.

**왜**: 상태 변경 권한을 ViewModel 안으로 제한한다. `update`는 읽기-수정-쓰기가 원자적이라
동시 갱신에서 값이 유실되지 않는다 — `.value = _state.value.copy(...)`로 쓰면 그 보장이 없다.

**정본**: `feature/home/.../home/HomeViewModel.kt` —
앵커 `private val _state = MutableStateFlow(HomeUiState())`

**Rodi는 `_state`/`_uiState`로 이름이 갈려 있다** — 통일 대상이다 → `../BACKLOG.md`.
새 코드에는 상태 타입과 맞춰 `_uiState`/`uiState`를 쓴다.

## 일회성 Effect는 buffered Channel + `receiveAsFlow()`

```kotlin
private val _effect = Channel<HomeEffect>(Channel.BUFFERED)
val effect: Flow<HomeEffect> = _effect.receiveAsFlow()
```

**왜**: Effect는 "스낵바를 띄워라", "이 화면으로 가라" 같은 **명령**이고, 소비자는 화면 하나다.
Channel은 각 원소를 **한 소비자에게만** 전달하고, 소비자가 없는 동안 버퍼에 쌓아둔다.

**보장하지 않는 것**: *정확히 한 번 실행*은 아니다. `repeatOnLifecycle` 수집이 원소를 꺼낸 뒤
처리 전에 취소되면 그 Effect는 **유실된다.** 반드시 실행돼야 하는 것(결제 완료 기록 같은)은
Effect로 보내지 말고 상태로 남겨 화면이 다시 읽게 한다. `SharedFlow`(replay=0)를 쓰면 수집자가 없는 순간에
보낸 이벤트가 조용히 사라진다 — 화면 전환 직후나 회전 중에 스낵바가 안 뜨는 형태로 터진다.
재생·다중 소비가 실제로 필요하다면 그때만 SharedFlow를 쓰고 이유를 Contract에 남긴다.

**정본**: `feature/home/.../home/HomeViewModel.kt` — 앵커 `Channel<HomeEffect>(Channel.BUFFERED)`

**재검증** (SharedFlow로 Effect를 내보내는 곳 — 이유가 적혀 있어야 한다):
```bash
rg -n 'MutableSharedFlow' -g '*ViewModel.kt' .
```
> 앵커를 `Channel(Channel.BUFFERED)`로 적으면 0건이 나온다. 실제 코드는
> `Channel<HomeEffect>(...)`라 제네릭이 사이에 낀다. 세는 쪽이 아니라 **어긋난 쪽을 세는**
> 명령이 짧고 덜 틀린다.

## Effect는 lifecycle-aware 공통 수집기로 소비

`CollectEffect`가 `repeatOnLifecycle(STARTED)`로 Flow를 수집한다.

**왜**: 화면이 중지된 동안 네비게이션이나 스낵바를 실행하면 크래시하거나 엉뚱한 화면에 뜬다.
수집 재시작 로직을 화면마다 손으로 쓰면 한 곳은 반드시 빠뜨린다.

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
