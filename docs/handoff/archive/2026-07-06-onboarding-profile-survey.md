# HANDOFF — 온보딩 설문(닉네임 확인 → 경력입력 → 추가정보) 3단계 추가

> Claude(기획)와 Codex(구현)가 주고받는 **단일 활성 작업 채널**.
> 완료되면 `docs/handoff/archive/<날짜>-<작업>.md`로 옮기고 이 파일은 다음 작업으로 비운다.

Status: DONE
Branch: feature/onboarding-profile-survey

## Context (왜)
Figma("루티 DESIGN")에 위치권한/약관/주의사항 이후에 이어지는 온보딩 설문 3화면이 추가됐다
(node: 996-17786, 996-17823, 902-12597, 915-16385, 902-13034 등). 현재 `:feature:entry`의
`EntryFlow`는 `LOCATION → TERMS → PRECAUTIONS` 3단계뿐이고, 이 설문 데이터를 저장할 구조도 전혀
없다(코드 확인 완료). 이번 작업은 같은 `EntryFlow` 상태 머신에 `NICKNAME → CAREER → PREFERENCE`
3단계를 이어 붙인다. Figma에서 이 3단계는 프로그레스 바가 1/3부터 새로 채워지므로(기존 3단계와
별개의 시각적 묶음), `EntryScaffold`의 `currentStep`(1~3)을 그대로 재사용하면 된다 — 별도 파라미터
변경 불필요.

**사용자와 확인해 정리된 결정 사항 (모두 확정, 재질문 불필요):**
- 목표 텍스트 입력(선택 3-3)은 **선택사항**. "다음" 활성화는 텍스트와 무관.
- PREFERENCE 화면 "다음" 활성화 조건은 **상황 칩 1개 이상 + 차종 1개 선택** (텍스트 무관). 사용자가
  잠정 결정한 것이라 실제 컨펌 전까지 확정 아님 — 나중에 바뀔 수 있음을 감안해 이 조건은
  `EntryViewModel.isPreferenceNextEnabled` 한 곳에만 존재하게 구현한다(다른 곳에 로직 중복 금지).
- 닉네임은 **서버에서 랜덤 배정 예정**이나 백엔드 API가 없어 이번엔 로컬 랜덤 생성으로 대체한다
  (BACKLOG에 이미 기록). 이번 스코프에 닉네임 수정 UI 없음(마이페이지에서 추후 지원).
- 상황 칩 최대 3개 초과 선택 시 **무시**(4번째 탭은 아무 효과 없음).
- "레벨 관련 정보"(Figma `1011-15065`) — 온보딩 답변별 점수를 클라이언트에서 합산해 서버로 보내는
  방식인데 배점이 미확정이라 **이번 스코프에서 점수 계산 로직 자체를 넣지 않는다**(Out of scope).
- "혼자 연습" 선택 시 나타나는 조건부 질문 2개(`CAREER` 3-4/3-5)는 다른 문항과 동일하게 필수이며,
  `roadExperience`를 "혼자 연습" 외 다른 값으로 바꾸면 그 즉시 두 조건부 답변을 초기화한다(구현
  기본값으로 결정 — 사용자에게 별도 확인은 안 받았지만 상태가 꼬이지 않게 하는 안전한 기본 동작).

**설계 노트:**
- `core:domain`에 이미 `PracticeTag`(Course 특징 태그) enum이 있는데, 이번 온보딩 "선호 상황"
  칩 목록과 라벨이 상당수 겹치지만 완전히 같지는 않다(온보딩 쪽에 "비보호 좌회전"/"코너링"/
  "좁은 도로 주행"/"다차로 주행"/"합류" 5개가 더 있고, `PracticeTag`엔 "야간운전"/"골목길"/
  "보조도로"/"고속도로"가 더 있음). 이번엔 별도 `PracticeSituation` enum으로 분리한다(재사용 강행 시
  두 바운디드 컨텍스트가 섞임). 통합 여부는 추천 로직 설계 시 재검토(BACKLOG에 기록됨).
- `core:ui`에 선택 가능한 pill 모양 칩 컴포넌트가 없어서(`Chip`/`Tag` grep 결과 없음) 신규로
  `RodiSelectableChip`을 추가한다. PROJECT.md 컨벤션상 `core:ui` 신규 컴포저블은 Preview 필수.
- `RodiButton`은 항상 `fillMaxWidth()`라서 PREFERENCE 화면의 "건너뛰기"(고정폭)+"다음"(나머지) 가로
  배치가 불가능하다 → `fillMaxWidth: Boolean = true` 파라미터를 추가한다(디폴트 true라 기존
  호출부는 전부 그대로 동작).
