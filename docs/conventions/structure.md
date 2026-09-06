# 소스 루트·패키지 배치

> Rodi가 의도적으로 유지하는 **프로젝트 고유 규칙**이다. 프로젝트 무관 규범은 전역 스킬
> `/android-code-standard`에 있고, 여기 규칙이 그것과 충돌하면 **이 문서가 우선**한다.
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

## Contract는 feature 루트에 하나

**왜**: 화면 계약을 한 파일에서 훑을 수 있게 하려는 의도였다.

**주의 — 이 규칙은 현재 절반만 지켜진다.** 루트 `*Contract.kt`와 ViewModel 파일에 상태를
내장한 것이 비슷한 수로 공존한다. **컨벤션대로 옮길지, 하위 화면별 Contract를 허용하도록
이 규칙을 고칠지 먼저 정해야 한다.** 그 전까지 새 코드는 루트 Contract를 따른다.
→ `../BACKLOG.md`, 수치는 `../audits/`

**재검증**:
```bash
rg -l 'data class \w+UiState' -g '*ViewModel.kt' .
```

## Android namespace는 기본 패키지 + 모듈 경로

하이픈이 있는 모듈명은 유효한 패키지 세그먼트로 나눈다.

```kotlin
android { namespace = "com.dororong.rodi.feature.course.registration" }
```

**정본**: `feature/course-registration/build.gradle.kts`
