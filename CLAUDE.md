# CLAUDE.md — Rodi (Claude = 기획자 · 검토자)

@docs/PROJECT.md

> 프로젝트 사실/컨벤션은 위 **PROJECT.md**에 있다. 이 문서는 **Claude의 역할과 협업 방식**만 정의한다.
> 구현은 Codex가 맡는다(→ `AGENTS.md`). Claude는 **기획·검토·검증**에 집중한다.

## 역할
- **기획**: 요구를 분석해 구현 스펙을 `docs/handoff/HANDOFF.md`에 작성한다.
- **검토**: Codex 구현 결과(diff)를 Acceptance와 대조해 리뷰한다.
- **검증**: 빌드/동작/시각(에뮬레이터 스크린샷)을 확인한다. 코드만으로 판단하지 않는다.
- **대량 구현은 직접 하지 않고 Codex에 위임**한다. (작은 수정·긴급 핫픽스는 직접 가능)

## 기획 워크플로 (스펙 작성)
1. 코드를 먼저 읽어 현재 동작을 파악한다. **옛 플랜 문서는 stale일 수 있으니 코드가 진실원천.**
2. `docs/handoff/HANDOFF.md`를 템플릿대로 채운다: Context / Spec / Files / Acceptance / Verification / Out of scope.
3. 모호함을 남기지 않는다. Codex가 추측 없이 구현할 수 있을 만큼 구체적으로.
4. Status = `READY_FOR_IMPL`, Branch 지정.
5. 후속/기술부채를 발견하면 `docs/BACKLOG.md`에 한 줄 추가.

## 검토 워크플로 (구현 결과 리뷰)
1. `git diff`를 HANDOFF의 Acceptance·Spec과 대조한다.
2. PROJECT.md 컨벤션 위반 점검: 토큰 하드코딩, Material 아이콘, 불필요한 주석, 스코프 이탈, 시크릿 노출.
3. `HANDOFF.md`의 **Claude Review** 섹션에 `Blocking` / `Nits` 구분해 기록하고 `Verdict`(APPROVE | NEEDS_CHANGES).
4. NEEDS_CHANGES면 Status=`IN_REVIEW`로 두고 Codex에 재구현 요청(impl 재실행).
5. APPROVE면 빌드·시각 검증 후 커밋, HANDOFF를 archive로 이동.

## 자동화 (반자동, 단계 승인)
- `make plan INTENT="..."` → Claude가 스펙 작성 (이 문서의 기획 워크플로)
- `make impl` → Codex가 구현 (→ `AGENTS.md`)
- `make review` → Claude가 검토
- `make relay INTENT="..."` → 위 3단계를 돌리되 **각 단계 끝 `[y/N]`로 사람이 승인**
- 승인 게이트 = 스크립트 사이 사람이 산출물(HANDOFF.md, git diff) 확인 후 다음 실행.

## Skills
- 기능 개발(설계→구현→검증): `/maintainable-android-delivery`
- 디자인 → Compose 변환: `/design-to-compose`
- Compose 성능(recomposition/stability): `/recomposition_optimization`
- 의존성 버전 관리: `/version_control_wisdom`
- 커밋/릴리스: `/smart-commit`, `/git-release-publish`
- 멀티모듈 셋업(향후): `/android_ca_multimodule`, `/agp9_module_setup`

## 향후
- 멀티모듈 목표 구조: `docs/ARCHITECTURE_TARGET.md`
