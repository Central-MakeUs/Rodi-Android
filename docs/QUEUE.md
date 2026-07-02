# QUEUE.md — 무인 자동화 큐 (scripts/queue-relay.sh)

> 형식: `브랜치명|요구사항|skip_plan(yes/no)|base_branch(생략 시 develop)`
> - skip_plan=yes 는 docs/handoff/HANDOFF.md 에 이미 READY_FOR_IMPL 스펙이 있는 경우
>   (기획 단계를 건너뛰고 바로 구현부터 시작).
> - base_branch: 이 브랜치가 아직 develop에 머지 안 된 다른 큐 항목에 의존하면, 그 브랜치명을
>   적는다 (PR은 자동 머지되지 않으므로 origin/develop만 보면 그 변경사항이 없다).
> - 처리된 항목은 지우거나 `#`으로 주석 처리한다. 위에서부터 순서대로 처리되고,
>   MAX_ITEMS 환경변수로 한 번 실행에 처리할 최대 건수를 제한한다.

# feat/hilt-di — 완료, PR #13 (https://github.com/Central-MakeUs/Rodi-Android/pull/13)
# feat/design-system-buttons — 완료, PR #14 (https://github.com/Central-MakeUs/Rodi-Android/pull/14)
feat/domain-usecases|core:domain에 UseCase 계층을 도입한다. runCatching 대신 CancellationException을 보존하는 runSuspendCatching 유틸을 core:common(또는 core:domain)에 추가하고, feat/hilt-di에서 만든 Repository 인터페이스를 감싸는 UseCase(예: GetCoursesUseCase, GetRouteUseCase)를 core:domain에 만들어 HomeViewModel이 Repository 대신 UseCase를 주입받도록 리팩터링한다.|yes|feat/hilt-di
