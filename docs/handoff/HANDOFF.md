# HANDOFF — Map grid clustering technical spike

> Claude(기획)와 Codex(구현)가 주고받는 **단일 활성 작업 채널**.
> 완료되면 `docs/handoff/archive/<날짜>-<작업>.md`로 옮기고 이 파일은 다음 작업으로 비운다.

Status: IMPL_DONE       <!-- PLANNING | READY_FOR_IMPL | IMPL_DONE | IN_REVIEW | DONE | BLOCKED -->
Branch: codex/spike-map-clustering

## Context (왜)
전국 범위에서는 고정 그리드, 지역 범위에서는 화면 거리 기반으로 코스 마커 겹침을 막는 2단 클러스터링을 기술 검증한다.
실서버 계약 확정 전이므로 기존 로컬 더미 데이터로 BBox 조회 경계와 클릭 확대 UX를 검증한다.

## Spec (무엇을·어떻게)
- 기본 줌은 13, 최소 줌은 7로 제한한다.
- CLUSTER LAB의 줌 7 버튼은 전국 중심으로 이동한다. 줌 7~10은 화면 BBox와 무관한 고정 대한민국 범위(NE 39.3, 131.8 / SW 32.7, 124.4)를 한 번 조회·캐시해 3열×5행 클러스터를 재사용한다.
- 줌 11~13은 현재 전체 MapView를 4열×6행으로 분할해 클러스터링한다.
- 줌 14 이상은 개별 마커를 표시한다.
- 줌 11~13은 56dp 화면 반경 안의 연결된 마커를 하나로 묶어, 카운트·단건 마커 사이의 최소 간격을 보장한다.
- 지역 클러스터에 한 건만 있으면 숫자 클러스터 대신 코스 출발 또는 주차장 개별 마커를 표시한다.
- 숫자 클러스터는 멤버 평균 좌표가 아닌 평균에 가장 가까운 실제 출발지에 표시한다.
- 전국 클러스터 클릭은 중심 좌표에서 줌 11, 지역 클러스터 클릭은 줌 14로 이동한다.
- 회전·기울기 제스처를 비활성화한다.
- 카메라 이동 종료 후 전체 MapView 기준 우상단과 좌하단을 `fromScreenPoint()`로 변환한다.
- `MapViewportQuery(northEast, southWest, zoomLevel)` 경계를 통해 데이터를 조회한다.
- 조회는 300ms debounce, 이전 요청 취소, 동일 쿼리 중복 방지를 적용한다.
- 지역 클러스터(줌 11~13)에서 사용자가 지도를 이동하면 마지막 조회 viewport의 가로·세로 반 화면 이동량을 기준으로 재검색 버튼을 표시한다. 버튼을 누르기 전까지 지역 조회와 클러스터 갱신을 보류하며, 탭 시 최신 viewport를 조회한다.
- 실서버 API 대신 기존 `SampleCourses.RODI_COURSES`와 서울권 N-퀸 배치 40건을 범위 필터링하는 로컬 데이터 소스를 사용한다.
- 탐색 클러스터·개별 마커·선택 경로 레이어를 분리한다.
- CLUSTER LAB 패널에 줌, 모드, 전국 그리드 또는 지역 반경, NE/SW, 조회·클러스터 수와 줌 이동 버튼을 제공한다.
- 스파이크 검증 중에는 바텀시트·지도 하단 padding을 제거해 전체 지도를 표시한다.

## Files to touch
- `core/domain/src/main/kotlin/com/dororong/rodi/core/domain/`
- `core/data/src/main/java/com/dororong/rodi/core/data/`
- `feature/home/src/main/java/com/dororong/rodi/feature/home/`
- 관련 모듈 테스트
- `docs/handoff/HANDOFF.md`
- `docs/verification/clustering-spike.md`

## Acceptance criteria
- [x] 앱 최초 진입 기본 줌이 13이다.
- [x] 지도는 줌 7보다 축소되지 않고 회전·기울기 제스처가 동작하지 않는다.
- [x] 고정 대한민국 범위의 전국 데이터를 한 번만 조회·캐시한다.
- [x] 줌 7의 3×5 기준판·클러스터 멤버·개수를 한 번만 계산하고 줌 10까지 유지한다.
- [x] 줌 11~13은 56dp 화면 거리 기반 클러스터, 줌 14 이상은 개별 마커다.
- [x] 클러스터 셀 한 건은 개별 코스·주차장 마커로 표시된다.
- [x] 숫자 클러스터는 실제 멤버 대표 지점에 표시된다.
- [x] 전국·지역 클러스터 클릭 시 각각 줌 11·14로 이동한다.
- [x] BBox는 바텀시트와 무관하게 전체 MapView의 NE/SW 두 점으로 구성된다.
- [x] 최신 viewport 요청만 UI에 반영되고 동일 쿼리는 중복 조회하지 않는다.
- [x] 지역 클러스터에서 반 화면 이상 이동 시 `현 지도에서 검색` 버튼이 150ms 페이드인하며, 탭 전까지 조회·클러스터 갱신을 보류한다.
- [x] 바텀시트 없이 전체 지도가 보인다.
- [x] 줌별 스크린샷과 전국→지역→개별 마커 녹화를 남긴다.
- [x] 관련 테스트와 debug build가 성공한다.

## Verification
```bash
git diff --check
./gradlew :core:domain:test
./gradlew :core:data:testDebugUnitTest
./gradlew :feature:home:testDebugUnitTest
./gradlew assembleDebug
```

## Out of scope
- 실제 서버 Retrofit 엔드포인트 연결
- 클러스터 최종 디자인
- Kakao Maps SDK 업그레이드
- spike 브랜치 push·PR·merge

---
## Codex Result
- Changed files: `feature/home` 단건 개별 핀·실제 대표 지점 숫자 클러스터 렌더링, 현 지도 재검색 보류·페이드 버튼, 지역 화면 거리 기반 충돌 방지 클러스터, `core:data` 기존 더미 데이터와 서울권 N-퀸 배치 40건 BBox 연결, 관련 클러스터 테스트, `docs/verification/clustering-spike.md`
- Build/test: `git diff --check`, `:core:data:testDebugUnitTest`, `:feature:home:testDebugUnitTest`, `assembleDebug` GREEN; 재검색 버튼은 Emulator에서 노출·탭 후 숨김 확인. 거리 기반 클러스터의 새 기기 캡처는 Emulator 연결 해제로 보류
- Open questions: 실제 서버에서는 고정 대한민국 범위의 개별 코스 목록 대신 전국 그리드 집계 응답으로 대체할 수 있다.

---
## Claude Review
- Blocking:
- Nits:
- Verdict:
---
