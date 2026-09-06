# 이름

> Rodi가 의도적으로 유지하는 **프로젝트 고유 규칙**이다. 프로젝트 무관 규범은 전역 스킬
> `/android-code-standard`에 있고, 여기 규칙이 그것과 충돌하면 **이 문서가 우선**한다.
> 수치는 적지 않는다 — 재검증 명령으로 대체한다(→ `README.md`). 시점별 조사 수치는 `../audits/`.
> 모든 `rg` 명령은 리포 루트에서 실행한다. `build/`는 `.gitignore`에 있어 rg가 알아서 건너뛴다.

## Composable은 UpperCamelCase, 상태 생성기만 예외

화면·컴포넌트는 UpperCamelCase. `remember*` 상태 생성기와 `Defaults` 내부 private 보조 함수만
일반 함수처럼 lowerCamelCase를 쓴다.

**왜**: 함수명만으로 "이게 화면에 그려지는 것"인지 "값을 만드는 것"인지 구분된다. 예외를
상태 생성기로 좁혀두면 lowerCamelCase가 보일 때 역할이 자동으로 좁혀진다.

**정본**: `feature/home/.../home/HomeScreen.kt` — 앵커 `fun HomeScreen(`

**재검증** (소문자로 시작하는 Composable 목록):
```bash
rg -B1 '^\s*(private |internal |public )?fun [a-z]' -g '*.kt' . | rg -A1 '@Composable'
```

## ViewModel은 `<Feature>ViewModel`, 같은 이름 파일에 단독으로

**왜**: 화면 상태 소유자를 파일 탐색만으로 찾을 수 있다. 다른 파일에 얹으면 그 화면의 상태가
어디서 관리되는지 코드를 읽어야만 알 수 있다.

**정본**: `feature/home/.../home/HomeViewModel.kt` — 앵커 `class HomeViewModel @Inject constructor`

**Rodi 예외 2건**은 규범이 아니라 수정 대상이다 → `../BACKLOG.md`.

**재검증** (선언명 ≠ 파일명):
```bash
rg -n 'class (\w+ViewModel)\b' -r '$1' -o --no-heading -g '*.kt' . \
| grep -v -E '/src/(test|androidTest)/' \
| while IFS=: read -r f _ vm; do [ "$(basename "$f" .kt)" != "$vm" ] && echo "$vm ← $f"; done
```

## UseCase는 `<Verb><Object>UseCase`

`Get`, `Save`, `Observe`, `Delete`, `Update`, `Register`, `Search`, `Start` 같은 동사로 시작한다.

**왜**: 이름만으로 수행 동작과 대상을 알 수 있어, 기능별 패키지 안에서도 역할이 흐려지지 않는다.

**정본**: `core/domain/.../usecase/course/GetRouteUseCase.kt` — 앵커 `class GetRouteUseCase`

`usecase/` 패키지에 있어도 **한 번 호출하고 끝나는 동작이 아니면** `UseCase`를 붙이지 않는다 —
Rodi의 `RadiusArrivalPolicy`(판정 규칙), `RouteProgressTracker`·`DrivingProgressAccumulator`
(상태를 들고 누적하는 객체)가 그 예다. 이름이 역할과 어긋나는 게 더 나쁘다.

**재검증** (접미사 누락 — 위 3건은 정상 결과):
```bash
rg --files -g '*.kt' core/domain/src/main | grep '/usecase/' | grep -v 'UseCase\.kt$'
```

## Repository 인터페이스는 `<Noun>Repository`, 일반 구현은 `<Noun>RepositoryImpl`

데코레이터처럼 별도 의미가 있는 구현은 역할을 드러내는 이름을 쓴다 — Rodi에는
`CachedPlaceRepository`가 그 예다.

**왜**: 인터페이스와 기본 구현을 일관되게 연결하면서, 이름이 다른 구현은 "뭔가 다르다"는
신호가 된다. 전부 `Impl`로 통일하면 캐시 데코레이터가 기본 구현으로 오해된다.

**정본**: `core/domain/.../repository/PlaceRepository.kt`,
`core/data/.../repository/PlaceRepositoryImpl.kt`

**재검증**:
```bash
rg --files -g '*.kt' core/data/src/main | grep '/repository/' | grep -v 'RepositoryImpl\.kt$'
```
> `CachedPlaceRepository`는 위에 적은 데코레이터 예외라 정상 결과다.

## 동작 콜백은 `onXxx`, Composable 슬롯은 명사

`onBack`, `onClick`, `onRetry`, `onDismiss`, `onSelect`. 반면 콘텐츠 슬롯은 `titleContent`,
`trailing`처럼 명사로 둔다.

**왜**: 시그니처만 보고 "외부에서 주입된 동작"과 "그려질 내용"이 구분된다.

**정본**: `feature/mypage/.../MyPageScreen.kt` — 앵커 `onSettingsClick: () -> Unit`

**주의**: 이건 **UI 콜백 파라미터** 규칙이고 MVI `Intent` 자식 타입 이름과는 다르다.
Intent는 `Retry`/`Submit` 같은 동작형을 쓴다(→ `mvi.md`).

## Boolean 상태는 질문으로 읽히게

`is*`가 기본, 존재 여부는 `has*`. 도메인 사건을 직접 표현하는 게 더 정확하면
`tutorialCompleted`처럼 쓴다. `should*`는 Rodi에 없다.

**왜**: 조건문이 문장으로 읽히고, 로딩 여부와 값 존재 여부가 이름에서 갈린다.

**정본**: `feature/home/.../home/HomeContract.kt` — 앵커 `val isNextPageLoading`, `val hasNextPage`

## UI 상태 컬렉션은 읽기 전용 타입 + 빈 컬렉션 기본값

`List`/`Set`/`Map`을 쓰고 `emptyList()`/`emptySet()`/`emptyMap()`으로 초기화한다.
nullable 컬렉션도 mutable 컬렉션도 쓰지 않는다.

**왜**: "아직 없음"을 null로 표현하면 화면이 null 분기를 지게 된다. 빈 컬렉션이면 렌더 코드가
그대로 동작한다. mutable을 노출하면 상태 소유권이 ViewModel 밖으로 샌다.

**정본**: `feature/home/.../home/search/SearchViewModel.kt` —
앵커 `val recentSearches: List<RecentSearch> = emptyList()`

**재검증** (UiState의 mutable/nullable 컬렉션):
```bash
rg -n 'val \w+: (Mutable(List|Set|Map)|(List|Set|Map)<[^>]*>\?)' \
  -g '*Contract.kt' -g '*ViewModel.kt' .
```
