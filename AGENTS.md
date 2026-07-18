# AGENTS.md — Rodi (Codex = 구현자)

> **작업 전 반드시 `docs/PROJECT.md`와 `docs/handoff/HANDOFF.md`를 읽어라.**
> `PROJECT.md` = 프로젝트 사실/컨벤션(단일 진실원천). `HANDOFF.md` = Claude가 작성한 현재 작업 스펙.
> Codex는 **구현**을 맡는다. 기획/검토는 Claude가 한다(→ `CLAUDE.md`).

## 역할 / 원칙
- `HANDOFF.md`의 **Spec을 정확히** 구현한다. **스코프를 넓히지 않는다.**
- 스펙이 모호하면 **추측하지 말고** `Codex Result`의 `Open questions`에 적고 Status=`BLOCKED`로 멈춘다.
- `PROJECT.md` 컨벤션을 지킨다:
  - 색/타이포는 `RodiTheme.colors` / `RodiTheme.typography`만 (하드코딩 금지)
  - **Material 아이콘 금지** (Figma 추출/요청, `Icons.*` 사용 금지)
  - 자명한 코드 주석 X, 외부 연동만 짧은 KDoc
  - 시크릿(local.properties 키) 커밋 금지
- 관련 없는 파일/사용자 변경을 되돌리지 않는다.

## 플로우 변경 범위
- 사용자가 특정 화면·단계를 예시로 들어도 별도의 범위 제한이 없다면, 해당 공통 UI/상태가 쓰이는 모든 화면과 정방향·뒤로 경로를 확인한다.
- 화면 전환 애니메이션·공통 상태 변경은 화면이 교체되는 경우에도 이전 상태가 유지되는지 확인한다. 현재 보이는 화면만 수정해 완료로 판단하지 않는다.
- 구현 전 영향을 받는 흐름을 짧게 명시하고, 구현 후에는 최소 한 번 정방향과 역방향 전환을 모두 검증한다.

## 스킬 선택
- Android 모듈, Gradle, Clean Architecture, MVI, Compose lifecycle, 테스트 작업은 구현 전에
  `maintainable-android-delivery`를 읽고 적용한다.
- Figma·스크린샷 기반 UI 구현/검증은 `visual-design-sync`를 함께 적용한다.
- 커밋·푸시·PR·병합·릴리스 작업은 `git-release-publish`를 적용한다.
- 스킬에 구체 규칙이 없으면 이 저장소의 `PROJECT.md`와 `ARCHITECTURE_TARGET.md`가 우선한다.

## 구조 규칙
- 같은 역할의 파일이 2개 이상이면 역할 패키지를 만든다. 하나뿐이면 Screen/ViewModel/Contract와 같은 depth에 둔다.
- feature의 Contract는 하나의 루트 파일로 유지한다.
- 재사용 public Composable은 파일 하나당 하나가 기본이다. 소유 컴포넌트 전용 private helper만 같은 파일에 둔다.
- Domain/Data 패키지와 의존 방향은 `docs/ARCHITECTURE_TARGET.md`를 따른다.
- 동일 Gradle configuration에서 항상 함께 쓰는 의존성이 2개 이상이면 version catalog bundle을 사용한다.
  BOM, compiler, debug/runtime 전용처럼 configuration 또는 적용 방식이 다른 의존성은 묶지 않는다.

## 절차
1. `docs/PROJECT.md` + `docs/handoff/HANDOFF.md` 정독. (Status가 `READY_FOR_IMPL`인지 확인)
2. Spec대로 구현. Files to touch 범위 안에서.
3. **빌드 green 필수**: `./gradlew assembleDebug` (실패 시 원인 수정 후 재시도).
4. `HANDOFF.md`의 **Codex Result** 섹션 작성:
   - `Changed files:` 변경 파일 목록
   - `Build/test:` 빌드/테스트 결과 (예: assembleDebug GREEN)
   - `Open questions:` 미해결/판단 보류 항목 (없으면 "none")
5. Status = `IMPL_DONE` (막혔으면 `BLOCKED`).
6. 커밋은 사용자가 명시적으로 요청한 경우에만 수행한다.

## PR 본문 규칙
- PR 본문의 **체크리스트**에는 코드 셀프 리뷰, 실기기 확인, 디자인 대조처럼 사람이 완료 여부를 판단하는 항목만 둔다.
- 빌드·테스트·lint 명령과 결과는 반드시 별도 `## 검증` 섹션에 일반 bullet로 기록한다. 검증 결과를 체크리스트 항목으로 쓰지 않는다.
- `gh pr create` 또는 `gh pr edit`의 `--body`에 Markdown backtick을 넣지 않는다. 셸 명령 치환으로 실행·삽입될 수 있으므로, 명령어 표기가 필요하면 `--body-file`을 사용한다.

## 보고 포맷 (Codex Result 예시)
```
## Codex Result
- Changed files: app/.../MainActivity.kt, app/.../ui/AppRoot.kt
- Build/test: ./gradlew assembleDebug GREEN
- Open questions: none
```

## 참고
- 명령: `./gradlew assembleDebug` / `test` / `assembleRelease` (상세는 PROJECT.md)
- 모듈·패키지 구조: `docs/ARCHITECTURE_TARGET.md`