- `EntryScaffold`는 하단에 버튼 1개만 고정 렌더링한다 → PREFERENCE 화면만 버튼 2개(건너뛰기+다음)가
  필요하므로 `bottomBar` 오버라이드 슬롯을 추가한다(디폴트 null이면 기존 동작 그대로).

## Spec (무엇을·어떻게)

### 1. `core:domain` — 온보딩 도메인 모델 신규
`core/domain/src/main/kotlin/com/dororong/rodi/core/domain/OnboardingProfile.kt` 신규:
```kotlin
package com.dororong.rodi.core.domain

enum class DrivingPeriod(val label: String) {
    UNDER_1_MONTH("1개월 미만"),
    MONTH_1_TO_3("1~3개월"),
    MONTH_3_TO_6("3~6개월"),
    MONTH_6_TO_YEAR_1("6~1년"),
    YEAR_1_TO_2("1~2년"),
    YEAR_2_TO_10("2~10년"),
    OVER_YEAR_10("10년 이상"),
}

enum class RecentDrivingFrequency(val label: String) {
    RARELY("거의 없음"),
    MONTHLY_1_TO_2("월 1~2회"),
    WEEKLY_1("주 1회"),
    WEEKLY_2_TO_3("주 2~3회"),
    WEEKLY_4_PLUS("주 4회 이상"),
}

enum class RoadExperience(val label: String) {
    NONE("없음"),
    WITH_COMPANION("동승 연습"),
    PROFESSIONAL_LESSON("전문 도로 연수"),
    SOLO("혼자 연습"),
}

enum class SoloDrivingRange(val label: String) {
    NEAR_HOME("집 근처"),
    FAMILIAR_ROAD("익숙한 길"),
    UNFAMILIAR_ROAD("낯선 도로"),
    HIGHWAY_LONG_DISTANCE("고속·장거리"),
}

enum class SoloParkingLevel(val label: String) {
    NONE("없음"),
    WIDE_SPACE_ONLY("넓은 곳만"),
    FAMILIAR_SPOT("익숙한 곳"),
    MOSTLY_POSSIBLE("대부분 가능"),
}

enum class PracticeSituation(val label: String) {
    U_TURN("유턴"),
    TURN("좌우 회전"),
    PARKING("주차"),
    LANE_CHANGE("차선변경"),
    INTERSECTION("교차로"),
    ROUNDABOUT("회전 교차로"),
    UNPROTECTED_LEFT_TURN("비보호 좌회전"),
    HIGHWAY_ENTRY("고속진입"),
    CORNERING("코너링"),
    NARROW_ROAD("좁은 도로 주행"),
    MULTI_LANE("다차로 주행"),
    MERGING("합류"),
    STRAIGHT("직선주행"),
}

enum class VehicleType(val label: String) {
    COMPACT("경차"),
    SMALL("소형차"),
    MIDSIZE("중형차"),
    SEMI_LARGE("준대형"),
    LARGE("대형차"),
    SUV("SUV"),
}

/** 온보딩 설문 응답. 점수 계산/서버 전송은 미확정이라 이번엔 로컬 저장까지만 한다. */
data class OnboardingProfile(
    val nickname: String,
    val drivingPeriod: DrivingPeriod?,
    val recentFrequency: RecentDrivingFrequency?,
    val roadExperience: RoadExperience?,
    val soloDrivingRange: SoloDrivingRange?,
    val soloParkingLevel: SoloParkingLevel?,
    val practiceSituations: List<PracticeSituation>,
    val vehicleType: VehicleType?,
    val goal: String,
)
```

`core/domain/src/main/kotlin/com/dororong/rodi/core/domain/OnboardingRepository.kt` 신규:
```kotlin
package com.dororong.rodi.core.domain

interface OnboardingRepository {
    suspend fun saveProfile(profile: OnboardingProfile)
}
```

`core/domain/src/main/kotlin/com/dororong/rodi/core/domain/usecase/SaveOnboardingProfileUseCase.kt` 신규
(기존 `SetEntryCompletedUseCase`와 동일 패턴):
```kotlin
package com.dororong.rodi.core.domain.usecase

import com.dororong.rodi.core.domain.OnboardingProfile
import com.dororong.rodi.core.domain.OnboardingRepository
import javax.inject.Inject

class SaveOnboardingProfileUseCase @Inject constructor(
    private val onboardingRepository: OnboardingRepository,
) {
    suspend operator fun invoke(profile: OnboardingProfile) = onboardingRepository.saveProfile(profile)
}
```

