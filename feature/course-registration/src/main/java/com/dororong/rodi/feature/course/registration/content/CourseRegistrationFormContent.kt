package com.dororong.rodi.feature.course.registration.content

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dororong.rodi.core.common.graphemeLength
import com.dororong.rodi.core.domain.model.course.CourseInputSpec
import com.dororong.rodi.core.domain.model.course.CoursePracticeCategory
import com.dororong.rodi.core.domain.model.course.CoursePracticeType
import com.dororong.rodi.core.domain.model.course.CourseRegistrationForm
import com.dororong.rodi.core.domain.model.course.CourseRegistrationSections
import com.dororong.rodi.core.domain.model.course.GeoPoint
import com.dororong.rodi.core.domain.model.course.RegistrationWaypoint
import com.dororong.rodi.core.domain.model.course.RegistrationWaypointType
import com.dororong.rodi.core.domain.model.course.RouteResult
import com.dororong.rodi.core.ui.R as CoreUiR
import com.dororong.rodi.core.ui.components.RodiSelectableChip
import com.dororong.rodi.core.ui.components.RodiSkeleton
import com.dororong.rodi.core.ui.components.button.RodiButton
import com.dororong.rodi.core.ui.components.button.RodiIconButton
import com.dororong.rodi.core.ui.components.button.RodiButtonVariant
import com.dororong.rodi.core.ui.components.snackbar.RodiSnackbarData
import com.dororong.rodi.core.ui.components.snackbar.RodiSnackbarHost
import com.dororong.rodi.core.ui.components.snackbar.RodiSnackbarHostState
import com.dororong.rodi.core.ui.theme.RodiRadius
import com.dororong.rodi.core.ui.theme.RodiSpacing
import com.dororong.rodi.core.ui.theme.RodiTheme
import com.dororong.rodi.core.ui.components.input.rodiCursorBrush
import com.dororong.rodi.core.ui.components.input.rodiInputBorderColor
import com.dororong.rodi.feature.course.registration.CourseRegistrationFormLoadState
import com.dororong.rodi.feature.course.registration.CourseRegistrationIntent

@Composable
fun CourseRegistrationFormContent(
    loadState: CourseRegistrationFormLoadState,
    form: CourseRegistrationForm?,
    waypoints: List<RegistrationWaypoint>,
    route: RouteResult?,
    selectedCategoryCode: String?,
    selectedPracticeTypeCodes: List<String>,
    caution: String,
    description: String,
    isSubmitting: Boolean,
    showValidationErrors: Boolean = false,
    canSubmit: Boolean,
    error: String?,
    onIntent: (CourseRegistrationIntent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(RodiTheme.colors.white)
            .statusBarsPadding(),
    ) {
        CourseRegistrationFormTopBar(onBack = onBack)
        when (loadState) {
            CourseRegistrationFormLoadState.Loading -> CourseRegistrationFormSkeleton(Modifier.weight(1f))
            CourseRegistrationFormLoadState.Error -> CourseRegistrationFormError(
                onRetry = { onIntent(CourseRegistrationIntent.Retry) },
                modifier = Modifier.weight(1f),
            )
            CourseRegistrationFormLoadState.Ready -> form?.let {
                Box(Modifier.weight(1f)) {
                    CourseRegistrationFormFields(
                        form = it,
                        selectedCategoryCode = selectedCategoryCode,
                        selectedPracticeTypeCodes = selectedPracticeTypeCodes,
                        caution = caution,
                        description = description,
                        showValidationErrors = showValidationErrors,
                        onIntent = onIntent,
                    )
                }
                CourseRegistrationFormSubmitBar(
                    enabled = canSubmit && !isSubmitting,
                    onSubmit = { onIntent(CourseRegistrationIntent.Submit) },
                )
            }
        }
    }
}

