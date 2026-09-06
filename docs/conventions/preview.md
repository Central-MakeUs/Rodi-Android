# Preview

> Rodi가 의도적으로 유지하는 **프로젝트 고유 규칙**이다. 프로젝트 무관 규범은 전역 스킬
> `/android-code-standard`에 있고, 여기 규칙이 그것과 충돌하면 **이 문서가 우선**한다.
> 수치는 적지 않는다 — 재검증 명령으로 대체한다(→ `README.md`). 시점별 조사 수치는 `../audits/`.

## `private` 최상위 함수, 이름은 `<대상><상태>Preview`

`showBackground = true`를 항상 붙인다.

```kotlin
@Preview(showBackground = true)
@Composable
private fun LoginContentFirstPreview() { ... }
```

**왜**: private이 아니면 Preview가 모듈 공개 API로 새어 나가 다른 모듈에서 호출 가능해진다.
이름 끝을 `Preview`로 고정하면 프로덕션 컴포저블과 검색에서 섞이지 않는다. `showBackground`가
없으면 투명 배경 위에 그려져 밝은 색 컴포넌트가 안 보인다.

**정본**: `feature/auth/.../LoginScreen.kt` — 앵커 `private fun LoginContentFirstPreview`

**재검증** (public Preview):
```bash
rg -B2 '@Preview' -g '*.kt' . | rg '^\S*[-:]fun \w+Preview'
```

## Preview 데이터는 실제 `UiState`와 도메인 모델로 조립한다

Preview 전용 상태 모델을 따로 만들지 않는다. 콜백에는 `{}`를 넘긴다. 재사용할 데이터는
feature 안의 `*PreviewData.kt`로 뺀다.

```kotlin
SearchUiState(query = "주차장", places = previewPlaces, isLoading = false)
```

**왜**: Preview가 프로덕션 타입을 그대로 쓰면 화면 시그니처가 바뀔 때 Preview도 같이 깨진다 —
그게 정상이다. 별도 Preview 모델을 두면 Preview는 계속 컴파일되는데 실제 화면과 어긋난 걸
아무도 모른다.

**정본**: `feature/home/.../HomePreviewData.kt`

**`PreviewParameterProvider`는 Rodi에 없다.** 도입할지는 정해진 바 없으니, 쓰고 싶으면
그때 판단하고 이 문서를 갱신할 것 — "안 쓰는 게 규범"이 아니라 "정한 적 없음"이다.

## `Dialog`/`Popup`은 `LocalInspectionMode` 분기 필수

이건 사례가 아니라 **절대 규칙**이다 → 전역 스킬 `/android-code-standard` §1.4.