### 2. `core:data` — DataStore 저장소 (기존 `EntryPreferences`/`EntryRepositoryImpl` 패턴 그대로)
`core/data/src/main/java/com/dororong/rodi/core/data/OnboardingPreferences.kt` 신규:
```kotlin
package com.dororong.rodi.core.data

import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.dororong.rodi.core.domain.OnboardingProfile

private val Context.onboardingDataStore by preferencesDataStore(name = "onboarding")

/**
 * 온보딩 설문(닉네임·경력·선호) 응답을 로컬에 저장한다.
 * 서버 API/점수 계산 스펙이 없어 지금은 DataStore 로컬 저장만 한다(BACKLOG 참고).
 */
class OnboardingPreferences(private val context: Context) {

    suspend fun saveProfile(profile: OnboardingProfile) {
        context.onboardingDataStore.edit { prefs ->
            prefs[KEY_NICKNAME] = profile.nickname
            profile.drivingPeriod?.let { prefs[KEY_DRIVING_PERIOD] = it.name }
            profile.recentFrequency?.let { prefs[KEY_RECENT_FREQUENCY] = it.name }
            profile.roadExperience?.let { prefs[KEY_ROAD_EXPERIENCE] = it.name }
            profile.soloDrivingRange?.let { prefs[KEY_SOLO_DRIVING_RANGE] = it.name }
            profile.soloParkingLevel?.let { prefs[KEY_SOLO_PARKING_LEVEL] = it.name }
            prefs[KEY_PRACTICE_SITUATIONS] = profile.practiceSituations.map { it.name }.toSet()
            profile.vehicleType?.let { prefs[KEY_VEHICLE_TYPE] = it.name }
            prefs[KEY_GOAL] = profile.goal
        }
    }

    private companion object {
        val KEY_NICKNAME = stringPreferencesKey("nickname")
        val KEY_DRIVING_PERIOD = stringPreferencesKey("driving_period")
        val KEY_RECENT_FREQUENCY = stringPreferencesKey("recent_frequency")
        val KEY_ROAD_EXPERIENCE = stringPreferencesKey("road_experience")
        val KEY_SOLO_DRIVING_RANGE = stringPreferencesKey("solo_driving_range")
        val KEY_SOLO_PARKING_LEVEL = stringPreferencesKey("solo_parking_level")
        val KEY_PRACTICE_SITUATIONS = stringSetPreferencesKey("practice_situations")
        val KEY_VEHICLE_TYPE = stringPreferencesKey("vehicle_type")
        val KEY_GOAL = stringPreferencesKey("goal")
    }
}
```

`core/data/src/main/java/com/dororong/rodi/core/data/OnboardingRepositoryImpl.kt` 신규:
```kotlin
package com.dororong.rodi.core.data

import android.content.Context
import com.dororong.rodi.core.domain.OnboardingProfile
import com.dororong.rodi.core.domain.OnboardingRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class OnboardingRepositoryImpl @Inject constructor(
    @ApplicationContext context: Context,
) : OnboardingRepository {
    private val prefs = OnboardingPreferences(context)

    override suspend fun saveProfile(profile: OnboardingProfile) = prefs.saveProfile(profile)
}
```

`core/data/src/main/java/com/dororong/rodi/core/data/di/DataModule.kt` 수정 — `@Binds` 추가:
```kotlin
@Binds
abstract fun bindOnboardingRepository(impl: OnboardingRepositoryImpl): OnboardingRepository
```

### 3. `core:common` — 닉네임 로컬 랜덤 생성기 (신규)
`core/common/src/main/kotlin/com/dororong/rodi/core/common/NicknameGenerator.kt` 신규:
```kotlin
package com.dororong.rodi.core.common

/** 온보딩 닉네임 자동 배정용 로컬 랜덤 조합. 추후 서버 API로 교체 예정(BACKLOG 참고). */
object NicknameGenerator {

    private val adjectives = listOf(
        "흐름타는", "여유로운", "차분한", "씩씩한", "야무진",
        "든든한", "설레는", "당당한", "포근한", "산뜻한",
    )

    private val animals = listOf(
        "고슴도치", "수달", "다람쥐", "부엉이", "너구리",
        "코알라", "판다", "펭귄", "여우", "토끼",
    )

    fun generate(): String = "${adjectives.random()} ${animals.random()}"
}
```
`feature/entry/build.gradle.kts`에 `implementation(project(":core:common"))` 추가(현재 없음 — 확인 완료).

