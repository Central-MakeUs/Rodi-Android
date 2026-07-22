# HANDOFF — Rodi 1.1.0-alpha04 출시 필수 수정

Status: BLOCKED
Branch: release/1.1.0-alpha04
PR: none

## Spec

- 홈 지도는 사용자의 미세한 이동·확대·축소에도 재검색 버튼을 표시하고, 서버 검색 성공 전까지 유지한다.
- 축척은 현위치 버튼 왼쪽에 배치한다. 초기 위치 확정 전에는 기본 서울 좌표의 장소를 노출하지 않는다.
- 지도 좌표·클러스터·장소 상세은 서버 데이터만 사용하며 기존 샘플 캐시를 제거한다.
- 온보딩 분석 문장과 레벨별 추천은 최신 Notion 정책으로 단일화한다.
- 로그인 응답의 서버 닉네임과 탈퇴 유예 계정 복구 흐름을 연결한다.
- 마이페이지 조회·운전 목표 수정·저장한 장소 목록 API를 실제 화면에 연결한다.
- 기존 empty 시트 하강 동작을 보존하고 버전을 `1.1.0-alpha04`/code 5로 변경한다.
- 새 6구간 운전 기간 wire enum은 서버 OpenAPI가 제공한 값만 사용하며, 미반영 시 출시를 차단한다.

## Alpha04 implementation

- 지도 제스처가 시작되면 변화량과 무관하게 재검색 상태를 유지하고, 사용자가 누른 재검색의 서버 갱신이 성공한 경우에만 해제한다. 현위치·초기 위치·클러스터 이동은 이 상태를 만들지 않는다.
- 축척을 `BOTTOM | RIGHT`, 우측 60dp에 배치했다. 위치 준비 전에는 서울 기본 좌표로 검색하거나 마커를 노출하지 않고, 위치 실패 후 사용자가 직접 이동해 재검색하는 흐름은 유지했다.
- 샘플 장소 코드와 테스트를 제거하고 기존 음수 ID 캐시를 삭제한다. 좌표 캐시는 고유 서버 ID 응답으로 트랜잭션 교체하며 클러스터 수 역시 고유 장소 ID를 센다.
- 온보딩 계산 결과를 `score + level`로 모델링하고, 점수 대신 계산된 `level`만 전송한다. 분석 문장과 온보딩·마이페이지 추천은 공통 도메인 정책으로 통합했다.
- 로그인 응답의 상태·토큰·신규 회원 여부·서버 닉네임을 보존하고, 빈 닉네임을 오류로 처리한다. 탈퇴 유예 복구 확인·취소·실패 및 복구 후 기존 회원 이동을 연결했다.
- 둘러보기 온보딩 완료본은 `pending`으로 보존하되 신규 회원 로그인이 확인된 뒤에만 서버 전송을 허용한다. 로그인 직후 전송 실패는 로그인 성공을 취소하지 않으며 앱 시작·포그라운드 복귀 때 재시도한다. 기존 회원 로그인에는 둘러보기 답변을 덮어쓰지 않는다.
- 클러스터는 고정 전국 격자 대신 SDK viewport와 지도 패딩으로 계산한 실제 노출 영역 안의 고유 서버 장소 ID만 한 번씩 포함한다. 56dp 반경의 화면 좌표 클러스터가 연쇄적으로 과대 확장되지 않게 제한했고, 선택 시 부모의 전체 ID 집합을 유지한 채 모든 좌표가 들어오는 bounds로 이동한다.
- 마이페이지 조회와 목표 부분 수정, 코스·주차장 통합 저장 장소 커서 목록을 화면에 연결했다. nullable 계약, 첫 페이지 총 개수, 타입·ID 중복 제거와 페이지별 재시도를 반영했다.
- empty/error 시트의 아래 방향 숨김 동작을 보존했고 `versionCode=5`, `versionName=1.1.0-alpha04`를 적용했다.
- 릴리스 워크플로를 Java 21로 맞추고 키스토어·태그 버전·APK/AAB 서명 검증, 테스트·lint, 수동 실행 산출물 업로드를 추가했다.

## API audit (2026-07-23)

- 서버 OpenAPI의 인증·회원·온보딩·장소 11개 경로를 Retrofit 선언과 대조했으며 미연결 엔드포인트는 없다.
- `GET/PATCH /api/v1/members/me`, `GET /api/v1/places/bookmarks`, 로그인 닉네임, `WITHDRAWAL_PENDING` 복구 경로가 연결됐다.
- 서버 `drivingPeriod`는 아직 `UNDER_1_MONTH`, `MONTHS_1_3`, `MONTHS_3_6`, `MONTHS_6_12`, `YEARS_1_2`, `YEARS_2_10`, `OVER_10_YEARS`의 구형 7개 enum이다. 승인된 새 6구간 wire value가 아니므로 이름을 추정하지 않았고 alpha04 출시는 차단한다.

## Codex Result — alpha04

- Changed files: `.github/workflows/release.yml`; `app/build.gradle.kts`, `app/src/main/java/com/dororong/rodi/ui/{MainScreen,RodiApp,RodiAppViewModel}.kt`와 app tests; `core/domain`의 auth/member/onboarding/place 모델·저장소·use case·tests; `core/data`의 auth/member/place API·DTO·mapper·repository·DataStore·Room cache·tests; `core/ui/.../AccountRecoveryDialog.kt`; `feature/auth`, `feature/entry`, `feature/home`, `feature/mypage` 화면·ViewModel·Contract·tests; `docs/PROJECT.md`, `docs/handoff/HANDOFF.md`; 삭제 `core/data/.../SamplePlaces.kt`, `SamplePlacesTest.kt`
- Build/test: `git diff --check` GREEN; `./gradlew :feature:home:testDebugUnitTest` GREEN; 최종 결합 트리 `./gradlew test lint assembleDebug assembleRelease bundleRelease` GREEN (778 tasks); debug APK emulator install·cold launch·프로세스 유지 GREEN
- Open questions: 서버가 새 6구간 `drivingPeriod` wire enum을 공개해야 기간 선택지·매핑·전송 테스트를 완료하고 Status를 `IMPL_DONE`으로 변경할 수 있다. Navigator 이미지 확정, GitHub `KEYSTORE_BASE64` secret 교체 후 workflow 수동 실행, 지도 제스처·위치 지연/거절·서버 고유 ID 대비 클러스터 합계·축척 간격·온보딩 Figma 대조·계정 복구·empty/normal 시트 실기기 QA가 남아 있다.

## Previous alpha03 record

Status: IMPL_DONE
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

## Follow-up — 빈 상태 시트 단방향 드래그

Status: IMPL_DONE

- 빈 결과·초기 오류 시트는 Material 양방향 swipe를 사용하지 않는다.
- 핸들 행을 아래로 12dp 이상 드래그할 때만 시트를 숨기며, 위 방향 드래그는 소비하고 전체 화면 전환도 차단한다.
- Changed files: `feature/home/src/main/java/com/dororong/rodi/feature/home/HomeScreen.kt`, `feature/home/src/main/java/com/dororong/rodi/feature/home/list/components/PlaceEmptyContent.kt`
- Build/test: `git diff --check` GREEN; `./gradlew :feature:home:testDebugUnitTest assembleDebug` GREEN; debug APK emulator install GREEN
- Open questions: none
