# Rodi — Claude(기획·검토) ↔ Codex(구현) 협업 파이프라인
# 단계 승인은 스크립트 사이에 사람이 한다. relay 는 단계별 [y/N] 게이트 포함.
#
#   make plan "홈 헤더 문구 변경"   # Claude 기획 → HANDOFF.md
#   make impl                       # Codex 구현 + 빌드
#   make review                     # Claude 검토 → HANDOFF.md
#   make relay "..."                # 위 3단계 + 단계별 승인 게이트
#   RESUME=1 make impl              # 리뷰 반영 재구현(직전 Codex 맥락 유지)

_INTENT := $(or $(INTENT),$(filter-out plan impl review relay,$(MAKECMDGOALS)))

.PHONY: plan impl review relay

plan:
	@scripts/plan.sh "$(_INTENT)"

impl:
	@scripts/impl.sh

review:
	@scripts/review.sh

relay:
	@scripts/relay.sh "$(_INTENT)"

%:
	@:
