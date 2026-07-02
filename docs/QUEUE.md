# QUEUE.md — 무인 자동화 큐 (scripts/queue-relay.sh)

> 형식: `브랜치명|요구사항|skip_plan(yes/no)|base_branch(생략 시 develop)|pr(yes/no, 생략 시 yes)`
> - skip_plan=yes 는 docs/handoff/HANDOFF.md 에 이미 READY_FOR_IMPL 스펙이 있는 경우
>   (기획 단계를 건너뛰고 바로 구현부터 시작).
> - base_branch: 이 브랜치가 아직 develop에 머지 안 된 다른 큐 항목에 의존하면, 그 브랜치명을
>   적는다 (PR은 자동 머지되지 않으므로 origin/develop만 보면 그 변경사항이 없다).
> - pr=no 는 APPROVE 후 커밋·아카이브까지만 하고 push/PR 생성은 생략한다
>   (사용자가 직접 확인해야 하는 작업 — 로컬 브랜치에 커밋만 남는다. 확인 후 직접 push/PR).
> - 처리된 항목은 지우거나 `#`으로 주석 처리한다. 위에서부터 순서대로 처리되고,
>   MAX_ITEMS 환경변수로 한 번 실행에 처리할 최대 건수를 제한한다.

# feat/hilt-di — 완료, PR #13 (https://github.com/Central-MakeUs/Rodi-Android/pull/13)
# feat/design-system-buttons — 완료, PR #14 (https://github.com/Central-MakeUs/Rodi-Android/pull/14)
# feat/domain-usecases — 완료, PR #15 (https://github.com/Central-MakeUs/Rodi-Android/pull/15)
# feat/network-db-skeleton — 완료, PR #16 (https://github.com/Central-MakeUs/Rodi-Android/pull/16)
feat/unit-tests-ci|현재 기능(Home/Entry, core:domain의 UseCase 계층)에 JUnit5 + MockK 기반 단위 테스트를 작성하고, GitHub Actions CI에 테스트 게이트를 추가한다. GetCoursesUseCase/GetRouteUseCase/runSuspendCatching(core:domain, core:common)과 HomeViewModel/EntryViewModel의 핵심 로직을 우선 커버한다. 테스트 작성 컨벤션(파일 위치, 네이밍, MockK 사용법)을 정리해 앞으로 로직 추가 시 재사용 가능하게 한다.|no|develop|yes
feat/snackbar-and-theme|docs/handoff/HANDOFF.md 스펙(커스텀 Snackbar + RodiTheme Semantic Colors 분리) 그대로 구현. 참고 프로젝트 경로는 샌드박스 밖이라 스펙에 이미 반영해뒀으니 접근 불필요.|yes|develop|no
