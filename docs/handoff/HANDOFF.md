# HANDOFF — alpha03 홈 지도·시트 및 복구 작업 통합

Status: IMPL_DONE
Branch: fix/home-1.1
PR: #47 (`release/1.1.0-alpha03` ← `fix/home-1.1`)

## Context

alpha03 홈 지도에서 현 위치보다 기본 viewport 조회가 먼저 실행되는 경합, 동일 좌표 샘플/서버 마커 중복, 홈·마이 전환 시 바텀 내비 재구성, 주차장 상세 시트의 고정 높이와 토글 재중앙 정렬을 수정한다.
PR #47의 주소 축약과 부분 목록 지도 패딩을 유지하고, 별도 워크트리에 남아 있던 온보딩 분석 자산과 완료 상태 수정을 최신 `origin/develop` 기준으로 통합한다.

## Implemented

- 현 위치는 권한 허용 후 최대 5초간 기다리고, 성공 시 목표 카메라 이동이 끝난 뒤 최초 조회한다.
- 카메라 자동 검색은 목표 좌표·줌·세대·이동 사유를 보관하며, viewport 중심이 목표점에서 가로·세로 span의 5% 이내일 때만 소비한다.
- 새 자동 이동이 이전 pending 검색을 대체하고, 사용자 제스처는 pending 검색을 취소한다. ViewModel의 요청 세대와 Job 취소가 최신 조회 결과만 반영한다.
- 위치 거절·타임아웃은 기본 viewport로 폴백한다. 늦은 위치는 사용자가 지도를 움직이거나 별도 viewport를 선택하지 않았을 때만 자동 이동·재조회한다.
- KakaoMap 최소 줌을 6으로 고정하고 회전·회전 줌·기울기 제스처를 비활성화했다.
- 동일 타입·동일 좌표에 서버 양수 ID가 있으면 샘플 음수 ID만 제외한다. 서로 다른 좌표와 서버끼리의 동일 좌표는 유지한다.
- 좌표·목록 캐시 단계에서 같은 정규화를 적용해 전국/지역 클러스터, 개별 마커, 목록이 동일한 데이터를 사용한다.
- `MainScreen`이 하나의 `movableContentOf` 바텀 내비를 소유하고 홈에서는 시트 아래, 마이에서는 상위 레이어로 이동 배치한다.
- 주차장 상세은 `heightIn(max = 400.dp)`와 내부 스크롤을 사용한다. 장소별 최초 유효 높이만 지도 패딩으로 고정하고 토글 높이 변화는 지도 중심에 반영하지 않는다.
- `versionCode 4`, `versionName 1.1.0-alpha03`을 반영했다.
- 온보딩 분석 GIF를 680×600 고해상도 자산으로 교체하고 기존 GIF 재생 방식을 유지한 채 180dp 폭으로 표시한다.
- 온보딩 분석 제출 성공 시 `PRECAUTIONS` 진행 상태를 결과 노출 전에 저장한다. 결과 확인이나 앱 재시작 후 설문이 다시 노출·제출되지 않으며 주의사항 단계에서는 이전 설문으로 돌아가지 않는다.
- 최신 `origin/develop`을 PR 브랜치에 병합해 마이페이지·약관 등 이후 반영 사항이 PR 최종 트리에서 빠지지 않도록 했다.
- 온보딩은 투명 배경 `illust_level_*`, 마이페이지는 배경 포함 `illust_profile_*` 자산을 각각 사용하는 것을 확인했다.

## Recovery audit

- 유지: 현재 GIF 방식과 고해상도 GIF 자산, 온보딩 완료 진행 상태 보존, 최신 develop의 레벨 이미지·약관 자산.
- 제외: Media3/WebM 재생안과 WebM preview 이미지는 현재 GIF 유지 요청과 충돌해 적용하지 않았다. 원본 `codex/terms-backstack-reset` 워크트리 변경은 삭제하지 않았다.
- 제외: 오래된 코스 상세 간격 변경은 이후 닫기 버튼·상세 시트 디자인 수정과 겹쳐 적용하지 않았다.
- 제외: 홈 stash 2건과 `feat/home-1.1-fix`의 미병합 변경은 현재 홈 구조 및 PR #41–46에서 대체되었으므로 통째 적용하지 않았다. stash와 워크트리는 보존했다.
- 제외: detached 워크트리의 `RodiApplication`·마이페이지 배경 변경은 최신 develop에 동등하거나 이후 구현이 존재해 적용하지 않았다. 해당 워크트리도 보존했다.

## Manual QA

- signed debug APK 설치 성공: `app/build/outputs/apk/debug/app-debug.apk`
- 현 위치 마커와 주변 결과가 같은 viewport에 표시되는 것을 확인했다.
- 지역 클러스터 클릭 후 줌 14 개별 주차장 마커 전환을 확인했다.
- 홈 → 마이 → 홈 양방향에서 바텀 내비가 한 개만 표시되고 선택 상태가 전환되는 것을 확인했다.
- 주차장 상세이 바텀 내비 위를 덮고, 영업시간 토글 전후 지도 마커 위치가 유지되며 긴 내용은 400dp 안에서 스크롤되는 것을 확인했다.
- 온보딩·마이페이지 레벨 이미지 파일과 코드 매핑을 대조해 투명/배경 포함 자산이 화면별로 분리된 것을 확인했다.
- 기기에서 별도 위치 지연·권한 거절, 전국 줌 6 클러스터, 30% 재검색 경계, 짧은 주차장 wrap은 자동화/코드 검증까지만 수행했다.
- 고해상도 GIF와 분석 완료 후 뒤로가기 흐름은 단위 테스트·빌드까지만 검증했으며 기기 수동 QA가 추가로 필요하다.

## Codex Result

- Changed files: `app/build.gradle.kts`, `app/src/main/java/com/dororong/rodi/ui/MainScreen.kt`, `core/data` 캐시 repository·샘플 정규화·tests, `feature/home` HomeScreen·location·map pending search·parking layout state·tests, `feature/entry` 분석 GIF·dialog·flow·ViewModel·주의사항 content·tests, `docs/handoff/HANDOFF.md`, 이전 HANDOFF archive
- Build/test: `git diff --check` GREEN; `./gradlew :core:domain:test :core:data:testDebugUnitTest :feature:entry:testDebugUnitTest :feature:home:testDebugUnitTest lint assembleDebug assembleRelease` GREEN; debug APK emulator install GREEN
- Open questions: 위치 지연·권한 거절과 전국/30% 경계, 고해상도 GIF 및 분석 완료 후 뒤로가기 흐름은 실제 기기 수동 QA가 추가로 필요하다.
