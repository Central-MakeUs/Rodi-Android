# AGENTS.md — Rodi Codex-only 워크플로

> 작업 전 반드시 `docs/PROJECT.md`와 `docs/handoff/HANDOFF.md`를 읽는다
> `PROJECT.md`는 프로젝트 사실과 컨벤션의 단일 진실원천이고 `HANDOFF.md`는 현재 작업 하나의 실행 기록이다

## 공통 원칙

- Codex가 Planner, Implementer, Reviewer 역할을 순서대로 수행한다
- 역할별 책임과 수정 권한을 섞지 않는다
- `HANDOFF.md`의 Spec과 Acceptance를 정확히 따르고 스코프를 넓히지 않는다
- 스펙이 모호하거나 서로 충돌하면 추측하지 않고 Status를 `BLOCKED`로 기록한다
- 관련 없는 파일과 사용자 변경을 되돌리지 않는다
- `PROJECT.md` 컨벤션을 지킨다
  - 색과 타이포는 `RodiTheme.colors`와 `RodiTheme.typography`만 사용한다
  - Material 아이콘은 사용하지 않으며 `Icons.*`도 금지한다
  - 자명한 코드 주석은 쓰지 않고 외부 연동에만 짧은 KDoc을 허용한다
  - `local.properties` 키와 다른 시크릿은 커밋하지 않는다

## HANDOFF 관리

- `docs/handoff/HANDOFF.md`에는 현재 작업 하나만 유지한다
- 새 작업은 `docs/handoff/TEMPLATE.md`를 복사해 시작한다
- 빈 `HANDOFF.md`는 진행 중인 작업이 없음을 뜻한다
- 완료된 작업은 Final Review 승인 직후 `docs/handoff/archive/`로 이동하고 `HANDOFF.md`를 빈 템플릿으로 되돌린다
- archive 파일명은 `YYYYMMDD-<task-slug>.md` 형식을 기본으로 하되 사용자가 지정한 이름이 있으면 그 이름을 사용한다
- archive는 과거 기록이며 사용자가 명시적으로 요청하지 않으면 내용을 읽거나 현재 작업의 근거로 사용하지 않는다
- 현재 작업의 계획, 구현 결과, 리뷰, 수정 이력은 모두 같은 `HANDOFF.md`에 누적한다
- 상태 전이는 `PLANNING` → `READY_FOR_IMPL` → `IMPL_DONE` → `DONE` 순서를 기본으로 한다
- 수정이 필요하면 `IMPL_DONE` → `REVISION_REQUIRED` → `REVISION_READY` → `REVISION_DONE` → `DONE`으로 진행한다
- 진행할 수 없는 경우 어느 단계에서든 `BLOCKED`로 전환하고 근거를 해당 역할 섹션에 기록한다

## Planner

- 사용자 요청, `PROJECT.md`, 현재 코드의 읽기 전용 조사 결과를 바탕으로 HANDOFF를 작성한다
- 앱 소스, Gradle, 테스트 코드를 수정하지 않는다
- Context, Goal, Spec, Acceptance, Expected Files, Verification, Out of Scope를 구체적으로 작성한다
- Branch와 Base에는 실제 작업 브랜치와 통합 기준을 기록한다
- Risk는 변경 영향과 실패 가능성을 기준으로 `LOW`, `MEDIUM`, `HIGH` 중 하나로 기록한다
- Acceptance는 관찰 가능하고 검증 가능한 완료 조건으로 작성한다
- Expected Files는 예상 변경 범위를 제한하며 범위 밖 변경이 필요하면 이유를 먼저 HANDOFF에 반영한다
- 구현 판단에 필요한 정보가 충분할 때만 Status를 `READY_FOR_IMPL`로 변경한다
- Review의 유효한 지적을 Review Triage에서 수용, 기각, 보류로 분류하고 근거를 남긴다
- 수용한 지적은 Revision Plan에 파일, 변경 내용, 검증 방법을 작성한 뒤 Status를 `REVISION_READY`로 변경한다

## Implementer

