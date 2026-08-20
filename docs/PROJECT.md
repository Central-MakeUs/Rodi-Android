# PROJECT.md — Rodi (공유 진실원천)

> 이 문서는 **Claude와 Codex가 모두 참조하는 단일 사실원천**이다.
> 프로젝트 사실/컨벤션은 여기에만 적고, `CLAUDE.md`·`AGENTS.md`는 이 문서를 참조한다(중복 금지).
> **잘 안 변하는 사실만** 담는다. 변동 큰 기능 동작은 코드를 진실원천으로 본다.

## 정체성
- 앱: **Rodi** (구 Routi — 브랜드 잔재 정리 완료)
- 패키지: `com.dororong.rodi`
- 구조: 멀티모듈(`:core:domain/data/ui/common` + `:feature:auth/entry/home/settings`). 전체 구조는
  `docs/ARCHITECTURE_TARGET.md` 참고

## 빌드/버전
- minSdk 30 / targetSdk 36 / compileSdk 37
- versionName `1.4.0` / versionCode 18
- 명령:
  - 빌드 `./gradlew assembleDebug`
  - 테스트 `./gradlew test`
  - 릴리스 `./gradlew assembleRelease`
  - 린트 `./gradlew lint`
- 테스트 컨벤션: `docs/TESTING.md` 참고

## `:app`에 남은 것
`MainActivity`(엔트리 포인트), `RodiApplication`(Kakao SDK 초기화), `ui/RodiApp`(Navigation3 `NavDisplay` 기반 게이트→홈 라우팅).
화면·기능 코드는 전부 `core:*`/`feature:*`로 이관 완료.

## 모듈 맵
| 모듈 | 역할 |
|---|---|
| `:core:domain` | 도메인 모델(`Course` 등) |
| `:core:data` | `EntryPreferences`/온보딩 동기화 상태(DataStore), `SampleCourses`, `KakaoDirectionsClient`(REST), `NaviPreference`, `AuthApi`/`MemberApi`/`PlaceApi`/`AuthTokenStore`(인증·회원·장소 API와 세션 관리, Android Keystore AES-GCM + DataStore) |
| `:core:ui` | `RodiTheme` 토큰(colors/typography/spacing/radius) · 공용 약관 WebView(`terms.TermsWebView`) |
| `:core:common` | 확장함수/유틸(`runSuspendCatching` 등) |
| `:feature:auth` | 카카오 로그인 화면 + SDK 로직, 서버 로그인·재발급·로그아웃·탈퇴 유예 계정 복구 연동 |
| `:feature:entry` | 진입 게이트(위치권한·약관·운전 주의사항) + 온보딩 설문(닉네임·경력·선호), `EntryFlow` + 단계별 Content |
| `:feature:home` | 홈 화면(지도+코스 바텀시트), 지도 렌더(`map`), 외부 내비 런처(`navi`), 현재 위치(`location`) |
| `:feature:mypage` | 서버 프로필·운전 목표·저장한 장소 목록과 코스·주차장 상세 진입 |
| `:feature:settings` | 설정과 약관 목록·WebView. Home과 직접 의존하지 않고 App route로 연결 |

## 컨벤션 (필수)
- **테마 토큰만 사용**: 색/타이포는 `RodiTheme.colors` / `RodiTheme.typography`만. 하드코딩 금지.
- **Material 아이콘 금지**: 필요한 아이콘은 Figma에서 추출하거나 사용자에게 요청. (`Icons.*` 사용 금지)
- **주석**: 자명한 코드엔 주석 X. @Composable 함수 단위 주석 X(섹션 마커만 허용).
  외부 연동(카카오맵/내비 등) 동작·함정·폴백은 짧은 KDoc 권장. *왜*만 적고 *무엇*은 코드로.
- **커밋**: 한국어 conventional commit (`feat(home):`, `fix(entry):` …). PR/이슈 참조는 소스에 넣지 않음.
  "Phase 1/2" 같은 내부 계획 용어·HANDOFF 제목을 그대로 커밋 메시지에 쓰지 않는다 — 계획 문서는
  아카이브 후 사라지므로, 커밋 메시지만 보고 무엇이 바뀌었는지 알 수 있게 실제 변경 내용으로 적는다.
- **시크릿**: `local.properties` → `KAKAO_NATIVE_APP_KEY`, `KAKAO_REST_API_KEY`. **절대 커밋 금지.**
- **패키지**: 같은 역할 파일이 2개 이상이면 역할 패키지를 만들고, 하나면 feature 루트에 둔다.
  Contract는 feature 루트에 하나로 유지하고 public 재사용 Composable은 파일당 하나를 기본으로 한다.
- **의존성**: 같은 configuration에서 항상 함께 쓰는 2개 이상의 의존성은 version catalog bundle을 사용한다.
  BOM·compiler·debug/runtime 전용 의존성은 bundle에서 제외한다.
- **`core:ui` 컴포넌트 Preview 필수**: `core:ui`에 새 컴포저블을 추가하면 `@Preview(showBackground = true,
  widthDp = 360)` + `RodiTheme { }` 래핑으로 최소 1개(variant/상태가 여러 개면 그만큼) 작성한다.
  기존 예시는 `RodiButton.kt`/`RodiSnackbar.kt` 참고.
- **서버 응답 파싱 오류는 기본값만 채워 덮지 말 것**: `MissingFieldException` 등 역직렬화 예외가 나오면,
  먼저 라이브 Swagger(`https://api.stillstar.store/v3/api-docs`)를 다시 받아 실제 필드명·구조가
  바뀌었는지 대조한다. 필드에 기본값만 넣어 크래시를 막으면 파싱은 성공해도 그 필드가 항상
  기본값으로 조용히 틀리게 채워질 수 있다(예: `totalCount`가 `totalReviewCount`로 개명됐는데
  기본값 0만 채웠다가 "전체보기" 노출 조건이 항상 거짓이 된 사례, 2026-08-13). enum 값도 같은
  이유로 반드시 Swagger 원문과 1:1 대조한다.
- **`Dialog`/`Popup` 기반 컴포저블은 `LocalInspectionMode` 분기 필수**: `Dialog`/`Popup`은 별도
  윈도우로 뜨기 때문에 IDE `@Preview`에서 실제 앱과 다르게(또는 아예 안) 그려진다.
  `if (LocalInspectionMode.current) { 내용만 직접 그리기 } else { Dialog(...) { 내용 } }`으로
  분기해 프리뷰에서는 진짜 `Dialog`/`Popup`을 띄우지 않고 내용 Composable을 그대로 그린다.
  참고 구현: `core/ui/.../dialog/RodiDialog.kt`.

## 디자인 원천
- Figma "루티 DESIGN" (예: 홈 node 366-3412). 토큰/픽셀은 Figma 확정값 기준.

## 후속/기술부채
→ `docs/BACKLOG.md` (Claude 메모리를 못 보는 Codex와 공유하는 채널)