@Composable
private fun CourseRegistrationFormTopBar(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = RodiSpacing.md),
    ) {
        RodiIconButton(
            painter = painterResource(CoreUiR.drawable.ic_chevron_left),
            onClick = onBack,
            contentDescription = "지도 선택으로 돌아가기",
            tint = RodiTheme.colors.black,
            modifier = Modifier.align(Alignment.CenterStart),
        )
        Text(
            text = "코스 등록",
            style = RodiTheme.typography.headline1,
            color = RodiTheme.colors.black,
            modifier = Modifier.align(Alignment.Center),
        )
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun CourseRegistrationFormFields(
    form: CourseRegistrationForm,
    selectedCategoryCode: String?,
    selectedPracticeTypeCodes: List<String>,
    caution: String,
    description: String,
    showValidationErrors: Boolean,
    onIntent: (CourseRegistrationIntent) -> Unit,
) {
    val categories = form.categories.sortedBy(CoursePracticeCategory::order)
    // 선택한 카테고리 하나의 연습유형만 노출한다. 다른 카테고리에서 이미 고른 연습유형은
    // (지금 안 보여도) selectedPracticeTypeCodes에 그대로 남아 완료 조건에 계속 반영된다.
    val visiblePracticeTypes = categories
        .filter { it.code == selectedCategoryCode }
        .flatMap { it.practiceTypes.sortedBy(CoursePracticeType::order) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = RodiSpacing.md,
            end = RodiSpacing.md,
            top = 24.dp,
            bottom = 24.dp,
        ),
        // 섹션 사이 40dp. 디자인(4164:11572) 기준으로, 24dp일 때 "기본정보"와 첫 섹션이 붙어 보였다.
        verticalArrangement = Arrangement.spacedBy(40.dp),
    ) {
        item {
            Text(
                text = form.sections.basicInfo,
                style = RodiTheme.typography.heading2,
                color = RodiTheme.colors.black,
            )
        }
        item {
            FormSection(title = form.sections.practiceCategory, required = true) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    categories.forEach { category ->
                        RodiSelectableChip(
                            text = category.label,
                            selected = category.code == selectedCategoryCode,
                            onClick = { onIntent(CourseRegistrationIntent.SelectCategory(category.code)) },
                        )
                    }
                }
            }
        }
        if (visiblePracticeTypes.isNotEmpty()) {
            item(key = "practice-types-$selectedCategoryCode") {
                FormSection(title = form.sections.practiceType, required = true) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        visiblePracticeTypes.forEach { type ->
                            PracticeTypeChip(
                                type = type,
                                selected = type.code in selectedPracticeTypeCodes,
                                onClick = { onIntent(CourseRegistrationIntent.TogglePracticeType(type.code)) },
                            )
                        }
                    }
                }
            }
        }
        item {
            FormSection(title = form.sections.caution) {
                RodiInputField(
                    value = caution,
                    spec = form.cautionInput,
                    onValueChanged = { onIntent(CourseRegistrationIntent.CautionChanged(it)) },
                    label = form.sections.caution,
                    showValidationError = showValidationErrors,
                    showCounter = false,
                    minLines = 1,
                )
            }
        }
        item {
            FormSection(title = form.sections.description, required = true) {
                RodiInputField(
                    value = description,
                    spec = form.descriptionInput,
                    onValueChanged = { onIntent(CourseRegistrationIntent.DescriptionChanged(it)) },
                    label = form.sections.description,
                    // 서버 문구는 예시문이라 10자 조건이 드러나지 않는다. 이 조건은 완료를 눌러야
                    // 알 수 있어서, 디자인대로 입력 전에 미리 알려주는 문구를 쓴다.
                    placeholder = "최소 10자 이상 입력해주세요.",
                    showValidationError = showValidationErrors,
                    showCounter = true,
                    minLines = 1,
                )
            }
        }
    }
}

@Composable
private fun FormSection(title: String, required: Boolean = false, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = if (required) {
                buildAnnotatedString {
                    append("$title ")
                    withStyle(SpanStyle(color = RodiTheme.colors.primary600)) { append("*") }
                }
            } else {
                AnnotatedString(title)
            },
            style = RodiTheme.typography.body1SemiBold,
            color = RodiTheme.colors.black,
        )
        content()
    }
}

@Composable
private fun PracticeTypeChip(type: CoursePracticeType, selected: Boolean, onClick: () -> Unit) {
    RodiSelectableChip(
        text = type.label,
        selected = selected,
        onClick = onClick,
    )
}

@Composable
private fun RodiInputField(
    value: String,
    spec: CourseInputSpec,
    onValueChanged: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = spec.placeholder,
    minLines: Int = 2,
    showCounter: Boolean = true,
    showValidationError: Boolean = false,
) {
    var isFocused by remember { mutableStateOf(false) }
    var hasInteracted by rememberSaveable { mutableStateOf(false) }
    // 최소 글자 수는 입력 중 에러로 표시하지 않는다 — 완료를 눌렀을 때 안내 문구로 알리는 게 명세라
    // 여기서 빨간 테두리까지 띄우면 "1자 이상이면 완료 활성화"와 어긋나 보인다.
    // 글자 수는 사용자가 세는 단위(grapheme)로 보여준다 — 이모지 하나가 2로 세이던 문제.
    val graphemeCount = value.graphemeLength()
    val valid = if (value.isBlank()) !spec.required else graphemeCount <= spec.maxLength
    val shape = RoundedCornerShape(RodiRadius.sm)
    val showError = (showValidationError || hasInteracted) && !valid
    val borderColor = if (showError) RodiTheme.colors.pointRed else rodiInputBorderColor(isFocused)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        BasicTextField(
            value = value,
            onValueChange = {
                hasInteracted = true
                onValueChanged(it.take(spec.maxLength))
            },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged {
                    if (isFocused && !it.isFocused) hasInteracted = true
                    isFocused = it.isFocused
                }
                .clip(shape)
                .background(RodiTheme.colors.white, shape)
                .border(1.dp, borderColor, shape)
                .padding(16.dp)
                .semantics { contentDescription = label },
            textStyle = RodiTheme.typography.body3Regular.copy(color = RodiTheme.colors.black),
            cursorBrush = rodiCursorBrush(),
            minLines = minLines,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Default),
            decorationBox = { inner ->
                Box {
                    if (value.isBlank()) {
                        Text(
                            text = placeholder,
                            style = RodiTheme.typography.body3Regular,
                            color = RodiTheme.colors.gray500,
                        )
                    }
                    inner()
                }
            },
        )
        if (showCounter || showError) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                if (showError) {
                    Text(
                        text = "입력 길이를 확인해 주세요.",
                        style = RodiTheme.typography.caption2Regular,
                        color = RodiTheme.colors.pointRed,
                    )
                } else {
                    Spacer(Modifier.width(1.dp))
                }
                if (showCounter) {
                    Text(
                        text = "$graphemeCount /${spec.maxLength}",
                        style = RodiTheme.typography.caption2Regular,
                        color = RodiTheme.colors.gray500,
                    )
                }
            }
        }
    }
}