- Status가 `READY_FOR_IMPL` 또는 `REVISION_READY`일 때만 구현 파일을 수정한다
- 그 외 상태에서는 앱 소스, Gradle, 테스트 코드를 수정하지 않는다
- Spec, Acceptance, Expected Files, Out of Scope를 구현 경계로 사용한다
- 최초 구현 결과는 Implementation Result에, 수정 구현 결과는 Revision Result에 기록한다
- 결과에는 Changed files, 실행한 검증 명령과 결과, 남은 Open questions를 포함한다
- 최초 구현이 끝나면 Status를 `IMPL_DONE`, 수정 구현이 끝나면 `REVISION_DONE`으로 변경한다
- 구현 중 스펙 모호성, 권한 부족, 외부 의존성으로 완료할 수 없으면 Status를 `BLOCKED`로 변경한다
- 사용자가 명시적으로 요청하지 않으면 커밋하지 않는다

## Reviewer

- 구현에 참여하지 않은 독립 Codex 컨텍스트에서 검토한다
- 앱 소스, Gradle, 테스트 코드와 구현 산출물을 수정하지 않는다
- 최초 검토는 `IMPL_DONE`, 수정 후 검토는 `REVISION_DONE` 상태에서 시작한다
- HANDOFF의 Acceptance와 Base 기준 `git diff`를 검토의 중심 근거로 사용한다
- 커밋된 차이뿐 아니라 staged, unstaged, untracked 변경과 실제 최종 트리를 확인한다
- 구현 결과를 신뢰 전제로 두지 않고 누락, 회귀, 경계 조건, 스코프 이탈, 검증 공백을 적극적으로 찾는 적대적 리뷰를 수행한다
- 각 Acceptance를 충족, 미충족, 증거 부족으로 판정하고 코드, diff, 테스트 출력 등 재현 가능한 증거를 남긴다
- 지적에는 심각도, 파일과 위치, 증거, 사용자 영향, 필요한 수정 조건을 포함한다
- 근거 없는 추측이나 스타일 취향은 blocking finding으로 기록하지 않는다
- 코드를 수정하지 않으며 최초 결과는 Review, 수정 후 결과는 Final Review에 기록한다
- 수정이 필요하면 Status를 `REVISION_REQUIRED`로 변경한다
- 모든 Acceptance가 충족되고 blocking finding이 없으면 Final Review에 승인 근거를 기록하고 Status를 `DONE`으로 변경한다

## 플로우 변경 범위

- 사용자가 특정 화면이나 단계를 예시로 들어도 별도 범위 제한이 없다면 해당 공통 UI와 상태가 쓰이는 모든 화면을 확인한다
- 화면 전환과 공통 상태 변경은 정방향과 뒤로 경로에서 모두 검증한다
- 화면이 교체되는 경우에도 이전 상태 유지 여부를 확인하고 현재 보이는 화면만 수정해 완료로 판단하지 않는다
- Implementer는 구현 전 영향받는 흐름을 짧게 기록하고 구현 후 최소 한 번 정방향과 역방향 전환을 검증한다

## 스킬 선택

- Android 모듈, Gradle, Clean Architecture, MVI, Compose lifecycle, 테스트 작업은 구현 전에 `maintainable-android-delivery`를 읽고 적용한다
- Figma 또는 스크린샷 기반 UI 구현과 검증은 `visual-design-sync`를 함께 적용한다
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

- Implementer는 HANDOFF Verification에 적힌 검증과 `./gradlew assembleDebug`를 실행한다
- 실패하면 원인을 수정하고 다시 실행하며 실행할 수 없는 검증은 이유와 영향을 기록한다
- Reviewer는 `git diff --check`와 Acceptance별 검증 증거를 독립적으로 확인한다
- PR 본문의 체크리스트에는 코드 셀프 리뷰, 실기기 확인, 디자인 대조처럼 사람이 완료 여부를 판단하는 항목만 둔다
- 빌드, 테스트, lint 명령과 결과는 PR 본문의 별도 `## 검증` 섹션에 일반 bullet로 기록한다
- `gh pr create` 또는 `gh pr edit`의 `--body`에 Markdown backtick을 직접 넣지 않고 필요하면 `--body-file`을 사용한다

## 참고

- 명령은 `./gradlew assembleDebug`, `./gradlew test`, `./gradlew assembleRelease`, `./gradlew lint`를 사용하며 상세는 `docs/PROJECT.md`를 따른다
- 모듈과 패키지 구조는 `docs/ARCHITECTURE_TARGET.md`를 따른다
