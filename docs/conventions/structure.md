# 소스 루트·패키지 배치

> Rodi가 의도적으로 유지하는 **프로젝트 고유 규칙**이다. Global 판단 절차는 전역 Skill
> `android-development`, 코드 위생은 `android-code-standard`를 참고한다. API 사실·프로젝트 결정·
> 현재 구현·작업 범위의 authority 구분은 `README.md`를 따른다.
> 수치는 적지 않는다 — 재검증 명령으로 대체한다(→ `README.md`). 시점별 조사 수치는 `../audits/`.

## 소스 루트는 모듈 종류로 가른다

- 순수 Kotlin(JVM) 모듈: `src/main/kotlin/`
- Android 모듈: `src/main/java/`

**왜**: 프로젝트 초기에 갈린 채로 굳었고, 지금 통일하면 전 모듈 이동이라 diff만 커지고
얻는 게 없다. **새 모듈은 그 종류의 기존 관례를 따른다.**

**정본**: `core/common/src/main/kotlin/`, `core/ui/src/main/java/`

## 패키지는 파일 수가 만든다 — 임계는 2개

- 같은 역할 파일이 **2개 이상**이면 역할 패키지를 만든다. **1개면** feature 루트에 둔다.
- 예외: 독립 화면이거나 외부 통합 경계(지도·위치 등)면 파일이 하나여도 패키지로 격리한다.

**왜**: 파일 하나짜리 패키지가 늘면 탐색 비용만 커진다. 임계를 숫자로 못 박으면 매번
논쟁하지 않아도 된다. 예외를 "독립 화면 / 기술 경계"로 좁혀두면 남용되지 않는다.

**정본**: `feature/course-registration/.../map/`(외부 통합 경계),
`feature/settings/.../licenses/`(독립 화면)

## 컴포넌트 패키지는 복수형 `components`

**왜**: 다수가 이미 복수형이고, 단수/복수가 섞이면 경로를 외울 수 없다.
`feature/entry`의 `component`(단수) 하나가 어긋나 있다 → `../BACKLOG.md`.

**재검증**:
```bash
find app core feature -type d -name component -not -path '*/build/*'
```

## Contract는 화면(ViewModel)마다 하나, ViewModel 옆에

`XxxViewModel.kt`와 같은 패키지에 `XxxContract.kt`를 두고, 그 화면의 보조 타입·`UiState`와
필요한 `Intent`·`Effect`를 여기에 선언한다. 두 타입은 모든 화면의 필수 구성요소가 아니다.
ViewModel 파일에는 ViewModel과 그 private 헬퍼만 둔다.
ViewModel 없는 화면(상태를 Composable이 소유)은 Contract를 만들지 않는다.

**왜**: 2026-09-17에 "feature 루트에 하나"에서 바꿨다. mypage처럼 화면이 여러 개인 feature를
한 파일로 모으면 서로 무관한 계약 수백 줄이 섞이고, 실제로도 하위 화면들은 이미 화면별 Contract를
쓰고 있었다. 이름 규칙(`Xxx` 공유)만으로 ViewModel·Screen·Contract가 짝지어져 찾기 쉽다.

**정본**: `feature/mypage/.../myposts/MyPostsContract.kt` — 앵커 `data class MyPostsUiState`

**재검증** (CI BLOCK — ViewModel 파일에 계약 타입을 선언한 곳):
```bash
rg -n -g '*ViewModel.kt' '^(data class|sealed interface) \w+(UiState|Intent|Effect)\b' .
```

## 화면은 UseCase를 거쳐 domain에 접근한다

`feature`·`app`의 ViewModel과 Coordinator는 `core.domain.repository.*`를 직접 주입하지 않는다.
단순 위임이라도 UseCase를 둔다. 이는 **Rodi의 strict 정책**이며 Android 전체의 필수 경계가 아니다.
UseCase가 많으면 VM ownership과 하나의 business workflow인지 먼저 검토한다. constructor
개수만 줄이는 wrapper나 Repository 직접 접근으로 우회하지 않는다.

```kotlin
class CourseRegistrationViewModel @Inject constructor(
    private val searchLocations: SearchCourseLocationsUseCase, // CourseLocationRepository 직접 주입 X
)
```

**왜**: `error-handling.md` 계층표에서 UseCase는 실패를 `Result`로 감싸고 ViewModel은 그 실패를
문구로 바꾼다. Repository를 직접 부르면 이 경계가 사라져 ViewModel이 try/catch로 취소 재던지기까지
떠안는다. 또 같은 규칙이 두 벌이 된다 — 코스 등록은 UseCase 11개를 만들어 두고 ViewModel이
Repository를 직접 불러, 초안 저장 분기와 제출 검증이 UseCase·ViewModel·RepositoryImpl에 흩어졌다.

**예외**: `RodiAppViewModel`의 `AuthRepository.observeSessionExpiration()` — 화면 기능이 아니라
앱 전역 세션 만료 신호를 구독하는 진입점으로 **현재 허용하는 제한된 예외**다.
같은 VM constructor default의 `ReissueAuthTokenUseCase(authRepository)` 직접 생성은
전역 composition-root 패턴이 아니라 별도 정리할 legacy debt다. 예외 확대는 명시 결정이 필요하다.
Repository import 차단은 현재 checker의 자동 집행 항목이 아니며 아래 검색과 review로 확인한다.

**정본**: `feature/course-registration/.../CourseRegistrationViewModel.kt` — 앵커 `@Inject constructor(`

**재검증** (위 예외 1건만 나와야 한다):
```bash
rg -n 'import com\.dororong\.rodi\.core\.domain\.repository\.' feature/*/src/main app/src/main
```

## Android namespace는 기본 패키지 + 모듈 경로

하이픈이 있는 모듈명은 유효한 패키지 세그먼트로 나눈다.

```kotlin
android { namespace = "com.dororong.rodi.feature.course.registration" }
```

**정본**: `feature/course-registration/build.gradle.kts`
