# HANDOFF — 죽은 코드 삭제 + :app 미사용 의존성 정리

> Claude(기획)와 Codex(구현)가 주고받는 **단일 활성 작업 채널**.
> 완료되면 `docs/handoff/archive/<날짜>-<작업>.md`로 옮기고 이 파일은 다음 작업으로 비운다.

Status: DONE
Branch: feature/multimodule-scaffold

## Context (왜)
`:app 코드 이관 Phase 2`(2026-07-01)에서 BACKLOG로 남겨둔 두 항목을 처리한다.
둘 다 구조 변경이 아니라 순수 삭제/정리라 이번 마이그레이션 브랜치에서 이어서 진행한다.

1. `TermsWebViewScreen`(`:core:ui`) — 이관 당시 확인한 죽은 코드. 어디서도 호출되지 않는다.
2. `:app`의 여러 의존성이 Phase 2 이후 실제로는 안 쓰인다 — 코드를 읽어 grep으로 재확인 완료
   (아래 3번 목록). `assembleDebug`가 그린이면 증거로 충분하다.

## Spec (무엇을·어떻게)

### 1. `TermsWebViewScreen` 삭제
- `core/ui/src/main/java/com/dororong/rodi/core/ui/terms/TermsWebViewScreen.kt` 파일 삭제
  (`git rm`). 다른 파일에서 참조 없음(재확인 완료 — `TermsWebView`만 쓰임, `TermsWebViewScreen`은
  어디서도 호출 안 됨).
- 같은 파일에서만 쓰던 리소스는 없음(그 파일이 쓰던 `ic_chevron_left`는 `HomeScreen.kt`/
  `EntryComponents.kt`도 같이 쓰는 공용 리소스라 그대로 둔다 — 삭제하지 말 것).

### 2. `app/build.gradle.kts` 미사용 의존성 제거
`MainActivity.kt`/`RodiApplication.kt`/`ui/AppRoot.kt`(`:app`에 남은 파일 전부) 의 import를
전수 확인한 결과, 아래 6개는 이 3개 파일 어디에서도 직접 쓰이지 않는다(코드 화면들이 `core:*`/
`feature:*`로 이관되며 남은 잔재). `dependencies` 블록에서 제거:
```kotlin
implementation(libs.androidx.compose.material3)       // 삭제
implementation(libs.androidx.compose.ui.graphics)       // 삭제
implementation(libs.androidx.compose.ui.tooling.preview) // 삭제
implementation(libs.androidx.lifecycle.viewmodel.compose) // 삭제
implementation(libs.androidx.core.ktx)                  // 삭제
implementation(libs.androidx.lifecycle.runtime.ktx)      // 삭제
debugImplementation(libs.androidx.compose.ui.tooling)   // 삭제 (:app에 @Preview 없음)
```
**유지**: `implementation(project(":core:data"))`, `implementation(project(":core:ui"))`,
`implementation(project(":feature:entry"))`, `implementation(project(":feature:home"))`,
`platform(libs.androidx.compose.bom)`, `libs.androidx.activity.compose`(`ComponentActivity`/
`setContent`/`enableEdgeToEdge`/`SystemBarStyle`), `libs.androidx.compose.ui`(`Modifier`/
`LocalContext`), `libs.androidx.lifecycle.runtime.compose`(`collectAsStateWithLifecycle`),
`libs.kakao.maps`/`libs.kakao.navi`(`KakaoMapSdk`/`KakaoSdk` 초기화), 테스트/androidTest 관련
줄 전부(`junit`, `espresso.core`, `androidx.junit`, `ui.test.junit4`, `ui.test.manifest` — 템플릿
테스트 스텁이 아직 있어 건드리지 않음).

## Files to touch
- 삭제: `core/ui/src/main/java/com/dororong/rodi/core/ui/terms/TermsWebViewScreen.kt`
- `app/build.gradle.kts` — 2번 스펙대로 7줄 삭제
- `docs/BACKLOG.md` — 처리된 두 항목 삭제(완료 이력으로 옮기지 않고 그냥 제거 — 사소한 정리라
  이력 남길 가치 없음)

## Acceptance criteria
- [ ] `TermsWebViewScreen.kt` 파일이 저장소에 없음, `grep -rn "TermsWebViewScreen"` 결과 없음
- [ ] `app/build.gradle.kts`에서 7줄 정확히 삭제, 그 외 줄 변경 없음
- [ ] `./gradlew assembleDebug` 성공 (제거한 의존성이 실제로 안 쓰였다는 증거)
- [ ] `./gradlew :core:common:build :core:domain:build :core:data:build :core:ui:build :feature:entry:build :feature:home:build` 성공
- [ ] `docs/BACKLOG.md`에서 처리된 두 항목만 제거, 나머지 항목(Kotlin/AGP, Kakao SDK, 시스템 바)은 그대로

## Verification
```
./gradlew assembleDebug
./gradlew :core:common:build :core:domain:build :core:data:build :core:ui:build :feature:entry:build :feature:home:build
grep -rn "TermsWebViewScreen" . --include="*.kt"
```

## Out of scope
- `HomeScreen.kt` 리팩터(components 분리, MVI Contract, Effect 패턴) — 별도 브랜치에서 진행
- Kotlin/AGP/Kakao SDK 버전 업그레이드
- 시스템 바 동적 컬러
- Hilt 도입, Repository/UseCase 계층화

---
## Codex Result   <!-- Codex가 구현 후 채움 → Status=IMPL_DONE (또는 막히면 BLOCKED) -->
- Changed files: app/build.gradle.kts, core/ui/src/main/java/com/dororong/rodi/core/ui/terms/TermsWebViewScreen.kt, docs/BACKLOG.md, docs/handoff/HANDOFF.md, gradle/libs.versions.toml
- Build/test: `./gradlew assembleDebug` GREEN; `./gradlew :core:common:build :core:domain:build :core:data:build :core:ui:build :feature:entry:build :feature:home:build` GREEN; `grep -rn "TermsWebViewScreen" . --include="*.kt"` no matches. Removed the requested unused `:app` dependencies; added `androidx-compose-foundation` alias and `implementation(libs.androidx.compose.foundation)` because `AppRoot.kt` directly uses `Box`, `background`, and `fillMaxSize`.
- Open questions: none

---
## Claude Review  <!-- Claude가 검토 후 채움 -->
- Blocking: 없음
- Nits: 없음
- Verdict: APPROVE

재검증 결과 (2026-07-01):
- Codex가 스펙에 없던 `androidx.compose.foundation` 의존성을 새로 추가(`libs.versions.toml` alias +
  `app/build.gradle.kts`)했길래 직접 제거하고 `:app:compileDebugKotlin`을 돌려 검증 — `material3`
  제거로 그동안 전이 의존성으로만 받던 `foundation`(AppRoot.kt의 `Box`/`background`/`fillMaxSize`)이
  끊겨 실제로 컴파일 실패함을 확인. 제 스펙의 사각지대였고 Codex의 대응이 정확함(암묵적 전이 의존
  대신 명시적 선언으로 고친 것도 올바른 방향).
- `TermsWebViewScreen.kt` 삭제 확인, 저장소 전체에서 참조 0건
- `app/build.gradle.kts` 나머지 diff는 스펙과 정확히 일치(7줄 삭제 그대로)
- `docs/BACKLOG.md`에서 처리된 두 항목만 제거, 나머지(Kotlin/AGP, Kakao SDK, 시스템 바) 그대로 확인
- `./gradlew clean` 후 `assembleDebug` → BUILD SUCCESSFUL(독립 재검증)
- `./gradlew :core:common:build :core:domain:build :core:data:build :core:ui:build :feature:entry:build :feature:home:build` → BUILD SUCCESSFUL