### 4. `core:ui` — `RodiSelectableChip` 신규 + `RodiButton`/`EntryScaffold` 확장

`core/ui/src/main/java/com/dororong/rodi/core/ui/components/RodiSelectableChip.kt` 신규.
Figma "tag_chips_type01": 미선택 = 배경 `white`, 테두리 1dp `primary200`, 텍스트 `gray600`;
선택 = 배경 `primary100`, 테두리 1dp `primary600`, 텍스트 `primary800`. 모양 pill(`RoundedCornerShape(16.dp)`),
패딩 가로 14dp·세로 6dp, 타이포 `body3Medium`. `order`(1,2,3...)가 있으면 우상단에 지름 18dp 원형
배지(`primary600` 배경, `white` 텍스트, `caption3SemiBold`)를 살짝 겹치게 표시(다중 선택 순서 표시용,
경력입력/추가정보 문항에서 필요). 단일 선택 그룹에선 `order = null`로 배지 없이 사용.
```kotlin
@Composable
fun RodiSelectableChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    order: Int? = null,
)
```
`@Preview(showBackground = true, widthDp = 360)` 1개 이상 필수(미선택/선택/배지 3가지 상태 한 Row에).

`core/ui/src/main/java/com/dororong/rodi/core/ui/components/RodiButton.kt` 수정 — `fillMaxWidth` 파라미터
추가(디폴트 `true`, 기존 호출부 전부 무변화):
```kotlin
@Composable
fun RodiButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: RodiButtonVariant = RodiButtonVariant.Primary,
    enabled: Boolean = true,
    fillMaxWidth: Boolean = true,
    shape: Shape = RodiButtonDefaults.shape(),
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .let { if (fillMaxWidth) it.fillMaxWidth() else it }
            .height(RodiButtonDefaults.Height),
        ...
    )
}
```

`feature/entry/src/main/java/com/dororong/rodi/feature/entry/EntryComponents.kt`의 `EntryScaffold` 수정 —
`bottomBar` 오버라이드 슬롯 추가(디폴트 `null`이면 기존 동작 그대로, 기존 5개 호출부 무변화):
```kotlin
@Composable
fun EntryScaffold(
    currentStep: Int,
    onBack: (() -> Unit)?,
    buttonText: String = "",
    buttonEnabled: Boolean = false,
    onButtonClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    bottomBar: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    // ... 기존 앱바/진행바/content 동일 ...
    if (bottomBar != null) {
        bottomBar()
    } else {
        RodiButton(
            text = buttonText,
            onClick = onButtonClick,
            enabled = buttonEnabled,
            modifier = Modifier
                .navigationBarsPadding()
                .padding(horizontal = RodiSpacing.md, vertical = 10.dp),
        )
    }
}
```

### 5. `feature:entry` — `EntryStep` 확장 + `EntryViewModel` 상태/로직

`EntryFlow.kt`의 `EntryStep` enum:
```kotlin
enum class EntryStep { LOCATION, TERMS, PRECAUTIONS, NICKNAME, CAREER, PREFERENCE, TERMS_WEBVIEW }
```

`EntryViewModel`에 `SaveOnboardingProfileUseCase` 주입 추가하고, 아래 상태/함수 추가:
```kotlin
var nickname by mutableStateOf("")
    private set
var drivingPeriod: DrivingPeriod? by mutableStateOf(null)
    private set
var recentFrequency: RecentDrivingFrequency? by mutableStateOf(null)
    private set
var roadExperience: RoadExperience? by mutableStateOf(null)
    private set
var soloDrivingRange: SoloDrivingRange? by mutableStateOf(null)
    private set
var soloParkingLevel: SoloParkingLevel? by mutableStateOf(null)
    private set
var practiceSituations: List<PracticeSituation> by mutableStateOf(emptyList())
    private set
var vehicleType: VehicleType? by mutableStateOf(null)
    private set
var goal by mutableStateOf("")
    private set

fun ensureNicknameGenerated() {
    if (nickname.isBlank()) nickname = NicknameGenerator.generate()
}

fun selectDrivingPeriod(value: DrivingPeriod) { drivingPeriod = value }
fun selectRecentFrequency(value: RecentDrivingFrequency) { recentFrequency = value }

fun selectRoadExperience(value: RoadExperience) {
    roadExperience = value
    if (value != RoadExperience.SOLO) {
        soloDrivingRange = null
        soloParkingLevel = null
    }
}

fun selectSoloDrivingRange(value: SoloDrivingRange) { soloDrivingRange = value }
fun selectSoloParkingLevel(value: SoloParkingLevel) { soloParkingLevel = value }

fun togglePracticeSituation(value: PracticeSituation) {
    practiceSituations = when {
        practiceSituations.contains(value) -> practiceSituations - value
        practiceSituations.size >= 3 -> practiceSituations
        else -> practiceSituations + value
    }
}

fun selectVehicleType(value: VehicleType) { vehicleType = value }
fun setGoal(value: String) { goal = value }

val isCareerStepValid: Boolean
    get() = drivingPeriod != null && recentFrequency != null && roadExperience != null &&
        (roadExperience != RoadExperience.SOLO || (soloDrivingRange != null && soloParkingLevel != null))

val isPreferenceNextEnabled: Boolean
    get() = practiceSituations.isNotEmpty() && vehicleType != null
```

