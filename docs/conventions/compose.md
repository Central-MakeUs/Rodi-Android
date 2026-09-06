# Compose 작성

> Rodi가 의도적으로 유지하는 **프로젝트 고유 규칙**이다. 프로젝트 무관 규범은 전역 스킬
> `/android-code-standard`에 있고, 여기 규칙이 그것과 충돌하면 **이 문서가 우선**한다.
> 수치는 적지 않는다 — 재검증 명령으로 대체한다(→ `README.md`). 시점별 조사 수치는 `../audits/`.

## Modifier는 필수 상태·콜백 뒤, 선택 옵션 앞. 기본값 `Modifier`

```kotlin
@Composable
fun RodiButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    ...
)
```

**왜**: "Modifier가 항상 첫 번째"라는 통설과 다르다 — Rodi는 필수 입력을 먼저 드러내는 쪽을
택했다. 기본값이 있어야 호출자가 생략할 수 있고, 기본값 없는 Modifier는 private helper에만 둔다.

**정본**: `core/ui/.../components/button/RodiButton.kt` — 앵커 `modifier: Modifier = Modifier`

**재검증** (기본값 없는 Modifier — private helper는 정상이므로 가시성을 함께 볼 것):
```bash
rg -n 'modifier: Modifier[,)]' -g '*.kt' .
```

## `remember`의 key에는 **무효화해야 할 identity**를 넣는다

```kotlin
val mapState = remember(place.id) { ... }
val controller = remember(retryToken) { ... }
```

**왜**: key 없이 두면 다른 장소로 이동해도 이전 장소의 객체가 그대로 남는다. 반대로 매 프레임
바뀌는 값을 key로 넣으면 매번 재생성돼 remember가 무의미해진다. **"이 값이 바뀌면 만들어둔 걸
버려야 하는가"**가 유일한 기준이다.

**정본**: `feature/home/.../detail/CourseDetailSheet.kt` — 앵커 `remember(place.id)`

## `LaunchedEffect`에는 재실행 조건을 나타내는 key를 반드시 명시

단발성 초기 작업에만 `Unit`을 쓴다.

**왜**: key가 Effect의 취소·재시작 시점을 호출부에 드러낸다. `Unit`을 습관적으로 쓰면 상태가
바뀌어도 Effect가 다시 안 돌아 화면이 낡은 데이터에 멈춘다.

**정본**: `feature/mypage/.../registeredcourses/RegisteredCoursesScreen.kt` —
앵커 `LaunchedEffect(uiState.`

## 파생 조건은 `derivedStateOf`, 입력 상태를 key로

```kotlin
val shouldLoadNextPage by remember(listState, hasNextPage) {
    derivedStateOf { ... }
}
```

**왜**: 스크롤 위치는 매 프레임 바뀌지만 "다음 페이지를 불러야 하는가"는 거의 안 바뀐다.
`derivedStateOf` 없이 쓰면 스크롤할 때마다 재구성되고 페이지 요청이 중복된다.

**정본**: `feature/home/.../list/components/PlaceListContent.kt` — 앵커 `derivedStateOf`

## 동적 Lazy 목록에는 안정적인 identity key

개수가 고정된 skeleton만 key를 생략한다.

**왜**: key가 없으면 Compose가 위치로 항목을 식별한다. 목록 중간에 삽입·삭제가 일어나면
항목 상태(애니메이션, 스크롤 내 위치, `remember` 값)가 엉뚱한 데이터에 달라붙는다.

**정본**: `feature/mypage/.../registeredcourses/RegisteredCoursesScreen.kt` —
앵커 `key = { it.courseId }`

**재검증** (key 없는 items — 개수가 고정된 skeleton은 정상 결과다):
```bash
rg -U -P -n 'items(Indexed)?\((?![^)]*key)[^)]*\)\s*\{' -g '*.kt' . \
| grep -v -E '/src/(test|androidTest)/'
```

## WindowInsets는 시스템 UI와 맞닿는 경계에서만 적용

Scaffold나 Sheet의 기본 inset을 끈 경우 Content 루트에서 직접 적용한다.

```kotlin
contentWindowInsets = WindowInsets(0, 0, 0, 0)
```

키보드 위에 고정되는 하단 CTA는 IME와 navigation bar를 합친다.

```kotlin
WindowInsets.ime.union(WindowInsets.navigationBars)
```

**왜**: Scaffold·Sheet·하단 CTA가 각자 inset을 적용하면 여백이 두 번 들어가 화면이 뜬다.
반대로 IME만 보고 navigation bar를 빼먹으면 키보드가 없을 때 CTA가 제스처 바에 깔린다.

**정본**: `feature/home/.../review/ReviewBottomBarInsets.kt` — 앵커 `WindowInsets.ime.union`
