package com.dororong.rodi.core.domain.model.onboarding

data class OnboardingForm(val steps: List<OnboardingFormStep>) {
    fun visibleQuestions(step: OnboardingFormStep, profile: OnboardingProfile): List<OnboardingQuestion> {
        val visible = mutableListOf<OnboardingQuestion>()
        for (question in step.questions) {
            val condition = question.showWhen
            if (condition != null && profile.answers(condition.question).none { it in condition.anyOf }) continue
            visible += question
            if (question.options.any { it.skipsFollowUps && it.code in profile.answers(question.code) }) break
        }
        return visible
    }

    fun isStepValid(step: OnboardingFormStep, profile: OnboardingProfile): Boolean =
        visibleQuestions(step, profile).all { question ->
            val answers = profile.answers(question.code)
            (!question.required || answers.isNotEmpty()) &&
                (question.maxSelect == null || answers.size <= question.maxSelect) &&
                (answers.size <= 1 || question.options.none { it.exclusive && it.code in answers })
        }

    fun skipsFollowUps(step: OnboardingFormStep, profile: OnboardingProfile): Boolean =
        visibleQuestions(step, profile).any { question ->
            question.options.any { it.skipsFollowUps && it.code in profile.answers(question.code) }
        }
}

data class OnboardingFormStep(
    val order: Int,
    val skippable: Boolean,
    val title: String,
    val description: String,
    val questions: List<OnboardingQuestion>,
)

enum class OnboardingQuestionType { SINGLE_SELECT, MULTI_SELECT, RANKED_SELECT, TEXT }

data class OnboardingQuestion(
    val code: String,
    val order: Int,
    val type: OnboardingQuestionType,
    val required: Boolean,
    val title: String,
    val description: String? = null,
    val placeholder: String? = null,
    val maxSelect: Int? = null,
    val maxLength: Int? = null,
    val showWhen: OnboardingShowWhen? = null,
    val options: List<OnboardingOption> = emptyList(),
)

data class OnboardingOption(
    val code: String,
    val label: String,
    val order: Int,
    val exclusive: Boolean = false,
    val skipsFollowUps: Boolean = false,
)

data class OnboardingShowWhen(val question: String, val anyOf: List<String>)

fun OnboardingProfile.answers(question: String): List<String> = when (question) {
    "drivingPeriod" -> listOfNotNull(drivingPeriod?.answerCode())
    "recentFrequency" -> listOfNotNull(recentFrequency?.answerCode())
    "roadExperiences" -> roadExperiences.map { it.answerCode() }
    "soloDrivingRange" -> listOfNotNull(soloDrivingRange?.answerCode())
    "soloParkingLevel" -> listOfNotNull(soloParkingLevel?.answerCode())
    "practiceTypes" -> practiceSituations.map { it.answerCode() }
    "carType" -> listOfNotNull(vehicleType?.answerCode())
    "drivingGoal" -> listOf(goal).filter(String::isNotBlank)
    else -> emptyList()
}

