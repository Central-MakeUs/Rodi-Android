# 2026-09-06 코드 관용구 조사

> **이 문서는 현재 사실이 아니다.** 아래 시점의 스냅샷이다.
> 현재 값이 궁금하면 이 숫자를 인용하지 말고 각 항목의 **측정 명령을 다시 돌린다.**
> 이 파일은 갱신하지 않는다 — 다시 조사할 일이 생기면 새 날짜로 새 파일을 만든다.

| | |
|---|---|
| 조사일 | 2026-09-06 |
| 커밋 | `5d49d179` (`5d49d17908d4324e59365074844c4ec4cd780c3d`) |
| 범위 | `app`, `core`, `feature`의 main·test 소스 (`.kt` 511개) |
| 제외 | `build/`(gitignore), `app/.../spike`, `SampleCourses`, `CourseRepositoryImpl.getCourses()` 등 PoC·죽은 코드 |
| 도구 | ripgrep 15.2.0 (로컬) / 14.1.0 (CI). 명령은 두 버전에서 같은 값을 냄 |
| 측정자 | Claude (Codex 1차 조사 결과를 전수 재측정) |

## 재현 방법

모든 명령은 리포 루트에서 실행한다. `build/`는 `.gitignore`에 있어 rg가 알아서 건너뛴다.
**경로 글롭(`--glob '**/*.kt'`)은 쓰지 않는다** — ripgrep 14와 15에서 결과가 다르다.
베이스네임 글롭(`-g '*.kt'`)과 명시 경로(`.`)를 쓴다.

## 결과

### MVI 계약

| 항목 | 값 | 측정 명령 |
|---|---|---|
| 상태 프로퍼티 `_state` | 8 | `rg -l 'private val _state\b' -g '*ViewModel.kt' .` |
| 상태 프로퍼티 `_uiState` | 9 | `rg -l 'private val _uiState\b' -g '*ViewModel.kt' .` |
| Effect를 `Channel(BUFFERED)`로 전달 | 8 ViewModel | `rg -l 'Channel<' -g '*ViewModel.kt' .` |
| Effect를 `MutableSharedFlow`로 전달 | 1 (`CourseRegistrationViewModel`) | `rg -l 'MutableSharedFlow' -g '*ViewModel.kt' .` |
| `CollectEffect`로 소비하는 화면 | 7 | `rg -l 'CollectEffect' -g '*.kt' .` |
| Effect를 복수형(`effects`)으로 노출 | 1 (`AccountSettingsViewModel`) | `rg -n 'val effects\b' -g '*ViewModel.kt' .` |
| 루트 `*Contract.kt` | 8 | `rg --files -g '*Contract.kt' .` |
| UiState를 ViewModel 파일에 내장 | 9 | `rg -l 'data class \w+UiState' -g '*ViewModel.kt' .` |

### 이름·배치

| 항목 | 값 | 측정 명령 |
|---|---|---|
| ViewModel 선언명 ≠ 파일명 | 2 | `conventions/naming.md` #2 |
| `UseCase` 접미사 없는 usecase 패키지 파일 | 3 (정책·추적기 클래스, 정상) | `conventions/naming.md` #3 |
| `RepositoryImpl` 아닌 repository 파일 | 2 (`CachedPlaceRepository`, `PlaceSampleNormalizer`) | `conventions/naming.md` #4 |
| `component`(단수) 패키지 | 1 (`feature/entry`) | `find app core feature -type d -name component -not -path '*/build/*'` |
| 테스트 파일명 ≠ 클래스명 | 1 파일에 2 클래스 (`HomeSheetAnchorsTest.kt`) | `conventions/testing.md` #1 |

### 에러 처리