`next()`/`back()` 확장:
```kotlin
fun next() {
    step = when (step) {
        EntryStep.LOCATION -> EntryStep.TERMS
        EntryStep.TERMS -> EntryStep.PRECAUTIONS
        EntryStep.PRECAUTIONS -> EntryStep.NICKNAME
        EntryStep.NICKNAME -> EntryStep.CAREER
        EntryStep.CAREER -> EntryStep.PREFERENCE
        EntryStep.PREFERENCE -> EntryStep.PREFERENCE
        EntryStep.TERMS_WEBVIEW -> EntryStep.TERMS
    }
}

fun back(): Boolean {
    step = when (step) {
        EntryStep.PRECAUTIONS -> EntryStep.TERMS
        EntryStep.TERMS -> EntryStep.LOCATION
        EntryStep.TERMS_WEBVIEW -> EntryStep.TERMS
        EntryStep.NICKNAME -> EntryStep.PRECAUTIONS
        EntryStep.CAREER -> EntryStep.NICKNAME
        EntryStep.PREFERENCE -> EntryStep.CAREER
        EntryStep.LOCATION -> return false
    }
    return true
}
```

`complete()` 수정 — `OnboardingProfile` 저장 후 기존 로직(순서 중요: 프로필 저장 실패 시 완료 처리도
하지 않음):
```kotlin
fun complete(onDone: () -> Unit) {
    viewModelScope.launch {
        val profile = OnboardingProfile(
            nickname = nickname,
            drivingPeriod = drivingPeriod,
            recentFrequency = recentFrequency,
            roadExperience = roadExperience,
            soloDrivingRange = soloDrivingRange,
            soloParkingLevel = soloParkingLevel,
            practiceSituations = practiceSituations,
            vehicleType = vehicleType,
            goal = goal,
        )
        try {
            saveOnboardingProfileUseCase(profile)
            setEntryCompletedUseCase()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Throwable) {
            return@launch
        }
        onDone()
    }
}
```

### 6. `feature:entry` — 신규 Content 컴포저블 3개

**`NicknameContent.kt`** (Figma `902-13034`). `EntryScaffold(currentStep = 1, onBack = onBack,
buttonText = "다음", buttonEnabled = true, onButtonClick = onNext)`. 콘텐츠 영역 세로 중앙 정렬로
"닉네임"(`headline1`, black, 가운데 정렬) / `'$nickname'`(`heading1`, `primary600`, 가운데 정렬, 앞뒤에
문자 그대로의 `‘`/`’` 포함) / "로 시작해요."(`headline1`, black, 가운데 정렬) 3줄을 세로로 쌓는다
(줄간격 12dp, `Column(verticalArrangement = Arrangement.spacedBy(12.dp))`).

**`CareerContent.kt`** (Figma `996-17786`/`915-17394`/`915-17481`/`996-17823`). `EntryScaffold(currentStep = 2, ...)`.
- 헤더: "운전 경험에 대해 알려주세요."(`heading2`) / "자세히 입력할수록 더 잘 맞는 연습 장소를 추천해요."(`body3Medium`, `gray600`)
- Q1 "면허 취득 후 실제 운전한 기간을 알려주세요"(`body1SemiBold`) — `DrivingPeriod` 7개 단일선택
  (`RodiSelectableChip`, `FlowRow`로 자연 줄바꿈, `order = null`)
