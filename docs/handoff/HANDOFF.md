# HANDOFF — Current Task

Status: REVISION_DONE
Task: 검색어 연관·지역 결과 흐름
Branch: feat/search
Base: origin/develop
Risk: 새 API의 지역 후보는 좌표 없이 지역명만 제공하므로, 지도 이동은 기존 정적 구청 좌표 resolver와의 정규화 일치에 의존한다.

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

- 수용: 연관검색·최근검색 DTO mapper 분리, 최근검색 인증 예외 경로 테스트, 검색 실패 뒤 non-blank Idle 화면 fallback, 지역 resolver Map 조회와 미매핑 테스트, 검색 이동 effect 단일 발생 테스트, 등록 실패 비차단 이동 테스트.
- 보류: 서버 지역명이 resolver 정규화와 다르다는 실제 계약·응답 근거가 없어 축약형 변환은 추가하지 않는다. 현재 OpenAPI 예시의 `서울특별시`는 정규화 처리한다.
- 기각: `REVISION_DONE`은 이 프로젝트의 AGENTS 상태 전이에 정의된 값이므로 외부 리뷰의 `IN_REVIEW`/`Claude Review` 형식 요구는 적용하지 않는다.

## Revision Plan

- `GET places/related-search` DTO·PlaceRepository·use case를 추가하고, SearchViewModel의 300ms 자동완성 및 다음 페이지 조회를 해당 API로 교체한다.
- 서버 지역 후보의 관련도순을 보존해 화면에 표시하고, 선택 시에만 로컬 resolver로 구청 좌표를 해석한다.
- 최근 검색어 POST DTO·repository·use case를 추가한다. IME는 입력어를 REGION으로, 지역/장소 후보 선택은 각각 REGION/PLACE와 placeId로 비차단 등록한다.
- 입력만으로 등록하지 않는 규칙, 등록 실패 비차단, 페이지네이션·서버 지역 후보 표시를 단위 테스트로 검증한다.
- 최근 검색어 등록 성공 뒤 목록을 조용히 다시 조회해, 지역 결과 Empty에서 입력을 지운 즉시 새 항목이 보이도록 한다.

## Revision Result

- `PlaceApi`와 `PlaceRepository`에 `related-search` 경계를 추가했고, 검색 화면의 디바운스·페이지네이션은 서버 지역·장소 후보를 사용한다.
- 로컬 resolver의 자동완성/거리 정렬을 제거하고, 서버 지역명에 대한 구청 좌표·줌 해석만 유지했다.
- 최근 검색어 POST를 추가했다. 타이핑은 등록하지 않으며, IME 입력은 REGION, 지역 후보·최근 지역은 REGION, 장소 후보·최근 장소는 PLACE와 placeId로 비차단 등록한다.
- 등록 실패는 이동/상세 진입을 막지 않는다.
- 등록 성공 뒤 최근 검색어를 다시 조회해, 지역 결과 Empty에서 입력을 지우면 방금 선택한 지역이 즉시 표시된다.
- 검증: `./gradlew --no-daemon :core:data:testDebugUnitTest :feature:home:testDebugUnitTest :app:assembleDebug --console=plain` BUILD SUCCESSFUL.
- 회귀 검증: `./gradlew --no-daemon :feature:home:testDebugUnitTest :app:assembleDebug --console=plain` BUILD SUCCESSFUL.
- 리뷰 수정 검증: `./gradlew --no-daemon :core:data:testDebugUnitTest :feature:home:testDebugUnitTest :app:assembleDebug --console=plain` BUILD SUCCESSFUL.
- 정적 검증: `git diff --check` GREEN.

## Final Review