fun localOnboardingForm(): OnboardingForm {
    fun <T> options(values: List<T>, code: (T) -> String, label: (T) -> String): List<OnboardingOption> =
        values.mapIndexed { index, value -> OnboardingOption(code(value), label(value), index + 1) }
    val detailedCareer = OnboardingShowWhen("drivingPeriod", DrivingPeriod.entries.filterNot { it.isNavigatorLevel }.map { it.answerCode() })
    val soloOnly = OnboardingShowWhen("roadExperiences", listOf("SOLO"))
    return OnboardingForm(listOf(
        OnboardingFormStep(
            order = 1,
            skippable = false,
            title = "운전 경험에 대해 알려주세요.",
            description = "자세히 입력할수록 더 잘 맞는 연습 장소를 추천해요.",
            questions = listOf(
                OnboardingQuestion("drivingPeriod", 1, OnboardingQuestionType.SINGLE_SELECT, true,
                    "면허 취득 후 실제 운전한 기간을 알려주세요",
                    options = options(DrivingPeriod.entries, DrivingPeriod::answerCode, DrivingPeriod::label)
                        .map { it.copy(skipsFollowUps = it.code in listOf("YEARS_3_9", "OVER_10_YEARS")) }),
                OnboardingQuestion("recentFrequency", 2, OnboardingQuestionType.SINGLE_SELECT, true,
                    "가장 최근, 운전을 언제 하셨나요?", showWhen = detailedCareer,
                    options = options(RecentDrivingFrequency.entries, RecentDrivingFrequency::answerCode, RecentDrivingFrequency::label)),
                OnboardingQuestion("roadExperiences", 3, OnboardingQuestionType.MULTI_SELECT, true,
                    "면허 취득후 도로주행을 해본 적이 있나요?", showWhen = detailedCareer,
                    options = options(RoadExperience.entries, RoadExperience::answerCode, RoadExperience::label)
                        .map { it.copy(exclusive = it.code == "NONE") }),
                OnboardingQuestion("soloDrivingRange", 4, OnboardingQuestionType.SINGLE_SELECT, true,
                    "혼자 운전, 어디까지 해봤나요?", showWhen = soloOnly,
                    options = options(SoloDrivingRange.entries, SoloDrivingRange::answerCode, SoloDrivingRange::label)),
                OnboardingQuestion("soloParkingLevel", 5, OnboardingQuestionType.SINGLE_SELECT, true,
                    "혼자 주차는 어느 정도 해봤나요?", showWhen = soloOnly,
                    options = options(SoloParkingLevel.entries, SoloParkingLevel::answerCode, SoloParkingLevel::label)),
            ),
        ),
        OnboardingFormStep(
            order = 2,
            skippable = true,
            title = "추가 정보를 입력하면 더 정확해요.",
            description = "딱 맞는 코스 추천을 위한 선택항목이에요.",
            questions = listOf(
                OnboardingQuestion("practiceTypes", 6, OnboardingQuestionType.RANKED_SELECT, false,
                    "더 연습해보고 싶은 상황이 있나요?", description = "1순위부터 순서대로 선택해주세요.", maxSelect = 3,
                    options = options(PracticeSituation.entries, PracticeSituation::answerCode, PracticeSituation::label)),
                OnboardingQuestion("carType", 7, OnboardingQuestionType.SINGLE_SELECT, false,
                    "주로 타는 차종은 무엇인가요?",
                    options = options(VehicleType.entries, VehicleType::answerCode, VehicleType::label)),
                OnboardingQuestion("drivingGoal", 8, OnboardingQuestionType.TEXT, false,
                    "이루고 싶은 운전 목표를 입력해주세요.", placeholder = "ex)강남 운전 자신있게 하기!", maxLength = 30),
            ),
        ),
    ))
}

fun OnboardingProfile.withAnswers(question: String, answers: List<String>): OnboardingProfile = when (question) {
    "drivingPeriod" -> copy(drivingPeriod = DrivingPeriod.entries.firstOrNull { it.answerCode() in answers })
    "recentFrequency" -> copy(recentFrequency = RecentDrivingFrequency.entries.firstOrNull { it.answerCode() in answers })
    "roadExperiences" -> copy(roadExperiences = answers.mapNotNull { code -> RoadExperience.entries.firstOrNull { it.answerCode() == code } })
    "soloDrivingRange" -> copy(soloDrivingRange = SoloDrivingRange.entries.firstOrNull { it.answerCode() in answers })
    "soloParkingLevel" -> copy(soloParkingLevel = SoloParkingLevel.entries.firstOrNull { it.answerCode() in answers })
    "practiceTypes" -> copy(practiceSituations = answers.mapNotNull { code -> PracticeSituation.entries.firstOrNull { it.answerCode() == code } })
    "carType" -> copy(vehicleType = VehicleType.entries.firstOrNull { it.answerCode() in answers })
    "drivingGoal" -> copy(goal = answers.firstOrNull().orEmpty())
    else -> this
}

val OnboardingProfile.hasValidRoadExperiences: Boolean
    get() = RoadExperience.NONE !in roadExperiences || roadExperiences.size == 1

fun OnboardingProfile.normalizeRoadExperiences(): OnboardingProfile {
    val experiences = roadExperiences.distinct().let {
        if (it.size > 1) it - RoadExperience.NONE else it
    }
    return copy(
        roadExperiences = experiences,
        soloDrivingRange = soloDrivingRange.takeIf { RoadExperience.SOLO in experiences },
        soloParkingLevel = soloParkingLevel.takeIf { RoadExperience.SOLO in experiences },
    )
}

fun OnboardingProfile.conformTo(form: OnboardingForm): OnboardingProfile {
    var profile = normalizeRoadExperiences()
    val questions = form.steps.flatMap { it.questions }
    for (question in questions) {
        if (question.type == OnboardingQuestionType.TEXT) continue
        var answers = profile.answers(question.code).filter { code -> question.options.any { it.code == code } }.distinct()
        if (answers.size > 1 && question.options.any { it.exclusive && it.code in answers }) {
            val ordinary = answers.filter { code -> question.options.none { it.code == code && it.exclusive } }
            answers = ordinary.ifEmpty { answers.take(1) }
        }
        profile = profile.withAnswers(question.code, answers.take(question.maxSelect ?: Int.MAX_VALUE))
    }
    val visibleCodes = form.steps.flatMap { form.visibleQuestions(it, profile) }.map { it.code }.toSet()
    for (question in localOnboardingForm().steps.flatMap { it.questions }) {
        if (question.code !in visibleCodes) profile = profile.withAnswers(question.code, emptyList())
    }
    return profile.normalizeRoadExperiences()
}