- Q2 "가장 최근, 운전을 언제 하셨나요?" — `RecentDrivingFrequency` 5개 단일선택
- Q3 "면허 취득 후 도로 주행을 해본 적이 있나요?" — `RoadExperience` 4개 단일선택
- `roadExperience == RoadExperience.SOLO`일 때만 아래 2문항 추가 노출(값이 바뀌면 ViewModel이 이미
  초기화하므로 Content는 단순히 `if (roadExperience == RoadExperience.SOLO) { ... }`로 조건부 렌더링만
  하면 됨):
  - Q4 "혼자 운전, 어디까지 해봤나요?" — `SoloDrivingRange` 4개 단일선택
  - Q5 "혼자 주차는 어느 정도 해봤나요?" — `SoloParkingLevel` 4개 단일선택
- 버튼: "다음", `buttonEnabled = nextEnabled`(=`viewModel.isCareerStepValid`), 뒤로가기 있음. 건너뛰기 없음.

**`PreferenceContent.kt`** (Figma `902-12597`/`915-16385`). `EntryScaffold(currentStep = 3, onBack = onBack,
bottomBar = { ... })` — `bottomBar`로 "건너뛰기"(`Secondary`, `fillMaxWidth = false`, 고정폭
`Modifier.width(136.dp)`) + "다음"(`Primary`, `fillMaxWidth = false`, `Modifier.weight(1f)`, `enabled = nextEnabled`)를
`Row(horizontalArrangement = Arrangement.spacedBy(6.dp))`로 가로 배치.
- 헤더: "추가 정보를 입력하면 더 정확해요."(`heading2`) / "딱 맞는 코스 추천을 위한 선택항목이에요."(`body3Medium`, `gray600`)
- "더 연습해보고 싶은 상황이 있나요?"(`body1SemiBold`) + 뒤에 "최대 3개"(`body3Medium`, `gray600`) —
  `PracticeSituation` 13개 다중선택(`FlowRow`), 선택된 칩엔 선택 순서대로 `order = 1,2,3` 배지 표시
  (`practiceSituations.indexOf(situation) + 1`), 이미 3개 선택된 상태에서 미선택 칩 탭은
  `togglePracticeSituation`이 그대로 무시하므로 Content는 클릭 핸들러만 연결하면 됨
- "주로 타는 차종은 무엇인가요?"(`body1SemiBold`) — `VehicleType` 6개 단일선택
- "이루고 싶은 운전 목표를 입력해주세요."(`body1SemiBold`) — `OutlinedTextField`, 선택 입력, placeholder
  "복잡한 강남 자신있게 운전하기!"(`gray500`), 테두리 `gray300`/포커스 `primary600`, 모양
  `RoundedCornerShape(RodiRadius.sm)`, 최소 3줄 높이

`EntryFlow.kt`의 `when (target)`에 3개 분기 추가 (`NICKNAME` 진입 시 `LaunchedEffect(Unit) { viewModel.ensureNicknameGenerated() }`로
1회만 생성 — 뒤로 갔다 와도 재생성되지 않게), `PREFERENCE`의 "다음"/"건너뛰기" 모두
`{ viewModel.complete(onComplete) }` 호출.

## Files to touch
- 신규: `core/domain/.../OnboardingProfile.kt`, `core/domain/.../OnboardingRepository.kt`,
  `core/domain/.../usecase/SaveOnboardingProfileUseCase.kt`
- 신규: `core/data/.../OnboardingPreferences.kt`, `core/data/.../OnboardingRepositoryImpl.kt`
- 수정: `core/data/.../di/DataModule.kt` (`@Binds` 추가)
- 신규: `core/common/.../NicknameGenerator.kt`
- 신규: `core/ui/.../components/RodiSelectableChip.kt` (Preview 포함)
- 수정: `core/ui/.../components/RodiButton.kt` (`fillMaxWidth` 파라미터)
- 수정: `feature/entry/.../EntryComponents.kt` (`EntryScaffold.bottomBar`)
- 수정: `feature/entry/.../EntryFlow.kt`, `feature/entry/.../EntryViewModel.kt`
- 신규: `feature/entry/.../NicknameContent.kt`, `feature/entry/.../CareerContent.kt`,
  `feature/entry/.../PreferenceContent.kt` (각 `@Preview` 포함)
- 수정: `feature/entry/build.gradle.kts` (`implementation(project(":core:common"))` 추가)
- 수정: `feature/entry/src/test/java/.../EntryViewModelTest.kt` (아래 Acceptance의 신규 로직 테스트 추가)
- 수정: `docs/PROJECT.md` — `:feature:entry` 모듈 맵 행 설명에 온보딩 설문(닉네임/경력/선호) 추가
- (완료) `docs/BACKLOG.md` — Claude가 계획 단계에서 이미 추가함, Codex는 추가 수정 불필요

