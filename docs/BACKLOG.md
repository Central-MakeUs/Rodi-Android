# BACKLOG.md — Rodi 후속/기술부채 (에이전트 공유)

> Codex는 Claude의 개인 메모리를 볼 수 없다. **두 에이전트가 공유해야 할 후속 항목은 여기에** 둔다.
> 한 줄씩 누적하고, 착수 시 `docs/handoff/HANDOFF.md`로 옮겨 작업한다.

## 열린 항목
- [ ] **Repository 인터페이스 domain 이동 검토** — 현재 `RouteResult`(Kakao `LatLng` 포함)와 `NaviApp`이
  `core:data` 타입이라 Hilt 도입 작업에서는 Repository 인터페이스/구현체를 함께 `core:data`에 둠.
  domain purity를 위해 vendor 타입 분리 후 `core:domain` 이동 여부를 별도 작업으로 검토.
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
