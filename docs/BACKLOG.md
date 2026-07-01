# BACKLOG.md — Rodi 후속/기술부채 (에이전트 공유)

> Codex는 Claude의 개인 메모리를 볼 수 없다. **두 에이전트가 공유해야 할 후속 항목은 여기에** 둔다.
> 한 줄씩 누적하고, 착수 시 `docs/handoff/HANDOFF.md`로 옮겨 작업한다.

## 열린 항목
- [ ] **시스템 바 화면별 동적 컬러** — 현재 `MainActivity`가 `SystemBarStyle.light`로 상·하단 아이콘을
  항상 검정 고정. entry/약관 등 **어두운 배경 화면에서는 흰 아이콘**이 필요. 공식·비deprecated 방식
  (`enableEdgeToEdge` + 화면별 `SystemBarStyle` 전환 또는 `WindowInsetsControllerCompat.isAppearanceLightStatusBars`)
  으로 화면에 따라 동적 전환. ← 파이프라인 첫 실작업 후보.

## 완료 (이력)
- [x] Routi → Rodi 브랜드 식별자 정리 (PR #8)
