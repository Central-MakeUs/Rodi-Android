# docs/conventions — Rodi 고유 규칙

## 이 폴더가 소유하는 것

**Rodi가 의도적으로 유지하는 프로젝트 고유 규칙.** 다른 Android 프로젝트에 그대로 옮길
이유가 없는 것들이다 — 모듈 이름, 패키지 배치, 이 프로젝트가 고른 MVI 형태 같은 것.

Global 판단 workflow는 `android-development`, 코드 위생은 `android-code-standard`가 소유한다.
Rodi의 strict UseCase·테마·파일 배치·명명·검증 환경은 이 폴더의 프로젝트 결정이다.

| 질문 | Authority |
|---|---|
| API 존재·지원 버전·동작 | 공급자 공식 자료 + 실제 프로젝트 dependency/설정 |
| 일반 권장 | 공식 guidance; 프로젝트 결정과 구분 |
| Rodi가 선택한 정책 | conventions/ADR/명시적 결정 |
| 현재 구현 사실 | code/build config; legacy일 수 있음 |
| 이번 변경 범위 | 사용자 요청; Skill이 범위를 확대하지 않음 |

## 이 폴더가 소유하지 않는 것

| 무엇 | 어디 |
|---|---|
| 현재 구현이 실제로 어떤지 | **코드**. 이 문서는 코드의 복사본이 아니다 |
| 왜 이 구조를 골랐는지 | `../adr/` |
| 자동 판정 가능한 규칙의 집행 | `.github/scripts/check-conventions.sh` (CI) |
| 규칙과 코드의 현재 차이 | `../BACKLOG.md` |
| 특정 시점의 조사 수치 | `../audits/` |

## 수치를 적지 않는 이유

"20개 중 18개" 같은 카운트는 적는 순간부터 썩는다. 실제로 1차 조사 수치 4건이 며칠 만에
틀렸다(→ `../audits/2026-09-06-code-conventions.md`). 그래서 이 폴더에는 카운트 대신
**재검증 명령**을 적는다. 시점별 수치가 필요하면 `../audits/`를 보되, 그건 역사지 현재가 아니다.

같은 이유로 **정본 경로에 줄 번호를 붙이지 않는다.** 위에 한 줄만 들어가도 어긋난다.
대신 grep으로 찾을 수 있는 **앵커**(심볼명이나 특징적인 한 줄)를 적는다.

## 재검증 명령을 쓸 때 (전부 한 번씩 당한 것들)

명령도 코드다. 낡고, 틀리고, 조용히 0건을 낸다.

- **경로 글롭(`--glob '**/*.kt'`)을 쓰지 말 것.** ripgrep 14와 15에서 결과가 다르다.
  로컬에서 맞던 명령이 CI에서 전부 0건을 냈다. 베이스네임 글롭 `-g '*.kt'`을 쓰고
  디렉터리 한정은 `| grep '/src/test/'`처럼 **출력 경로를 거른다.**
- **rg에 경로(`.`)를 항상 준다.** 없으면 stdin을 읽으려 해서 멈추거나 0건을 낸다.
- **`build/`는 `.gitignore`에 있어 rg가 알아서 건너뛴다.** 따로 제외하지 않는다.
- **`xargs`를 쓰지 말 것.** 입력이 비면 인자 없이 실행돼 저장소 전체를 훑는다.
  `while read` + `grep -q`로 대체한다.
- **`-P`(PCRE2)는 빌드에 안 들어간 환경이 있다.** 여기 문서의 수동 확인용으로는 쓰되,
  CI 검사에는 쓰지 않는다.
- **안 돌려본 명령을 적지 말 것.** 앵커 하나가 어긋나면 0건이 나오고,
  **0건은 "깨끗하다"로 오독된다.**

## 파일

| 파일 | 다루는 것 |
|---|---|
| `structure.md` | 소스 루트, 패키지 임계, Contract 배치, namespace |
| `naming.md` | Composable·ViewModel·UseCase·Repository·콜백·Boolean·컬렉션 이름 |
| `mvi.md` | UiState/Intent/Effect 형태, Contract 순서, 상태 노출, Effect 전달·소비 |
| `error-handling.md` | `runSuspendCatching`, 취소 재전파, API 경계, 사용자 메시지 변환 |
| `mapper.md` | DTO↔Domain 확장함수, nullable 보존, 필수 필드 실패, enum 변환 |
| `compose.md` | Modifier 위치, `remember` key, `LaunchedEffect` key, inset, Lazy key |
| `testing.md` | 테스트 이름, JUnit5+MockK, 코루틴 테스트, Roborazzi |
| `preview.md` | Preview 함수 이름·가시성, Preview 데이터 만드는 법 |
| `gradle.md` | namespace, Convention Plugin 배치와 의존성 출처 |
| `release.md` | 태그 push 릴리스 흐름, 버전 위치 2곳, 범프 브랜치·커밋 형식, prerelease 판정, 릴리스 본문 |

## 리뷰 지적을 규칙으로

반복되는 리뷰 지적은 규칙 후보로 검토한다. 빈도만으로 MUST로 승격하지 않고 적용 조건·반례·
프로젝트 선택 여부와 검증 가능성을 확인한다.

1. **grep으로 판정되면** `.github/scripts/check-conventions.sh`에 넣는다. 위반이 0건이면 바로
   BLOCK, 기존 위반이 있으면 WARN으로 넣고 부채를 갚는 즉시 BLOCK으로 올린다.
2. **판정이 안 되면** 해당 주제 파일에 *왜*와 **정본 앵커**를 적는다. 정본은 규칙을 실제로 지키는
   파일이어야 한다 — 규칙과 다른 파일을 정본으로 가리키면 에이전트는 문장이 아니라 그 파일을 따른다.
3. 기존 코드가 규칙과 다르면 `../BACKLOG.md` "코드 관용구 정합성"에 남긴다.

## 코드와 다르면

현재 사실은 코드로 확인한다. 의도된 규범과 다르면 legacy debt, stale 문서, 의도된 정책 변경 중
무엇인지 구분한다. 코드에 맞춰 규범을 자동 수정하거나 규범만 보고 범위 밖 코드를 고치지 않는다.
과거 audit은 조사 revision의 snapshot으로 보존한다.
