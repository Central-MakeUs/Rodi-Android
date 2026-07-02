# Rodi — Claude(기획·검토) ↔ Codex(구현) 협업 파이프라인
# 단계 승인은 스크립트 사이에 사람이 한다. relay 는 단계별 [y/N] 게이트 포함.
#
# 실행 스크립트 자체는 이 저장소 밖 $(PIPELINE_DIR)에 있다 — 저장소 안에 두면 오래된
# 브랜치를 checkout/merge할 때 git이 파일을 실제로 지워버리는 문제가 있어서 분리했다.
#
#   make plan "홈 헤더 문구 변경"   # Claude 기획 → HANDOFF.md
#   make impl                       # Codex 구현 + 빌드
#   make review                     # Claude 검토 → HANDOFF.md (수동 후처리)
#   make review-auto                # Claude 검토 + APPROVE 시 커밋·아카이브·PR 자동
#   make relay "..."                # 위 3단계 + 단계별 승인 게이트
#   RESUME=1 make impl              # 리뷰 반영 재구현(직전 Codex 맥락 유지)
#
#   make queue                      # QUEUE.md 를 순서대로 무인 처리(기획→구현→검토→PR 반복)
#                                    # PR 생성까지만 자동, 머지는 절대 자동으로 안 함
#   make queue-bg                   # 위를 백그라운드(nohup)로 실행 — 자기 전에 이걸로
#   MAX_ITEMS=5 make queue-bg       # 한 번 실행에 처리할 최대 건수 조정(기본 3)

PIPELINE_DIR ?= $(HOME)/StudioProjects/rodi-pipeline

_INTENT := $(or $(INTENT),$(filter-out plan impl review review-auto relay queue queue-bg,$(MAKECMDGOALS)))

.PHONY: plan impl review review-auto relay queue queue-bg

plan:
	@$(PIPELINE_DIR)/plan.sh "$(_INTENT)"

impl:
	@$(PIPELINE_DIR)/impl.sh

review:
	@$(PIPELINE_DIR)/review.sh

review-auto:
	@$(PIPELINE_DIR)/review.sh --auto

relay:
	@$(PIPELINE_DIR)/relay.sh "$(_INTENT)"

queue:
	@$(PIPELINE_DIR)/queue-relay.sh

queue-bg:
	@mkdir -p docs/handoff
	@nohup $(PIPELINE_DIR)/queue-relay.sh > docs/handoff/queue-run.log 2>&1 < /dev/null &
	@echo "▶ 백그라운드 실행 시작. 로그: docs/handoff/queue-run.log"
	@echo "  진행 확인: tail -f docs/handoff/queue-run.log"

%:
	@:
