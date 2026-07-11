# HANDOFF - Project structure refactor

Status: IMPL_DONE
Branch: refactor/project-structure

## Context

Domain/Data/Feature 패키지와 Gradle 의존성 구성이 기능 추가 과정에서 일관되지 않게 누적되어,
현재 멀티모듈 구조에 맞는 소유권과 파일 분리 기준으로 정리한다.

## Implemented

- Domain 모델·repository·usecase를 역할/기능 패키지로 분리하고 `AuthSession` 조회 계약 추가
- Data를 `mapper`, `repository`, `source/local`, `source/remote`로 분리
- repository의 Context 직접 생성과 private mapper 제거, concrete source 주입
- App 전용 ViewModel에서 진입 상태 조합
- Auth/Entry/Home Contract와 lifecycle-aware 수집 통일
- Entry/Home public Composable 파일 분리와 패키지 정리
- `:feature:settings` 모듈과 App route 추가, Home은 navigation Effect만 발행
- version catalog bundle 적용
- 프로젝트 스킬 선택·패키지·컴포넌트·bundle 규칙 문서화

## Codex Result

- Changed files: `app`, `core/domain`, `core/data`, `feature/auth`, `feature/entry`, `feature/home`,
  `feature/settings`, `gradle/libs.versions.toml`, `settings.gradle.kts`, `AGENTS.md`, `docs/PROJECT.md`,
  `docs/ARCHITECTURE_TARGET.md`
- Build/test: Domain dependency static check GREEN; `./gradlew test` GREEN; `./gradlew lint` GREEN;
  `./gradlew assembleDebug` GREEN; `git diff --check` GREEN
- Open questions: Home -> Settings -> terms -> back manual device verification was not run. Local feature/settings/build/docs commits are pending because the Codex git-write approval quota was unavailable during delivery.
