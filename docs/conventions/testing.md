# 테스트

> Rodi가 의도적으로 유지하는 **프로젝트 고유 규칙**이다. 프로젝트 무관 규범은 전역 스킬
> `/android-code-standard`에 있고, 여기 규칙이 그것과 충돌하면 **이 문서가 우선**한다.
> 수치는 적지 않는다 — 재검증 명령으로 대체한다(→ `README.md`). 시점별 조사 수치는 `../audits/`.
>
> **테스트 컨벤션의 정본은 `../TESTING.md`다.** 이 문서는 거기 없는 관용구(코루틴 테스트
> 셋업, Roborazzi 설정 형태 등)만 담는다. 둘이 어긋나면 `../TESTING.md`를 고친다.

## 파일은 `<대상>Test.kt`, 클래스는 파일당 하나, 함수는 영어 백틱 서술형

```kotlin
@Test
fun `loads places when refresh succeeds`() = runTest { ... }
```

**왜**: 조건과 기대 결과가 IDE·CI 리포트에 문장으로 뜬다. 실패했을 때 코드를 열지 않고도
무엇이 깨졌는지 읽힌다. 파일명=클래스명이어야 실패한 테스트에서 파일로 바로 간다.

**정본**: `feature/home/src/test/.../HomeViewModelTest.kt`

**재검증** (파일명 ≠ 클래스명):
```bash
rg -n 'class (\w+Test)\b' -r '$1' -o --no-heading -g '*.kt' . \
| grep -E '/src/(test|androidTest)/' \
| while IFS=: read -r f _ c; do [ "$(basename "$f" .kt)" != "$c" ] && echo "$c ← $f"; done
```

## JVM 단위 테스트는 JUnit5 + Jupiter assertion + MockK

`assertEquals`/`assertTrue`/`assertFalse`/`assertThrows`를 쓴다. AssertK·Truth는 쓰지 않는다.

```kotlin
coEvery { getPlacesUseCase(any(), any(), any()) } returns Result.success(places)
assertEquals(expected, actual)
coVerify(exactly = 1) { getPlacesUseCase(any(), any(), any()) }
```

**왜**: 실행 엔진과 assertion 문법이 섞이면 파일마다 import를 확인해야 하고, JUnit4/5 러너가
공존하면 어느 쪽이 도는지 헷갈린다. 하나로 고정하는 것 자체가 가치다.

**예외**: Robolectric/Roborazzi와 계측 테스트는 JUnit4 러너 생태계에 묶여 있어
`org.junit.Test` + `@RunWith(AndroidJUnit4::class)`를 쓴다.

**재검증** (JUnit4를 쓰는 JVM 테스트 — Roborazzi가 아니면 정리 대상):
```bash
rg -l 'org\.junit\.Test' -g '*.kt' . | grep '/src/test/'
```

## 코루틴 테스트는 `runTest` + 제어 가능한 `TestDispatcher`

```kotlin
private val testDispatcher = StandardTestDispatcher()

@BeforeEach fun setUp() { Dispatchers.setMain(testDispatcher) }
@AfterEach fun tearDown() { Dispatchers.resetMain() }
```

Flow 검증은 Turbine `.test { }`.

**왜**: `Dispatchers.Main`은 JVM 테스트에 없어서 교체하지 않으면 ViewModel 생성부터 터진다.
`resetMain()`을 빠뜨리면 전역 상태가 남아 **다음 테스트가 실행 순서에 따라 랜덤하게 실패**한다 —
단독 실행하면 통과하는데 전체 실행에서만 깨지는 유형이라 원인 찾기가 오래 걸린다.

**정본**: `feature/home/src/test/.../HomeViewModelTest.kt` — 앵커 `Dispatchers.setMain`

**재검증** (setMain 하고 resetMain 안 하는 파일):
```bash
rg -l 'Dispatchers\.setMain' -g '*.kt' . | grep '/src/test/' \
| while read -r f; do grep -q 'Dispatchers\.resetMain' "$f" || echo "$f"; done
```
> `-L`은 ripgrep에서 `--files-without-match`가 **아니라** `--follow`(심볼릭 링크)다. 짧다고
> `-L`을 쓰면 조건이 뒤집혀 정상 파일이 위반으로 잡힌다 — 2026-09-06에 실제로 CI 점검
> 스크립트가 이 실수로 정상 파일을 무더기로 오탐했다. (당시 수치는 `../audits/` 참고)

## Roborazzi는 JVM 스크린샷 테스트, 스냅샷 경로는 클래스별로

```kotlin
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "...")
class RodiButtonRoborazziTest
```

```kotlin
captureRoboImage("RodiButtonRoborazziTest/enabled.png")
```

**왜**: SDK와 qualifier를 고정하지 않으면 로컬과 CI의 렌더 결과가 달라 스냅샷이 매번 깨진다.
경로를 클래스별로 나누면 어느 컴포넌트의 기준 이미지인지 파일 트리에서 바로 보인다.

**정본**: `core/ui/src/test/.../components/RodiButtonRoborazziTest.kt`
