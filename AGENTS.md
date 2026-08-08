# AGENTS.md — Rodi, Codex 전용 워크플로

> 작업 전 반드시 `docs/PROJECT.md`와 `docs/handoff/HANDOFF.md`를 읽는다
> `PROJECT.md`는 프로젝트 사실과 컨벤션의 단일 진실원천이고 `HANDOFF.md`는 현재 작업 하나의 실행 기록이다

## 공통 원칙

- 이 리포에서 Codex는 **Implementer 역할만** 수행한다. 기획과 검토는 Claude가 별도로 맡는다 —
  구현 중에 스펙을 다시 쓰거나 자기 결과물을 승인 판정하지 않는다
- `HANDOFF.md`의 Spec과 Acceptance를 정확히 따르고 스코프를 넓히지 않는다
- 스펙이 모호하거나 서로 충돌하면 추측하지 않고 Status를 `BLOCKED`로 기록한다
- 관련 없는 파일과 사용자 변경을 되돌리지 않는다
- `PROJECT.md` 컨벤션을 지킨다
  - 색과 타이포는 `RodiTheme.colors`와 `RodiTheme.typography`만 사용한다
  - Material 아이콘은 사용하지 않으며 `Icons.*`도 금지한다
  - 자명한 코드 주석은 쓰지 않고 외부 연동에만 짧은 KDoc을 허용한다
  - `local.properties` 키와 다른 시크릿은 커밋하지 않는다
- `HANDOFF.md`/`docs/handoff/archive/`는 작업 중인 스펙·구현 결과가 담기므로 원격에 올리지
  않는다(`.gitignore` 참고). 이 리포에 남기는 문서는 컨벤션과 역할 요약까지이고, 진행 상태
  추적·승인 절차 세부사항은 로컬 도구가 관리한다

## Implementer

- Spec, Acceptance, Expected Files, Out of Scope를 구현 경계로 사용한다
- 구현 결과와 실행한 검증 명령·결과, 남은 Open questions를 `HANDOFF.md`에 기록한다
- 구현이 끝나면 Status를 완료로, 스펙 모호성·권한 부족·외부 의존성으로 완료할 수 없으면
  `BLOCKED`로 변경한다
- 사용자가 명시적으로 요청하지 않으면 커밋하지 않는다

## 플로우 변경 범위

- 사용자가 특정 화면이나 단계를 예시로 들어도 별도 범위 제한이 없다면 해당 공통 UI와 상태가 쓰이는 모든 화면을 확인한다
- 화면 전환과 공통 상태 변경은 정방향과 뒤로 경로에서 모두 검증한다
- 화면이 교체되는 경우에도 이전 상태 유지 여부를 확인하고 현재 보이는 화면만 수정해 완료로 판단하지 않는다
- 구현 전 영향받는 흐름을 짧게 기록하고 구현 후 최소 한 번 정방향과 역방향 전환을 검증한다

## 스킬 선택

- Android 모듈, Gradle, Clean Architecture, MVI, Compose lifecycle, 테스트 작업은 구현 전에 `maintainable-android-delivery`를 읽고 적용한다
- Figma 또는 스크린샷 기반 UI 구현과 검증은 `figma-device-verify`를 함께 적용한다
- 커밋, 푸시, PR, 병합, 릴리스 작업은 `git-release-publish`를 적용한다
- 스킬에 구체 규칙이 없으면 `docs/PROJECT.md`와 `docs/ARCHITECTURE_TARGET.md`가 우선한다

## 구조 규칙

- 같은 역할의 파일이 2개 이상이면 역할 패키지를 만들고 하나뿐이면 Screen, ViewModel, Contract와 같은 depth에 둔다
- feature의 Contract는 하나의 루트 파일로 유지한다
- 재사용 public Composable은 파일 하나당 하나가 기본이며 소유 컴포넌트 전용 private helper만 같은 파일에 둔다
- Domain과 Data 패키지 및 의존 방향은 `docs/ARCHITECTURE_TARGET.md`를 따른다
- 동일 Gradle configuration에서 항상 함께 쓰는 의존성이 2개 이상이면 version catalog bundle을 사용한다
- BOM, compiler, debug/runtime 전용처럼 configuration이나 적용 방식이 다른 의존성은 묶지 않는다

## 검증과 보고

- HANDOFF Verification에 적힌 검증과 `./gradlew assembleDebug`를 실행한다
- 실패하면 원인을 수정하고 다시 실행하며 실행할 수 없는 검증은 이유와 영향을 기록한다
- 커밋·PR 생성은 기본적으로 하지 않는다(Claude가 별도로 수행). 사용자가 예외적으로 직접 커밋/PR을
  요청한 경우에만: PR 본문 체크리스트에는 코드 셀프 리뷰, 실기기 확인, 디자인 대조처럼 사람이
  완료 여부를 판단하는 항목만 두고, 빌드·테스트·lint 결과는 별도 `## 검증` 섹션에 일반 bullet로
  기록하며, `gh pr create`/`gh pr edit`의 `--body`엔 backtick을 직접 넣지 않고 `--body-file`을 쓴다

## 참고

- 명령은 `./gradlew assembleDebug`, `./gradlew test`, `./gradlew assembleRelease`, `./gradlew lint`를 사용하며 상세는 `docs/PROJECT.md`를 따른다
- 모듈과 패키지 구조는 `docs/ARCHITECTURE_TARGET.md`를 따른다
