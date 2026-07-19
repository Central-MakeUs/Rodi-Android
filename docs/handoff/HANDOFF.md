# HANDOFF — alpha03 홈 지도·시트 회귀 수정

Status: IMPL_DONE
Branch: fix/home-1.1
PR: #47 (`release/1.1.0-alpha03` ← `fix/home-1.1`)

## Context

alpha03 홈 지도에서 현 위치보다 기본 viewport 조회가 먼저 실행되는 경합, 동일 좌표 샘플/서버 마커 중복, 홈·마이 전환 시 바텀 내비 재구성, 주차장 상세 시트의 고정 높이와 토글 재중앙 정렬을 수정한다.
PR #47의 주소 축약과 부분 목록 지도 패딩은 유지한다.

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

## Manual QA

- signed debug APK 설치 성공: `app/build/outputs/apk/debug/app-debug.apk`
- 현 위치 마커와 주변 결과가 같은 viewport에 표시되는 것을 확인했다.
- 지역 클러스터 클릭 후 줌 14 개별 주차장 마커 전환을 확인했다.
- 홈 → 마이 → 홈 양방향에서 바텀 내비가 한 개만 표시되고 선택 상태가 전환되는 것을 확인했다.
- 주차장 상세이 바텀 내비 위를 덮고, 영업시간 토글 전후 지도 마커 위치가 유지되며 긴 내용은 400dp 안에서 스크롤되는 것을 확인했다.
- 기기에서 별도 위치 지연·권한 거절, 전국 줌 6 클러스터, 30% 재검색 경계, 짧은 주차장 wrap은 자동화/코드 검증까지만 수행했다.

## Codex Result

- Changed files: `app/build.gradle.kts`, `app/src/main/java/com/dororong/rodi/ui/MainScreen.kt`, `core/data` 캐시 repository·샘플 정규화·tests, `feature/home` HomeScreen·location·map pending search·parking layout state·tests, `docs/handoff/HANDOFF.md`, 이전 HANDOFF archive
- Build/test: `git diff --check` GREEN; `./gradlew :core:domain:test :core:data:testDebugUnitTest :feature:home:testDebugUnitTest lint assembleDebug assembleRelease` GREEN; debug APK emulator install GREEN
- Open questions: 위치 지연·권한 거절과 전국/30% 경계는 실제 기기 수동 QA가 추가로 필요하다.
