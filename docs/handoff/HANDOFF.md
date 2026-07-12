# HANDOFF — Map grid clustering technical spike

> Claude(기획)와 Codex(구현)가 주고받는 **단일 활성 작업 채널**.
> 완료되면 `docs/handoff/archive/<날짜>-<작업>.md`로 옮기고 이 파일은 다음 작업으로 비운다.

Status: IMPL_DONE       <!-- PLANNING | READY_FOR_IMPL | IMPL_DONE | IN_REVIEW | DONE | BLOCKED -->
Branch: codex/spike-map-clustering

## Context (왜)
전국·지역 범위에서 코스 마커가 겹치지 않도록 화면 그리드 기반 2단 클러스터링을 기술 검증한다.
실서버 계약 확정 전이므로 합성 데이터로 BBox 조회 경계와 클릭 확대 UX를 검증한다.

## Spec (무엇을·어떻게)
- 기본 줌은 13, 최소 줌은 7로 제한한다.
- CLUSTER LAB의 줌 7 버튼은 전국 중심으로 이동한다. 줌 7~10은 화면 BBox와 무관한 고정 대한민국 범위(NE 39.3, 131.8 / SW 32.7, 124.4)를 한 번 조회·캐시해 3열×5행 클러스터를 재사용한다.
- 줌 11~13은 현재 전체 MapView를 4열×6행으로 분할해 클러스터링한다.
- 줌 14 이상은 개별 마커를 표시한다.
- 전국 클러스터 클릭은 중심 좌표에서 줌 11, 지역 클러스터 클릭은 줌 14로 이동한다.
- 회전·기울기 제스처를 비활성화한다.
- 카메라 이동 종료 후 전체 MapView 기준 우상단과 좌하단을 `fromScreenPoint()`로 변환한다.
- `MapViewportQuery(northEast, southWest, zoomLevel)` 경계를 통해 데이터를 조회한다.
- 조회는 300ms debounce, 이전 요청 취소, 동일 쿼리 중복 방지를 적용한다.
- 실서버 API 대신 고정 시드 약 300개의 합성 데이터를 범위 필터링하는 로컬 데이터 소스를 사용한다.
- 탐색 클러스터·개별 마커·선택 경로 레이어를 분리한다.
- CLUSTER LAB 패널에 줌, 모드, 그리드, NE/SW, 조회·클러스터 수와 줌 이동 버튼을 제공한다.
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
- [x] 줌 11~13은 4×6, 줌 14 이상은 개별 마커다.
- [x] 전국·지역 클러스터 클릭 시 각각 줌 11·14로 이동한다.
- [x] BBox는 바텀시트와 무관하게 전체 MapView의 NE/SW 두 점으로 구성된다.
- [x] 최신 viewport 요청만 UI에 반영되고 동일 쿼리는 중복 조회하지 않는다.
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
- Changed files: `feature/home` 고정 대한민국 범위 캐시·전국 그리드 스냅샷·재렌더 방지, 관련 ViewModel/클러스터 테스트, `docs/verification/clustering-spike.md`
- Build/test: `git diff --check`, `:core:domain:test`, `:core:data:testDebugUnitTest`, `:feature:home:testDebugUnitTest`, `assembleDebug` GREEN
- Open questions: 실제 서버에서는 고정 대한민국 범위의 개별 코스 목록 대신 전국 그리드 집계 응답으로 대체할 수 있다.

---
## Claude Review
- Blocking:
- Nits:
- Verdict:
---
