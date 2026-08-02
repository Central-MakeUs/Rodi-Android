package com.dororong.rodi.core.domain.model.onboarding

import com.dororong.rodi.core.domain.model.place.PracticeType

data class OnboardingAnalysisCopy(
    val distanceExpression: String?,
    val frequencyExpression: String?,
    val stageSentence: String,
) {
    val text: String
        get() = if (distanceExpression == null || frequencyExpression == null) {
            stageSentence
        } else {
            "${distanceExpression}는 $frequencyExpression 나가는데,\n$stageSentence"
        }
}

fun OnboardingProfile.analysisCopy(level: OnboardingLevel = calculateLevel()): OnboardingAnalysisCopy {
    if (level == OnboardingLevel.NAVIGATOR) {
        return OnboardingAnalysisCopy(
            distanceExpression = null,
            frequencyExpression = null,
            stageSentence = "길잡이로 함께해요. 익숙한 운전 경험을 바탕으로 다른 운전자에게 도움이 되는 코스를 남겨보세요.",
        )
    }
    return OnboardingAnalysisCopy(
        distanceExpression = if (RoadExperience.SOLO in roadExperiences) {
            when (soloDrivingRange) {
                SoloDrivingRange.NEAR_HOME -> "집 근처"
                SoloDrivingRange.FAMILIAR_ROAD -> "자주 다니는 코스"
                SoloDrivingRange.UNFAMILIAR_ROAD -> "낯선 길까지"
                SoloDrivingRange.HIGHWAY_LONG_DISTANCE -> "멀리까지"
                null -> "혼자서"
            }
        } else {
            "혼자서"
        },
        frequencyExpression = when (recentFrequency) {
            RecentDrivingFrequency.RARELY, null -> "잘 안"
            RecentDrivingFrequency.MONTHLY_1_TO_2 -> "가끔"
            RecentDrivingFrequency.WEEKLY_1 -> "종종"
            RecentDrivingFrequency.WEEKLY_2_TO_3 -> "자주"
            RecentDrivingFrequency.WEEKLY_4_PLUS -> "거의 매일"
        },
        stageSentence = level.stageSentence,
    )
}

val OnboardingLevel.recommendations: List<String>
    get() = when (this) {
        OnboardingLevel.SEED -> listOf("직선주행", "좌우회전", "차선변경")
        OnboardingLevel.ROOKIE -> listOf("유턴", "교차로", "주차")
        OnboardingLevel.OWNER -> listOf("고속도로", "합류", "다차로주행")
        OnboardingLevel.EXPLORER -> listOf("비보호좌회전", "회전교차로", "좁은도로", "코너링")
        OnboardingLevel.NAVIGATOR -> listOf("코스 등록", "리뷰 작성", "추천 코스 공유")
    }

val OnboardingLevel.initialFilterTags: Set<PracticeType>
    get() = when (this) {
        OnboardingLevel.SEED -> setOf(
            PracticeType.STRAIGHT,
            PracticeType.LEFT_RIGHT_TURN,
            PracticeType.LANE_CHANGE,
        )
        OnboardingLevel.ROOKIE -> setOf(
            PracticeType.U_TURN,
            PracticeType.INTERSECTION,
            PracticeType.PARKING,
        )
        OnboardingLevel.OWNER -> setOf(
            PracticeType.HIGHWAY_ENTRY,
            PracticeType.MERGING,
            PracticeType.MULTILANE,
        )
        OnboardingLevel.EXPLORER -> setOf(
            PracticeType.UNPROTECTED_LEFT_TURN,
            PracticeType.ROUNDABOUT,
            PracticeType.NARROW_ROAD,
            PracticeType.CORNERING,
        )
        OnboardingLevel.NAVIGATOR -> emptySet()
    }

private val OnboardingLevel.stageSentence: String
    get() = when (this) {
        OnboardingLevel.SEED -> "도로에서 직접 핸들 잡는 게 아직 낯설어요."
        OnboardingLevel.ROOKIE -> "교차로·유턴이 아직 긴장돼요."
        OnboardingLevel.OWNER -> "고속도로 합류·다차로 주행이 아직 어려워요."
        OnboardingLevel.EXPLORER -> "더 다양한 상황들을 연습하고 싶어요."
        OnboardingLevel.NAVIGATOR -> error("Navigator uses a fixed analysis sentence.")
    }
