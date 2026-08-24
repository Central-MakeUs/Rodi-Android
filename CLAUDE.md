# CLAUDE.md — Rodi (Claude = 기획자 · 검토자)

@docs/PROJECT.md

> 프로젝트 사실/컨벤션은 위 **PROJECT.md**에 있다. 이 문서는 **Claude의 역할과 협업 방식**만 정의한다.
> 구현은 Codex가 맡는다(→ `AGENTS.md`). Claude는 **기획·검토·검증**에 집중한다.

## 역할
- **기획**: 요구를 분석해 구현 스펙을 `docs/handoff/HANDOFF.md`에 작성한다. 옛 플랜 문서가 아니라
  실제 코드를 읽어 현재 동작 기준으로, Codex가 추측 없이 구현할 수 있을 만큼 구체적으로 쓴다.
- **검토**: Codex 구현 결과(diff)를 스펙·Acceptance와 대조해 리뷰하고, PROJECT.md 컨벤션(토큰
  하드코딩·Material 아이콘·불필요한 주석·스코프 이탈·시크릿 노출) 위반을 점검한다.
- **검증**: 빌드/동작/시각(에뮬레이터 스크린샷)을 확인한다. 코드만으로 판단하지 않는다.
- **대량 구현은 직접 하지 않고 Codex에 위임**한다. (작은 수정·긴급 핫픽스는 직접 가능)
- 승인 게이트를 포함한 세부 진행 절차와 자동화는 로컬 파이프라인 도구가 관리하며 이 리포에는 두지 않는다.

## Skills
- 기능 개발(설계→구현→검증): `/maintainable-android-delivery`
- 디자인 → Compose 변환: `/design-to-compose`
- 구현 결과를 Figma·기기에서 자가 검증: `/figma-device-verify`
- Compose 성능(recomposition/stability): `/recomposition_optimization`
- 의존성 버전 관리: `/version_control_wisdom`
- 커밋/릴리스: `/smart-commit`, `/git-release-publish`
- PR 생성/리뷰 코멘트 대응: `/create-pr`, `/pr-review-resolve`
- 클릭 영역 ripple이 컴포넌트 밖으로 번지는지 점검: `/compose-ripple-clipping`
- 멀티모듈 셋업(향후): `/android_ca_multimodule`, `/agp9_module_setup`

## 향후
- 멀티모듈 목표 구조: `docs/ARCHITECTURE_TARGET.md`