@Composable
private fun CourseRegistrationFormSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(RodiSpacing.md),
        verticalArrangement = Arrangement.spacedBy(RodiSpacing.md),
    ) {
        repeat(6) { index ->
            RodiSkeleton(
                modifier = Modifier.fillMaxWidth().height(if (index == 1) 120.dp else 48.dp),
                shape = RoundedCornerShape(RodiRadius.sm),
            )
        }
    }
}

@Composable
private fun CourseRegistrationFormSubmitBar(enabled: Boolean, onSubmit: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(RodiTheme.colors.white)
            // 키보드가 올라오면 ime 인셋만 남기고 내비바 인셋은 흡수시킨다. 둘을 따로 주면
            // 키보드 위로 내비바 높이만큼 흰 여백이 떠 보인다.
            .navigationBarsPadding()
            .imePadding(),
    ) {
        HorizontalDivider(color = RodiTheme.colors.gray200, thickness = 1.dp)
        Box(Modifier.fillMaxWidth().padding(horizontal = RodiSpacing.md, vertical = 10.dp)) {
            RodiButton(text = "완료", onClick = onSubmit, enabled = enabled)
        }
    }
}

@Composable
private fun CourseRegistrationFormError(onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().padding(RodiSpacing.xl), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("등록 양식을 불러오지 못했어요", style = RodiTheme.typography.body1SemiBold, color = RodiTheme.colors.black)
        Spacer(Modifier.height(12.dp))
        RodiButton(text = "다시 시도", onClick = onRetry, fillMaxWidth = false, modifier = Modifier.width(120.dp), height = 42.dp)
    }
}

