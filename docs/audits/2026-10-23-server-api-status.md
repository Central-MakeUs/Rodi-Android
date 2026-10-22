# 서버 API 현황과 Android 변경 필요 사항 — 2026-10-23

온보딩 `없음` 복수 선택은 현재 서버 검증과 충돌하므로 우선 수정이 필요함
지도 클러스터 API와 필터·정렬 분리도 서버에 반영됐지만 Android는 기존 방식을 사용 중임
폐기 예정 API 두 개는 호환 경로가 남아 있어 당장 전체 앱이 중단된 상태로 해석하지 않음

## 조사 기준과 확인 범위

- Android: `/Users/uihyeon/StudioProjects/ROUTI`, `develop`, `2c705231`
- `git fetch origin --prune` 후 `HEAD`와 `origin/develop` 차이 0/0 확인, 조사 시작 시 작업 트리 깨끗함
- 서버: [Central-MakeUs/Rodi-Server](https://github.com/Central-MakeUs/Rodi-Server), 기본 브랜치 `develop`, 조회한 최신 커밋 `f42a9993`
- 배포 계약: [라이브 OpenAPI](https://api.stillstar.store/v3/api-docs), `Rodi API v1`
- 서버 소스·스펙은 `f42a9993e80131203bf66108e2c48b0da372c24b`에 고정해 확인함
- 정적 대조와 공개 GET 응답을 확인했으며, 인증 계정으로 온보딩·닉네임·신고·방문 등을 쓰는 요청과 기기 QA는 실행하지 않음
- 라이브 명세에 배포 Git SHA가 없으므로 서버 저장소 최신 SHA와 실행 중인 바이너리가 완전히 같다고 단정하지 않음
- 이 문서는 조사 스냅샷이며 구현 스펙이나 승인 판정이 아님
- [OpenAPI·공개 GET 원본과 SHA-256 기록](/Users/uihyeon/.codex/visualizations/2026/10/22/01a16429-9b1c-7b13-8690-4f4b78c14451/rodi-api-20261023/manifest.json)을 로컬 증거로 보존함

## 1. 서버에서 바뀐 주요 내용

조회한 서버 `develop`의 최근 큰 변경은 10월 15일의 다음 세 건임

| 서버 변경 | 현재 서버 계약 | Android 상태 |
|---|---|---|
| [#77 필터·정렬 분리와 온보딩 보완](https://github.com/Central-MakeUs/Rodi-Server/pull/77) | 저장 태그는 정렬용 `sortTags`, 실제 필터는 요청의 `practiceTypes` | 옛 저장 경로만 사용하며 실제 필터 파라미터 미전송 |
| 같은 #77의 온보딩 폼·검증 | 온보딩 폼 GET 추가, `NONE` 배타·중복 검증 | 문항은 로컬 정의, `NONE`과 다른 선택지 동시 선택 가능 |
| [#78 닉네임 변경·검증](https://github.com/Central-MakeUs/Rodi-Server/pull/78) | 형식·금칙어·중복 판정과 PATCH, 공백→`_` V28 마이그레이션 | 기본 수정·검증 API 연결 있음, 30일 제한 안내는 서버 정책과 충돌 |
| [#79 서버 지도 클러스터](https://github.com/Central-MakeUs/Rodi-Server/pull/79) | 범위 내 장소 + 확대 단계별 클러스터 일괄 반환 | 전체 좌표를 받아 앱에서 직접 묶는 기존 구현 사용 |

8월 변경인 코스 등록·승인·튜토리얼, 방문 인증·레벨 진행도, 후기 편집 등도 현재 명세에 존재하며 Android에 관련 인터페이스와 모델이 있음
이번에 새로 발견한 누락과 이미 연결한 기능을 구분해야 함

## 2. 수정·전환 우선순위

| 순서 | 항목 | 판단과 사용자 영향 |
|---|---|---|
| 1 | 온보딩 `NONE` 배타 선택 | **필수 수정** — 현재 앱이 만들 수 있는 조합을 서버가 400으로 거절함 |
| 2 | 필터와 정렬 상태 분리 | **새 필터 기능 반영에 필수** — 현재 필터 UI는 목록 순서만 바꾸며 비매칭 장소를 제거하지 못함 |
| 3 | 지도 `/places/clusters` 전환 | **폐기 경로 제거 전 필수** — URL 교체만으로 끝나지 않고 지도 상태·렌더링·캐시를 함께 바꿔야 함 |
| 4 | 연습기록 `isDeleted` 반영 | **기존 계약 누락 수정** — 삭제 코스의 후기 작성 버튼을 활성 상태로 둘 수 있음 |
| 결정 필요 | 닉네임 30일 제한 | **명세 충돌** — Android 안내와 서버의 명시적 정책이 다름 |
| 후속 | 온보딩 폼 GET 연결 | 문구·문항 흐름을 서버에서 관리하려면 필요하나, 미연결 자체가 제출 API 중단을 뜻하지 않음 |

### 2-1. 온보딩 제출 — 가장 먼저 수정할 항목

- API: `POST /api/v1/members/me/onboarding`
- 서버는 `roadExperiences: ["NONE", "SOLO"]`처럼 `NONE`과 다른 선택지를 함께 보내면 400 `COMMON_400`으로 거절함
- `["SOLO", "SOLO"]` 같은 중복 값도 거절하며, `NONE` 단독·빈 배열·생략은 허용함
- 현재 `EntryViewModel.toggleRoadExperience()`는 항목 추가·제거만 수행함
- `CareerContent`의 복수 선택 UI도 선택값을 그대로 ViewModel로 전달하며, `OnboardingMapper`는 선택 목록을 그대로 서버 값으로 변환함
- 따라서 운전 경력 후속 질문이 나오는 조건에서 `없음` → `혼자 연습`을 선택한 뒤 제출하면 계약에 어긋나는 요청이 만들어짐
- 위 흐름은 소스로 확인했으며 실제 계정의 POST 거절과 기기 화면은 미검증임

수정 시에는 `없음` 선택 시 기존 선택 해제, 다른 선택 시 `없음` 해제, 이미 로컬에 저장된 잘못된 조합의 복원·재전송도 함께 다뤄야 함

근거: [서버 스펙 019](https://github.com/Central-MakeUs/Rodi-Server/blob/f42a9993e80131203bf66108e2c48b0da372c24b/docs/specs/019-refinements-1.md), `feature/entry/.../EntryViewModel.kt:129`, `content/CareerContent.kt:87`, `core/data/.../mapper/OnboardingMapper.kt:14`

### 2-2. 필터·정렬 — 서로 다른 두 동작으로 분리됨

| 목적 | 새 계약 | 의미 |
|---|---|---|
| 정렬 기준 저장 | `PUT /members/me/sort-tags`, `{ "sortTags": [...] }` | 일치하는 장소가 먼저, 나머지도 계속 노출 |
| 실제 유형 필터 | 목록·검색·마커 요청의 `practiceTypes` | 일치하는 장소만 반환, `totalCount`도 필터 반영 |
| 옛 저장 경로 | `PUT /members/me/filter-tags`, `{ "filterTags": [...] }` | 새 정렬 저장의 별칭, deprecated, 앱 전환 뒤 제거 예정 |

- 현재 `MemberApi`와 `MemberRepositoryImpl`은 옛 `filter-tags`를 사용함
- `HomeViewModel.applyFilter()`는 로그인 확인 → 서버 태그 저장 → 목록 재조회로 이어짐
- `PlaceApi.getPlaces()`·`searchPlaces()`·`getCoordinates()`에는 `practiceTypes`가 없음
- 따라서 현재 선택은 서버 정렬에만 반영되고, 비매칭 장소를 제외하는 새 필터는 적용되지 않음
- 경로와 본문만 `sort-tags`로 바꿔도 실제 필터는 완성되지 않음
- 온보딩 후 `ApplyInitialFilterTagsUseCase`가 저장하는 레벨별 추천 태그도 **정렬 초기값**으로 분리해야 함
- 서버 스펙은 첫 진입 시 실제 필터 없음, 필터 선택은 앱 로컬에서 관리한다고 정의함
- 목록·마커의 필터는 비로그인도 허용하나 검색은 계속 JWT가 필요함 — 게스트 필터 UI 노출은 앱 결정 사항임
- 코스는 선택 유형 중 하나라도 맞으면 포함, 주차장은 `PARKING` 포함 시 노출함
- 필터·범위·정렬 조건이 바뀌면 첫 페이지부터 조회하고, 다음 페이지에는 같은 조건을 유지해야 함

근거: [서버 스펙 019](https://github.com/Central-MakeUs/Rodi-Server/blob/f42a9993e80131203bf66108e2c48b0da372c24b/docs/specs/019-refinements-1.md), `core/data/.../source/remote/api/MemberApi.kt`, `PlaceApi.kt`, `feature/home/.../HomeViewModel.kt:585`

### 2-3. 지도 마커 — 서버가 단계별 묶음을 내려줌

기존 `GET /places/coordinates`는 전체 좌표 배열이며 폐기 예정으로 남아 있음
대체 `GET /places/clusters`는 검색 범위 `swLat`, `swLng`, `neLat`, `neLng`가 필수이고 `practiceTypes`는 선택임

```text
data.places                  검색 범위 내 장소 정보
data.levels[].minLngSpan      현재 화면 경도 폭의 하한, 이상
data.levels[].maxLngSpan      현재 화면 경도 폭의 상한, 미만, null은 무한대
data.levels[].clusters        클러스터 위치·count·bounds
data.levels[].placeIds        개별 마커로 표시할 장소 ID
```

- 첫 진입·재검색 시 같은 범위와 필터로 목록과 클러스터를 조회함
- 확대·축소는 서버를 다시 부르지 않고 받은 단계 중 현재 화면 경도 폭에 맞는 것을 선택함
- 클러스터 탭은 서버가 준 `bounds`로 카메라를 맞춤
- 앱은 한반도 전체가 보이는 정도로 최대 축소를 제한하도록 서버 스펙에 정의되어 있음
- 현재 앱은 `HomeViewModel.loadCoordinates()`로 좌표를 받고 `HomeScreen`·`MapClusterer`에서 화면상 거리로 직접 묶음
- `PlaceRepository`, `CachedPlaceRepository`, 도메인 모델·UseCase, 홈 상태·재검색, 마커 렌더링·탭 처리가 전환 영향 범위임
- 전환 시 전체 좌표 캐시를 범위·필터와 무관하게 재사용하지 않도록 확인해야 함
- 폐기 API의 제거 날짜는 확인한 명세에 없음 — deprecated를 현재 404로 오해하지 않음

근거: [서버 스펙 021](https://github.com/Central-MakeUs/Rodi-Server/blob/f42a9993e80131203bf66108e2c48b0da372c24b/docs/specs/021-marker-clustering.md), `core/data/.../repository/CachedPlaceRepository.kt`, `feature/home/.../HomeScreen.kt:718`, `map/MapClusterer.kt`

### 2-4. 삭제 코스 — 저장 목록과 연습기록의 반영 수준이 다름

- 서버 `DELETE /courses/{courseId}`는 soft delete이며 다른 회원의 저장·연습기록을 남김
- 저장·연습 목록에는 `isDeleted=true`, 상세에는 `COURSE_404_2`를 반환하는 계약임
- 저장 장소 DTO는 `isDeleted`를 읽고 저장 목록은 삭제 항목을 제외함
- 반면 `PracticeItemResponse`, `PracticeRecordItem`, `MemberMapper`는 `isDeleted`를 전달하지 않음
- 연습기록의 후기 버튼 판단은 주차장 여부와 `hasReview`만 확인하므로 삭제 코스를 구분할 수 없음
- 마이페이지 미리보기와 전체 연습기록 양쪽을 확인해야 함
- 삭제된 항목을 숨길지, 상태를 표시하고 동작만 막을지는 기존 디자인·기획과 맞춰 확정할 사항임
- 이 항목은 이번 10월 신규 API가 아니라 기존 soft delete 계약의 미반영임

근거: [서버 스펙 015](https://github.com/Central-MakeUs/Rodi-Server/blob/f42a9993e80131203bf66108e2c48b0da372c24b/docs/specs/015-my-course-management.md), 라이브 `PracticeItem`, `core/data/.../model/member/MemberResponses.kt`, `core/domain/.../model/member/MyPage.kt`, `feature/mypage/.../practicerecords/PracticeRecord.kt`

## 3. 닉네임 — 서버 대기라는 기존 설명을 나눠 봐야 함

**30일 제한은 단순히 응답 필드가 빠진 상태가 아니라 문서상 정책 충돌임**

- 서버 스펙 020은 범위 밖에 `변경 횟수 제한(쿨다운) — 두지 않는다`고 명시함
- 서버 `NicknameService`·`MemberProfileService`에서도 시간 제한 검사 없이 형식·금칙어·중복을 판정하고 변경함
- Android `NicknameScreen`에는 `닉네임을 바꾸면 30일 후에 다시 바꿀 수 있어요` 안내가 있음
- 제한 상태 UI는 있으나 실제 서버 제한값을 연결한 상태가 아님
- 30일 정책을 유지하려면 서버 요구사항 변경과 다음 변경 가능 시각·거절 계약이 필요함
- 서버의 무제한 정책을 채택한다면 앱 안내·제한 UI 범위를 다시 맞춰야 함
- 사용자에게 확인된 기존 30일 요구를 이번 조사만으로 폐기하거나 클라이언트 날짜로 제한하지 않음

**기존 공백 닉네임의 `_` 이관은 서버 코드에 이미 있음**

- #78과 `V28__nickname_underscore_and_case_insensitive_unique.sql`에 기존 회원 닉네임의 공백을 `_`로 바꾸는 UPDATE와 대소문자 무시 유니크 인덱스가 있음
- 따라서 `docs/BACKLOG.md`의 이관 대기는 “서버 코드 미구현”과 “실제 DB 적용 미확인”을 구분해 재정리할 필요가 있음
- 이번에는 DB 마이그레이션 이력·전체 회원 데이터에 접근하지 않았으므로 운영 데이터 전부의 이관 완료를 확정하지 않음

근거: [서버 스펙 020](https://github.com/Central-MakeUs/Rodi-Server/blob/f42a9993e80131203bf66108e2c48b0da372c24b/docs/specs/020-nickname-change.md), [V28](https://github.com/Central-MakeUs/Rodi-Server/blob/f42a9993e80131203bf66108e2c48b0da372c24b/src/main/resources/db/migration/V28__nickname_underscore_and_case_insensitive_unique.sql), `feature/mypage/.../nickname/NicknameScreen.kt:144`, `docs/BACKLOG.md:35`

## 4. 추가 API·모델 차이 중 즉시 장애로 분류하지 않은 것

| 항목 | 확인 결과 |
|---|---|
| `GET /members/me/onboarding-form` | 앱 API 미정의, 문항·선택지·`skippable`·`exclusive`·`showWhen`·`skipsFollowUps` 서버 관리용 신규 API |
| 온보딩 폼과 점수 | 서버 폼은 점수를 주지 않음, 점수→레벨 계산은 계속 앱 책임, 폼 연동과 점수 계산 이관을 섞지 않음 |
| `GET /members/me/practices?level=...` | 현재 배포 API에 없음, 서버 스펙 022는 Deferred이므로 지금 필수 변경 목록에 넣지 않음 |
| 방문 응답의 `practiceId`·연습 항목의 `level` | 스펙 022의 미래 계약이며 현재 모델 누락으로 판정하지 않음 |
| `ReviewSummaryResponse.topDifficulty` | 앱은 `difficultyCounts`로 계산 중이며 BACKLOG에도 알려진 항목, 신규 차단 이슈 아님 |
| 신고 요청 `detailConsistent` | 앱은 전송하지만 서버 요청 필드가 아님, 서버의 내부 검증 메서드는 `@JsonIgnore`·`@Schema(hidden=true)`, 정리 후보이며 인증 POST 수락 여부는 미검증 |
| 신고 폼 `type` | 서버는 공용 폼의 type을 내려주고 앱 신고 DTO는 생략함, 추가 응답 필드는 `ignoreUnknownKeys=true`로 무시 |
| 회원 PATCH의 `nickname` | 앱에 `NicknameUpdateRequest`가 따로 있어 `MemberUpdateRequest`에 없다는 이유로 미구현 판정하지 않음 |
| 코스 등록 요청 schema | 라이브 OpenAPI는 요청 example만 있고 schema가 없어 자동 필드 비교로 삭제·누락 판정할 수 없음 |
| 관리자 코스 승인 PATCH | 서버에만 있는 관리용 API, 일반 Android 사용자 앱의 누락으로 보지 않음 |

근거: [서버 스펙 022](https://github.com/Central-MakeUs/Rodi-Server/blob/f42a9993e80131203bf66108e2c48b0da372c24b/docs/specs/022-level-practice-record.md), [신고 요청 DTO](https://github.com/Central-MakeUs/Rodi-Server/blob/f42a9993e80131203bf66108e2c48b0da372c24b/src/main/java/cmc/rodi/domain/review/dto/ReviewReportRequest.java), 라이브 OpenAPI와 현재 Android 코드

## 5. 실제 검증 결과와 한계

### 계약 전체 대조

- 라이브 OpenAPI: HTTP method+path 기준 49개 operation
- Android의 Rodi 서버 Retrofit API: 함수 46개, 중복 PATCH 정의를 합치면 45개 operation, 외부 Kakao API 제외
- 앱의 45개 operation은 모두 현재 명세에 존재함, 그중 `coordinates`와 `filter-tags` 2개는 deprecated
- 서버에만 있는 4개: `sort-tags`, `clusters`, `onboarding-form`, 관리자 승인 PATCH
- 반환 wrapper를 따라 DTO 필드를 재귀 대조했으며, 단순 이름 차이·별도 요청 DTO·schema 미노출을 수동으로 구분함
- 모든 API의 실제 성공·권한·데이터 상태를 검증했다는 뜻은 아님

### 공개 GET 확인

검색 범위는 남서 `(37.4, 126.7)`, 북동 `(37.7, 127.2)`, 목록 기준 위치 `(37.55, 126.95)`, `size=1`임
목록은 첫 페이지 `totalCount`를 사용해 클러스터의 전체 장소 수와 비교함

| 조건 | 클러스터 `places` 수 | 목록 `totalCount` | 클러스터 단계 |
|---|---:|---:|---:|
| 필터 없음 | 15 | 15 | 12 |
| `practiceTypes=PARKING` | 11 | 11 | 12 |

- 네 요청 모두 성공 envelope `COMMON_200`, `isSuccess=true`
- 두 조건 모두 12단계 각각 `clusters.count 합 + placeIds 수 = places 수` 확인
- 개별 `placeIds`가 `places`에 존재함을 확인, PARKING 조건은 모두 주차장임
- 인증 회원의 정렬 우선순위, 전국 범위 성능, 실제 카카오 지도 렌더링은 미검증임

### 저장소 검사

- `.github/scripts/check-conventions.sh`: BLOCK 0, WARN 2 — 기존 app Compose BOM 직접 선언
- 앱 소스 변경 없음, 빌드·단위 테스트·실기기 QA는 수행하지 않음
- 기존 HANDOFF·BACKLOG·Notion 원문은 수정하지 않고 이 조사 스냅샷만 추가함

## 6. 구현 전에 확인할 사항

1. 닉네임 정책을 30일 제한으로 통일할지, 서버의 현재 무제한 정책으로 통일할지 결정 필요
2. 옛 `filter-tags`·`coordinates` 제거 시점을 Android·iOS 전환과 맞춰야 함, 명세에는 날짜 없음
3. 삭제 코스의 연습기록 표시·후기 버튼 동작은 확정 디자인과 맞춰야 함
4. 실제 필터와 정렬을 별도 상태로 둔 뒤 홈→검색→상세→복귀, 필터 변경→재검색→다음 페이지, 클러스터 탭→뒤로 경로를 검증해야 함
5. 온보딩은 `없음`→다른 항목·역순 선택, 로컬 복원, 기존 잘못된 입력의 재전송을 같은 조건으로 전후 비교해야 함
