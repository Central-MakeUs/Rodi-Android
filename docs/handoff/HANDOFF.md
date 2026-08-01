# HANDOFF — Current Task

Status: IMPL_DONE
Task: 검색어 연관·지역 결과 흐름
Branch: feat/search
Base: origin/develop
Risk: 현재 장소 검색 API는 호출만으로 최근 검색어를 저장하며, 전국 시군구 구청 좌표 데이터는 아직 제공되지 않았다.

## Context

## Goal

## Spec

## Acceptance

## Expected Files

## Verification

## Out of Scope

## Implementation Result

- 최근 검색어를 최대 15개로 제한하고, 비어 있으면 제목·목록 없이 검색창만 표시한다.
- 입력 후 300ms 디바운스로 지역 제안(현위치 기준 최대 4개)과 장소 목록을 함께 표시한다. 지역을 먼저 표시하고 장소 목록 앞에 구분선을 둔다.
- 지역·장소 대상 정보를 최근 검색어 DTO/도메인 모델에 추가했다. 장소는 상세 진입, 지역은 구청 좌표 카메라 이동과 홈 부분 목록으로 연결한다.
- Figma 추출 위치 핀 자산과 최근/연관/두 Empty/선택 지역 검색바 Preview를 추가했다.
- 구청 좌표 resolver는 서버 좌표가 제공될 때 대체 가능한 경계를 두고, 현재 Figma 검증 대상 4개 중구를 포함한다.
- 검색 화면은 Scaffold의 중복 시스템 inset을 제거해 홈 검색바와 동일하게 상태바 아래 5dp에서 시작한다.
- 제공된 전국 지역명을 로컬 resolver에 추가하고, 각 시군구청 검색 결과 좌표를 정적 테이블로 저장했다. 선택한 지역은 해당 청사 좌표로 홈 지도 카메라를 이동한다.
- 시군구 경계 크기에 따라 8~13 사이의 지도 줌 레벨을 지역별로 저장하고, 지역 선택 시 해당 레벨로 카메라를 이동한다.
- 최근 검색어 영역을 스크롤 가능한 목록으로 전환하고, 장소 최근 검색어(`PLACE` 또는 `placeId` 보유)는 핀 아이콘, 나머지는 돋보기 아이콘으로 표시한다.
- 지역 선택 시 검색 화면에서 이미 조회한 첫 장소 목록을 홈으로 전달해 바텀시트에 즉시 표시한다. 이후 지도 카메라 이동이 끝나면 기존 viewport 조회 결과로 목록을 갱신한다.

## Review

- `./gradlew :feature:home:testDebugUnitTest :app:testDebugUnitTest assembleDebug` GREEN
- `git diff --check` GREEN
- 에뮬레이터에서 홈 검색바 → 최근 검색어 화면 전환 확인. 에뮬레이터 하드웨어 키보드 설정으로 소프트 키보드 캡처는 미확인.
- 에뮬레이터 캡처에서 홈·검색 화면 검색바의 상단 여백이 동일함을 확인했다.
- 시군구별로 서로 다른 구청 좌표를 반환하는 resolver 단위 테스트를 추가했다. 최종 앱 탭 흐름 재검증은 연결된 에뮬레이터가 종료되어 실행하지 못했다.
- 시군구 규모별 줌 레벨(종로구 12, 성남시 11, 홍천군 9)을 resolver 단위 테스트로 검증했다.
- `./gradlew :feature:home:testDebugUnitTest :app:testDebugUnitTest :app:assembleDebug` GREEN
- `git diff --check` GREEN

## Review Triage

## Revision Plan

## Revision Result

## Final Review