@Preview(showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun CourseRegistrationFormContentPreview() {
    val type = CoursePracticeType("parking", "주차", 1)
    val form = CourseRegistrationForm(
        maxWaypoints = 5,
        sections = CourseRegistrationSections("기본정보", "연습유형 카테고리 고르기", "연습유형", "주의사항 작성", "한줄 소개"),
        practiceTypeMaxSelect = 3,
        practiceTypeMaxSelectExceededMessage = "최대 3개까지 선택할 수 있어요.",
        categories = listOf(CoursePracticeCategory("basic", "기본 주행", 1, listOf(type))),
        cautionInput = CourseInputSpec(false, maxLength = 100, placeholder = "주의사항을 입력해 주세요."),
        descriptionInput = CourseInputSpec(true, minLength = 10, maxLength = 30, placeholder = "최소 10자 이상 입력해주세요 ."),
    )
    RodiTheme {
        CourseRegistrationFormContent(
            loadState = CourseRegistrationFormLoadState.Ready,
            form = form,
            waypoints = listOf(RegistrationWaypoint(RegistrationWaypointType.START, "서울역", "서울 중구", lat = 37.5, lng = 126.9)),
            route = RouteResult(listOf(GeoPoint(37.5, 126.9), GeoPoint(37.51, 126.91)), true, 1500),
            selectedCategoryCode = "basic",
            selectedPracticeTypeCodes = listOf("parking"),
            caution = "",
            description = "안전하게 연습해요.",
            isSubmitting = false,
            canSubmit = true,
            error = null,
            onIntent = {},
            onBack = {},
        )
    }
}

private fun previewForm() = CourseRegistrationForm(
    maxWaypoints = 4,
    sections = CourseRegistrationSections("기본정보", "연습유형 카테고리 고르기", "연습유형", "주의사항 작성", "한줄 소개"),
    practiceTypeMaxSelect = 3,
    practiceTypeMaxSelectExceededMessage = "최대 3개까지 선택할 수 있어요.",
    categories = listOf(
        CoursePracticeCategory(
            code = "basic",
            label = "기초 주행",
            order = 1,
            practiceTypes = listOf(
                CoursePracticeType("straight", "직선주행", 1),
                CoursePracticeType("turn", "좌･우회전", 2),
                CoursePracticeType("lane", "차선변경", 3),
            ),
        ),
        CoursePracticeCategory(
            code = "parking",
            label = "주차",
            order = 2,
            practiceTypes = listOf(
                CoursePracticeType("parallel", "평행주차", 1),
            ),
        ),
    ),
    cautionInput = CourseInputSpec(false, maxLength = 100, placeholder = "예) 갑자기 나오는 자전거 주의!"),
    descriptionInput = CourseInputSpec(true, minLength = 10, maxLength = 30, placeholder = "최소 10자 이상 입력해주세요 ."),
)

private val previewFormWaypoints = listOf(
    RegistrationWaypoint(RegistrationWaypointType.START, "서울역", "서울 중구 한강대로", lat = 37.5547, lng = 126.9707),
    RegistrationWaypoint(RegistrationWaypointType.DESTINATION, "남산", "서울 중구 남산공원길", lat = 37.5512, lng = 126.9882),
)

private val previewFormRoute = RouteResult(
    points = listOf(GeoPoint(37.5547, 126.9707), GeoPoint(37.5512, 126.9882)),
    isRealRoute = true,
    totalDistanceMeters = 2100,
    snappedPoints = previewFormWaypoints.map { GeoPoint(it.lat, it.lng) },
)

@Composable
private fun PreviewFormState(
    loadState: CourseRegistrationFormLoadState = CourseRegistrationFormLoadState.Ready,
    caution: String = "",
    description: String = "",
    selectedCategory: String? = "basic",
    selected: List<String> = listOf("straight"),
    submitting: Boolean = false,
    canSubmit: Boolean = false,
    error: String? = null,
    showValidationErrors: Boolean = false,
) {
    RodiTheme {
        CourseRegistrationFormContent(
            loadState = loadState,
            form = if (loadState == CourseRegistrationFormLoadState.Error) null else previewForm(),
            waypoints = previewFormWaypoints,
            route = previewFormRoute,
            selectedCategoryCode = selectedCategory,
            selectedPracticeTypeCodes = selected,
            caution = caution,
            description = description,
            isSubmitting = submitting,
            showValidationErrors = showValidationErrors,
            canSubmit = canSubmit,
            error = error,
            onIntent = {},
            onBack = {},
        )
    }
}

@Preview(name = "Form Loading", showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun CourseRegistrationFormLoadingPreview() {
    PreviewFormState(loadState = CourseRegistrationFormLoadState.Loading)
}

@Preview(name = "Form Empty", showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun CourseRegistrationFormEmptyPreview() {
    PreviewFormState(selected = emptyList())
}

@Preview(name = "Form Selection Carried Across Category", showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun CourseRegistrationFormCarriedSelectionPreview() {
    // 다른 카테고리("basic")에서 고른 연습유형("straight")이 있는 채로 지금은 다른
    // 카테고리를 보고 있는 상태 — 보이는 칩 중엔 선택된 게 없어도 완료 버튼은 켜져 있다.
    PreviewFormState(selectedCategory = "parking", selected = listOf("straight"), canSubmit = true)
}

@Preview(name = "Form Max Snackbar", showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun CourseRegistrationFormMaxSnackbarPreview() {
    val snackbarState = androidx.compose.runtime.remember {
        RodiSnackbarHostState().apply {
            show(RodiSnackbarData(message = "최대 3개까지 선택할 수 있어요."))
        }
    }
    Box(Modifier.fillMaxSize()) {
        PreviewFormState(selected = listOf("straight", "turn", "lane"), error = "최대 3개까지 선택할 수 있어요.")
        RodiSnackbarHost(snackbarState)
    }
}

@Preview(name = "Form Focused", showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun CourseRegistrationFormFocusedPreview() {
    PreviewFormState(description = "직진, 좌회전 연습하기 좋아요.")
}

@Preview(name = "Form Invalid", showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun CourseRegistrationFormInvalidPreview() {
    PreviewFormState(description = "짧아요", showValidationErrors = true)
}

@Preview(name = "Form Max", showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun CourseRegistrationFormMaxPreview() {
    PreviewFormState(description = "가".repeat(200), caution = "가".repeat(100))
}

@Preview(name = "Form Valid", showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun CourseRegistrationFormValidPreview() {
    PreviewFormState(
        caution = "주택가라 아이들이 많아요.",
        description = "직진, 좌회전 우회전 연습하기 좋아요!",
        canSubmit = true,
    )
}
