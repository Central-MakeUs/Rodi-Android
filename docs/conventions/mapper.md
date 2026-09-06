# DTO ↔ Domain 매퍼

> Rodi가 의도적으로 유지하는 **프로젝트 고유 규칙**이다. 프로젝트 무관 규범은 전역 스킬
> `/android-code-standard`에 있고, 여기 규칙이 그것과 충돌하면 **이 문서가 우선**한다.
> 수치는 적지 않는다 — 재검증 명령으로 대체한다(→ `README.md`). 시점별 조사 수치는 `../audits/`.

## data 계층의 `*Mapper.kt`에 최상위 확장 함수로 쓴다

mapper 객체나 클래스를 만들지 않는다. 중첩 DTO 변환은 같은 파일의 private 확장 함수로 둔다.

```kotlin
fun PlaceDetailResponse.toDomain() = PlaceDetail(...)

private fun WaypointResponse.toDomain() = PlaceWaypoint(...)
```

**왜**: Repository가 응답을 받은 자리에서 바로 변환할 수 있고, 매퍼를 주입받을 필요가 없다.
클래스로 만들면 Hilt 모듈·생성자 주입이 따라붙는데 상태가 없는 순수 변환에는 전부 낭비다.

**방향은 이름으로**: Domain → Request는 `toRequest()` 또는 `toData()`.

**정본**: `core/data/.../mapper/PlaceMapper.kt` — 앵커 `fun PlaceDetailResponse.toDomain()`

**재검증** (매퍼를 클래스/객체로 만든 곳):
```bash
rg -n '(class|object) \w+Mapper' -g '*.kt' . | grep -v -E '/src/(test|androidTest)/'
```

## 선택 필드는 null을 보존하고, 필수 계약 필드는 명시적으로 실패시킨다

```kotlin
// 필수 — 없으면 서버 계약 위반이다
accessToken = requireField(accessToken?.takeIf { it.isNotBlank() }, "accessToken")

// 선택 — 없을 수 있다
type = type?.let { v -> SearchTargetType.entries.firstOrNull { it.name == v } }
```

**왜**: 둘을 같게 다루면 정보가 사라진다. 필수 필드에 기본값을 채우면 파싱은 성공하는데 값이
**조용히 틀린다** — Rodi에서 `totalCount`가 `totalReviewCount`로 개명됐을 때 기본값 0을 채워
"전체보기" 노출 조건이 항상 거짓이 된 적이 있다(2026-08-13). 크래시는 눈에 띄지만 조용히
틀린 값은 몇 주 뒤에나 발견된다.

**정본**: `core/data/.../mapper/AuthMapper.kt` — 앵커 `requireField(`

## Domain enum → API 값은 명시적 `when` 매핑

`.name`을 쓰지 않는다. 서버와 Domain 명칭이 같아 보여도 마찬가지다.

```kotlin
RoadExperience.WITH_COMPANION -> "ACCOMPANIED"
VehicleType.COMPACT -> "LIGHT"
```

**왜**: `.name`을 쓰면 Kotlin enum 상수명이 곧 API 계약이 된다 — 리팩터링으로 이름을 바꾸는
순간 컴파일은 통과하고 서버 요청만 깨진다. 실제로 Rodi의 두 enum은 서버 값과 이름이 다르다.
`when`으로 적으면 새 상수를 추가할 때 컴파일러가 분기 누락을 잡아준다.

**정본**: `core/data/.../mapper/OnboardingMapper.kt` — 앵커 `fun ... toApiValue()`

**재검증** (enum 이름을 API 값으로 흘리는 곳):
```bash
rg -n '\.name\b' -g '*Mapper.kt' core/data/src/main
```

## 알 수 없는 enum 값: 필수는 실패, 선택은 drop

- **필수·제어 값**(코스 승인 상태처럼 분기를 결정하는 것) → 도메인 예외로 실패
- **선택 값**(화면에서 생략 가능한 것) → `null` 또는 `mapNotNull`로 제외
- **임의의 정상값으로 대체하지 않는다**

**왜**: 모르는 레벨을 `SEED`로, 모르는 연습 상태를 `PLANNED`로 바꾸면 앱은 안 죽지만 사용자에게
**틀린 정보를 자신 있게 보여준다.** 서버가 새 enum 값을 추가했을 때 "모른다"와 "기본값이다"는
전혀 다른 사건인데 후자로 뭉개진다.

**Rodi는 이게 3방식으로 갈려 있다** — 임의값 대체 2건은 수정 대상이다 →
`../BACKLOG.md`. 따라 할 사례가 아니다.

**재검증** (enum 폴백):
```bash
rg -n 'getOrElse|valueOf\(' -g '*Mapper.kt' core/data/src/main
```
