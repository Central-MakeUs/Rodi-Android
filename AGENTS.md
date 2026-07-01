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

## 절차
1. `docs/PROJECT.md` + `docs/handoff/HANDOFF.md` 정독. (Status가 `READY_FOR_IMPL`인지 확인)
2. Spec대로 구현. Files to touch 범위 안에서.
3. **빌드 green 필수**: `./gradlew assembleDebug` (실패 시 원인 수정 후 재시도).
4. `HANDOFF.md`의 **Codex Result** 섹션 작성:
   - `Changed files:` 변경 파일 목록
   - `Build/test:` 빌드/테스트 결과 (예: assembleDebug GREEN)
   - `Open questions:` 미해결/판단 보류 항목 (없으면 "none")
5. Status = `IMPL_DONE` (막혔으면 `BLOCKED`).
6. **커밋은 사용자/Claude가 한다** — Codex는 작업 트리만 남기고 보고로 끝낸다.

## 보고 포맷 (Codex Result 예시)
```
## Codex Result
- Changed files: app/.../MainActivity.kt, app/.../ui/AppRoot.kt
- Build/test: ./gradlew assembleDebug GREEN
- Open questions: none
```

## 참고
- 명령: `./gradlew assembleDebug` / `test` / `assembleRelease` (상세는 PROJECT.md)
- 멀티모듈 목표(미구현): `docs/ARCHITECTURE_TARGET.md`
