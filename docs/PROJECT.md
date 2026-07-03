# PROJECT.md — Rodi (공유 진실원천)

> 이 문서는 **Claude와 Codex가 모두 참조하는 단일 사실원천**이다.
> 프로젝트 사실/컨벤션은 여기에만 적고, `CLAUDE.md`·`AGENTS.md`는 이 문서를 참조한다(중복 금지).
> **잘 안 변하는 사실만** 담는다. 변동 큰 기능 동작은 코드를 진실원천으로 본다.

## 정체성
- 앱: **Rodi** (구 Routi — 브랜드 잔재 정리 완료)
- 패키지: `com.dororong.rodi`
- 구조: 멀티모듈(`:core:domain/data/ui/common` + `:feature:entry/home`). 목표 전체 구조는
  `docs/ARCHITECTURE_TARGET.md` 참고

## 빌드/버전
- minSdk 30 / targetSdk 36 / compileSdk 37
- versionName `1.0.0` / versionCode 2
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
| `:core:data` | `EntryPreferences`(DataStore), `SampleCourses`, `KakaoDirectionsClient`(REST), `NaviPreference` |
| `:core:ui` | `RodiTheme` 토큰(colors/typography/spacing/radius) · 공용 약관 WebView(`terms.TermsWebView`) |
| `:core:common` | 확장함수/유틸(`runSuspendCatching` 등) |
| `:feature:entry` | 진입 게이트(위치권한·약관·운전 주의사항), `EntryFlow` + 단계별 Content |
| `:feature:home` | 홈 화면(지도+코스 바텀시트), 지도 렌더(`map`), 외부 내비 런처(`navi`), 현재 위치(`location`) |

## 컨벤션 (필수)
- **테마 토큰만 사용**: 색/타이포는 `RodiTheme.colors` / `RodiTheme.typography`만. 하드코딩 금지.
- **Material 아이콘 금지**: 필요한 아이콘은 Figma에서 추출하거나 사용자에게 요청. (`Icons.*` 사용 금지)
- **주석**: 자명한 코드엔 주석 X. @Composable 함수 단위 주석 X(섹션 마커만 허용).
  외부 연동(카카오맵/내비 등) 동작·함정·폴백은 짧은 KDoc 권장. *왜*만 적고 *무엇*은 코드로.
- **커밋**: 한국어 conventional commit (`feat(home):`, `fix(entry):` …). PR/이슈 참조는 소스에 넣지 않음.
  "Phase 1/2" 같은 내부 계획 용어·HANDOFF 제목을 그대로 커밋 메시지에 쓰지 않는다 — 계획 문서는
  아카이브 후 사라지므로, 커밋 메시지만 보고 무엇이 바뀌었는지 알 수 있게 실제 변경 내용으로 적는다.
- **시크릿**: `local.properties` → `KAKAO_NATIVE_APP_KEY`, `KAKAO_REST_API_KEY`. **절대 커밋 금지.**

## 디자인 원천
- Figma "루티 DESIGN" (예: 홈 node 366-3412). 토큰/픽셀은 Figma 확정값 기준.

## 후속/기술부채
→ `docs/BACKLOG.md` (Claude 메모리를 못 보는 Codex와 공유하는 채널)