| 항목 | 값 | 측정 명령 |
|---|---|---|
| `runSuspendCatching`을 UseCase 밖에서 사용 | 0 | `conventions/error-handling.md` #1 |
| 취소 재전파가 catch 첫 문장이 아님 | 1 (`ReviewWriteViewModel`) | `conventions/error-handling.md` #2 |
| `authenticatedRequest` 헬퍼 중복 | 6 RepositoryImpl | `rg -l 'authenticatedRequest' -g '*.kt' .` |
| 공통 `userMessage()` 호출 ViewModel | 3 | `rg -l '\.userMessage\(\)' -g '*ViewModel.kt' .` |
| 예외 원문(`.message`)을 쓰는 ViewModel | 12 파일 | `rg -l -g '*ViewModel.kt' '\.message\b' . \| grep -v '/src/test/'` |
| 그중 `error`/`it`/`e`/`throwable`의 `.message` | 9 파일 | 같은 명령에 `'(error\|it\|e\|throwable)\.message\b'` |
| `UserMessageProvider` 구현 예외 | 4 (`Place`/`Review`/`Practice`/`Auth`) | `rg -l 'UserMessageProvider' -g '*.kt' .` |

### 테마·Compose

| 항목 | 값 | 측정 명령 |
|---|---|---|
| Material 아이콘(`Icons.`) | 0 | `rg -n 'Icons\.' -g '*.kt' .` |
| `MaterialTheme.colorScheme` 직접 참조 | 0 | `rg -n 'MaterialTheme\.colorScheme' -g '*.kt' .` |
| 하드코딩 색(`Color(0xFF`, 테마 밖) | 4 | `rg -n 'Color\(0xFF' -g '*.kt' . \| grep -v '/theme/'` |
| `collectAsState()`(lifecycle 미인식) | 0 | `rg -n 'collectAsState\(\)' -g '*.kt' .` |
| key 없는 동적 `items` | 0 (고정 개수 skeleton 5건만) | `conventions/compose.md` #2 |

### 테스트·빌드

| 항목 | 값 | 측정 명령 |
|---|---|---|
| `Dispatchers.setMain` 후 `resetMain` 누락 | 0 | `conventions/testing.md` #3 |
| JUnit4를 쓰는 JVM 테스트 | 9 파일 (Roborazzi 5 + `app` 4) | `conventions/testing.md` #2 |
| `useJUnitPlatform()` 수동 선언 모듈 | 1 (`core:data`) | `rg -ln 'useJUnitPlatform' -g 'build.gradle.kts' .` |
| 모듈에서 Compose BOM 재선언 | 2 (`app`만) | `conventions/gradle.md` #1 |
| `app`의 convention plugin 사용 | `hilt`만. application convention 미사용 | `sed -n '1,12p' app/build.gradle.kts` |
| `safeApiCall`/`NetworkResult`/`DataError` 외부 참조 | 0 (죽은 코드) | `rg -l 'safeApiCall\|NetworkResult\|DataError' -g '*.kt' . \| grep -v 'source/remote/network/'` |

## 1차 조사와 어긋난 항목

Codex의 1차 조사 결과를 재측정했더니 **4건이 이미 틀렸다.** 조사가 끝난 지 며칠 만이다.
이것이 조사 수치를 규범 문서에 넣지 않고 이 스냅샷에만 두는 이유다.

| 항목 | 1차 조사 | 재측정 |
|---|---|---|
| `_state` : `_uiState` | 10 : 9 | **8 : 9** |
| `userMessage()` 공통 : 직접 | 4 : 10 | **3 : 9** |
| 비-Roborazzi JUnit4 테스트 | 12 파일 | **4 파일** (전부 `app`) |
| `app`의 convention plugin | 미사용 | **`hilt`는 이미 사용 중** |

또한 직전 PR(#117)이 feature 의존성 구조를 바꾸면서 1차 조사의 Gradle 절 전체를 무효화했다.
그 사건이 ADR 템플릿에 "무효화하는 규범 항목" 칸을 만든 계기다 → `../adr/TEMPLATE.md`

## 이 조사에서 나온 후속 작업

내부 불일치와 결함은 여기 두지 않고 `../BACKLOG.md`의 "코드 관용구 정합성" 절에 있다.
이 문서는 **관찰**이고 그쪽이 **할 일**이다.