## Acceptance criteria
- [ ] `EntryStep` 6단계가 `LOCATION→TERMS→PRECAUTIONS→NICKNAME→CAREER→PREFERENCE` 순서로 전환되고
      `back()`은 정확히 역순으로 동작
- [ ] `NICKNAME` 진입 시 닉네임이 1회만 랜덤 생성되고, 뒤로 갔다가 다시 와도 같은 값 유지
- [ ] `CAREER`: Q1~Q3 중 하나라도 미응답이면 "다음" 비활성
- [ ] `CAREER`: `roadExperience`를 "혼자 연습"으로 선택하면 조건부 질문 2개가 나타나고, 그 상태에서
      "다음"은 두 조건부 질문까지 모두 답해야 활성화됨
- [ ] `CAREER`: "혼자 연습" 선택 후 다른 값으로 바꾸면 조건부 질문이 사라지고 그 값들이 초기화됨
- [ ] `PREFERENCE`: 상황 칩은 최대 3개까지만 선택되고, 선택 순서대로 1/2/3 배지가 표시되며, 이미 3개
      선택된 상태에서 다른 칩을 눌러도 무시됨
- [ ] `PREFERENCE`: "다음"은 상황 1개 이상 + 차종 선택 시에만 활성화(목표 텍스트 유무 무관),
      "건너뛰기"는 항상 활성화
- [ ] `PREFERENCE`: "다음"/"건너뛰기" 둘 다 `OnboardingProfile`을 저장한 뒤 온보딩 완료 처리(기존
      `EntryPreferences.setCompleted` 그대로 호출)까지 이어짐
- [ ] 온보딩 데이터는 서버 전송 없이 `OnboardingPreferences`(DataStore)에만 저장됨
- [ ] `RodiSelectableChip`은 `core:ui`에 추가되고 `@Preview(showBackground = true, widthDp = 360)`가
      최소 1개 있음(미선택/선택/배지 상태 확인 가능하게)
- [ ] `RodiButton`/`EntryScaffold` 변경 후에도 기존 위치권한/약관/주의사항 3화면은 시각적으로 전혀
      달라지지 않음
- [ ] 새 화면 전부 `RodiTheme.colors`/`RodiTheme.typography` 토큰만 사용, Material 아이콘 미사용
- [ ] `EntryViewModelTest.kt`에 최소 아래 케이스 테스트 추가: 6단계 전진/역진 전환, "혼자 연습" 선택→
      조건부 질문 노출→다른 값으로 변경 시 초기화, 상황 칩 최대 3개 제한(4번째 무시)
- [ ] `./gradlew assembleDebug` 성공
- [ ] `./gradlew test` 성공

## Verification
```
./gradlew assembleDebug
./gradlew :core:domain:build :core:data:build :core:common:build :core:ui:build :feature:entry:build
./gradlew test
```

## Out of scope
- 온보딩 설문/닉네임 서버 API 연동 (BACKLOG 기록됨 — 지금은 로컬 저장/로컬 랜덤 생성)
- 온보딩 답변 점수 계산·합산·서버 전송 ("레벨 관련 정보", 배점 미확정, BACKLOG 기록됨)
- 닉네임 마이페이지 수정 UI (BACKLOG 기록됨)
- `PracticeSituation`↔`PracticeTag`(Course) 통합/매핑 (BACKLOG 기록됨)
- `NicknameGenerator` 단어 리스트 최종 콘텐츠 검수 (BACKLOG 기록됨, 지금은 placeholder 10+10개)
- 코스 추천 로직에 온보딩 데이터 반영 (이번엔 저장까지만)

