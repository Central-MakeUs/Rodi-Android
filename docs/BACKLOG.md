# BACKLOG.md — Rodi 후속/기술부채 (에이전트 공유)

> Codex는 Claude의 개인 메모리를 볼 수 없다. **두 에이전트가 공유해야 할 후속 항목은 여기에** 둔다.
> 한 줄씩 누적하고, 착수 시 `docs/handoff/HANDOFF.md`로 옮겨 작업한다.

## 열린 항목
- [ ] **`TermsWebViewScreen` 죽은 코드 삭제** — `:app 코드 이관 Phase 2`(2026-07-01) 중 발견: 이
  컴포저블은 어디서도 호출되지 않는다(`TermsWebView`만 직접 쓰임). 이관 시엔 동작 보존 우선으로
  그대로 옮겼음(`:core:ui`의 `terms` 패키지). 실제 삭제는 별도로.
- [ ] **`:app` 미사용 의존성 정리 검토** — Phase 2 이관 후 `:app`의 `material3`/`ui.graphics`/
  `ui.tooling.preview`/`lifecycle.viewmodel.compose`가 `MainActivity`/`RodiApplication`/`AppRoot`
  에서 직접 쓰이는지 불확실. 이번 이관으로 죽은 게 확실한 `core:domain`/`play-services-location`만
  제거했고, 나머지는 별도 검증 후 정리.
- [ ] **Kotlin 2.2.10 → 2.4.0 / AGP 버전 업그레이드** — Google Maven 기준 Kotlin 최신 안정은 2.4.0,
  AGP는 현재 프로젝트(9.2.1)가 이미 공개 릴리스 노트보다 앞서 있음. 컴파일러 호환성(compose
  compiler, KSP 등) 검증이 필요해 Java 21 통일 작업(2026-07-01)에서 범위 밖으로 뺌.
- [ ] **Kakao Map/Navi SDK 버전 업그레이드 검토** — `kakaoMap`(2.11.9)/`kakaoSdk`(2.20.6) 최신 여부
  미확인. 지도·내비 핵심 기능 회귀 위험이 있어 별도 검증 후 진행.
- [ ] **시스템 바 화면별 동적 컬러** — 현재 `MainActivity`가 `SystemBarStyle.light`로 상·하단 아이콘을
  항상 검정 고정. entry/약관 등 **어두운 배경 화면에서는 흰 아이콘**이 필요. 공식·비deprecated 방식
  (`enableEdgeToEdge` + 화면별 `SystemBarStyle` 전환 또는 `WindowInsetsControllerCompat.isAppearanceLightStatusBars`)
  으로 화면에 따라 동적 전환. ← 파이프라인 첫 실작업 후보.

## 완료 (이력)
- [x] Routi → Rodi 브랜드 식별자 정리 (PR #8)
