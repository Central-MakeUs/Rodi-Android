# 0001. Compose 의존성 출처 통일과 feature Convention Plugin

## 상태

채택 (2026-09-06)

## 배경

feature 6개가 같은 의존성 목록 7줄을 반복해서, Compose 버전 하나를 조정하려면 6개 파일을 고쳐야 했고 한 곳을 빠뜨려도 빌드가 통과했다. `AndroidHiltConventionPlugin`이 플러그인만 적용하고 의존성을 넣지 않아 Hilt 의존성도 모듈마다 중복 선언된 상태였다.

## 결정

Compose는 `:core:ui`가 `api`로 재노출하는 것을 유일한 출처로 삼는다. 디자인 시스템 모듈의 공개 API가 이미 Compose 타입을 노출하므로 `api`가 의미상으로도 맞다.

`dororong.rodi.android.feature` 플러그인은 화면 아키텍처 라이브러리인 activity-compose, lifecycle-compose, hilt-compose, Hilt compiler, ui-tooling과 `:core:ui` 의존성만 담는다. Compose BOM과 Compose bundle은 담지 않는다.

Compose BOM은 `androidTestImplementation` configuration에도 같은 원칙으로 적용한다 — `AndroidLibraryComposeConventionPlugin`(Compose를 쓰는 모든 모듈이 `feature` 플러그인을 거치든 직접 적용하든 공통으로 지나가는 지점)에 한 번만 선언하고, `:core:ui`와 feature 모듈에서 중복 선언하던 걸 제거했다.

## 결과

모듈의 `build.gradle.kts`만 봐서는 Compose가 어디서 오는지 보이지 않고, 전체 의존성 구성을 이해하려면 `build-logic`과 `:core:ui`를 함께 읽어야 한다. 대신 Compose 버전을 조정하는 지점은 `:core:ui` 한 곳으로 줄었다.

## 무효화하는 규범 항목

이 결정이 나온 직후 실제로 겪은 일이라 이 칸을 만들었다 — 직전에 끝낸 코드 관용구 전수 조사의
Gradle 항목("feature 7개가 `library.compose`를 직접 적용", "각 feature가 Compose·Hilt를 직접
선언", "BOM 중복 선언")이 이 PR로 **통째로 틀렸다.** 문서는 스스로 갱신되지 않는다.

- [x] `docs/PROJECT.md` — 모듈 맵에 `build-logic`이 아예 없고 의존성이 어디서 오는지 서술이
  없어서, 이 문서만 읽으면 Compose 출처를 알 수 없었다 → `build-logic` 행과 "의존성 출처는
  하나" 컨벤션 항목을 추가했다.
- [x] `docs/conventions/gradle.md` —
  "Compose library 7개가 `library.compose`를 직접 적용"은 이제 "`:core:ui` 1개만 직접,
  feature 6개는 `android.feature`를 통해 간접"이다. feature별 Compose·Hilt 중복 선언 근거와
  BOM 중복 불일치 항목도 사라졌다 → 해당 파일을 이 결정 기준으로 다시 썼다.
- [x] `docs/BACKLOG.md` — "app이 convention plugin 미사용"과 "`core:data`가 `useJUnitPlatform()`
  직접 선언"은 이 PR 이후에도 **여전히 유효**하므로 그대로 남긴다(실측 확인 2026-09-06).

## 하지 않은 것

테스트 의존성 bundle은 모듈마다 조합이 다르므로 플러그인으로 옮기지 않았다. `roborazzi-test`는 `feature:home`과 `core:ui`만 사용하고, `flow-test`도 일부 모듈만 사용한다.

Roborazzi/Robolectric `testOptions` 블록도 `core:ui`와 `feature:home`에 중복돼 있지만 별도 작업으로 남겼다.