---
## Codex Result   <!-- Codex가 구현 후 채움 → Status=IMPL_DONE (또는 막히면 BLOCKED) -->
- Changed files: core/domain/src/main/kotlin/com/dororong/rodi/core/domain/OnboardingProfile.kt, core/domain/src/main/kotlin/com/dororong/rodi/core/domain/OnboardingRepository.kt, core/domain/src/main/kotlin/com/dororong/rodi/core/domain/usecase/SaveOnboardingProfileUseCase.kt, core/data/src/main/java/com/dororong/rodi/core/data/OnboardingPreferences.kt, core/data/src/main/java/com/dororong/rodi/core/data/OnboardingRepositoryImpl.kt, core/data/src/main/java/com/dororong/rodi/core/data/di/DataModule.kt, core/common/src/main/kotlin/com/dororong/rodi/core/common/NicknameGenerator.kt, core/ui/src/main/java/com/dororong/rodi/core/ui/components/RodiSelectableChip.kt, core/ui/src/main/java/com/dororong/rodi/core/ui/components/RodiButton.kt, feature/entry/build.gradle.kts, feature/entry/src/main/java/com/dororong/rodi/feature/entry/EntryComponents.kt, feature/entry/src/main/java/com/dororong/rodi/feature/entry/EntryFlow.kt, feature/entry/src/main/java/com/dororong/rodi/feature/entry/EntryViewModel.kt, feature/entry/src/main/java/com/dororong/rodi/feature/entry/NicknameContent.kt, feature/entry/src/main/java/com/dororong/rodi/feature/entry/CareerContent.kt, feature/entry/src/main/java/com/dororong/rodi/feature/entry/PreferenceContent.kt, feature/entry/src/test/java/com/dororong/rodi/feature/entry/EntryViewModelTest.kt, docs/PROJECT.md, docs/handoff/HANDOFF.md
- Build/test: ./gradlew :core:domain:build :core:data:build :core:common:build :core:ui:build :feature:entry:build GREEN; ./gradlew assembleDebug GREEN; ./gradlew test GREEN
- Open questions: none

---
## Claude Review  <!-- Claude가 검토 후 채움 -->
- Blocking: 없음
- Nits:
  - `PreferenceContent.kt`의 `GoalQuestion` `OutlinedTextField`에 `.border(1.dp, gray300, ...)` 모디파이어가
    `colors`/`shape`로 이미 그려지는 내장 테두리 위에 중복 적용돼 있었음(포커스 시 내장 테두리만
    `primary600`으로 바뀌고 이 모디파이어는 정적 `gray300`이라 두 테두리가 겹쳐 보일 위험) — 리뷰 중
    직접 제거하고 미사용 `import androidx.compose.foundation.border`도 함께 정리, `:feature:entry:build`
    재검증 완료(그린).
  - `EntryViewModel.setGoal`이 스펙 이름 대신 `updateGoal`로 구현됨 — 기능은 동일하고 다른 콜사이트와
    이름 일관성도 깨지지 않아 그대로 둠(리네임 불필요).
  - `RodiSelectableChip`의 순서 배지가 스펙에서 말한 "살짝 겹치게"가 아니라 칩 안쪽에 딱 맞게 배치됨
    — 실기기 확인 결과 시각적으로 문제 없어 그대로 둠.
- Verdict: APPROVE

**검증 상세 (2026-07-04):**
- 코드 대조: `EntryStep`/`EntryViewModel`/`EntryFlow`/`EntryComponents`/`RodiButton`/`RodiSelectableChip`/
  `NicknameContent`/`CareerContent`/`PreferenceContent`/`OnboardingProfile`/`OnboardingRepository`(+Impl)/
  `SaveOnboardingProfileUseCase`/`OnboardingPreferences`/`DataModule`/`NicknameGenerator` 모두 Spec과 diff
  대조 완료, 스펙 이탈 없음(사소한 Nit 3건 제외)
- PROJECT.md 컨벤션 점검: 테마 토큰만 사용(하드코딩 `Color(0x...)` grep 결과 없음), Material 아이콘
  미사용(`Icons.` grep 결과 없음), 시크릿 노출 없음, 스코프 이탈 없음
- 빌드 재검증(독립 실행): `./gradlew :core:domain:build :core:data:build :core:common:build :core:ui:build
  :feature:entry:build` GREEN, `./gradlew assembleDebug` GREEN, `./gradlew test` GREEN — Codex 보고와 일치
- 에뮬레이터(emulator-5554) 실기기 검증: 앱 완전 재설치 후 LOCATION→TERMS→PRECAUTIONS(기존 3화면 시각
  변화 없음 확인) → NICKNAME(닉네임 랜덤 생성 확인) → CAREER("혼자 연습" 선택 시 조건부 질문 2개 노출,
  다른 값으로 변경 시 초기화 확인) → PREFERENCE(상황 칩 3개 선택 시 순서 배지 1/2/3 정상 표시, 4번째
  탭 무시 확인, 차종 선택 전 "다음" 비활성 확인) → "다음" 클릭 → Home 정상 진입까지 전 과정 수동 확인
- `adb run-as ... cat files/datastore/onboarding.preferences_pb`로 저장값이 실제 탭한 선택지와 정확히
  일치함을 확인(driving_period=UNDER_1_MONTH, recent_frequency=RARELY, road_experience=WITH_COMPANION,
  practice_situations=[U_TURN,TURN,PARKING] 순서 보존, vehicle_type=SMALL)
- Nit 수정 1건 반영 후 `:feature:entry:build` 재검증 그린
