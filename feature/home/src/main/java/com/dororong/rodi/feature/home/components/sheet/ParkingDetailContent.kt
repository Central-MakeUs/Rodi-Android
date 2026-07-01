package com.dororong.rodi.feature.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dororong.rodi.core.data.SampleCourses
import com.dororong.rodi.core.domain.Course
import com.dororong.rodi.core.domain.ParkingDetail
import com.dororong.rodi.core.ui.theme.RodiTheme

@Composable
fun ParkingDetailContent(
    course: Course,
    onDismiss: () -> Unit,
    onNavigate: () -> Unit,
    modifier: Modifier = Modifier,
    initialAddressExpanded: Boolean = false,
    initialHoursExpanded: Boolean = false,
) {
    val parking = course.parkingDetail
    var addressExpanded by rememberSaveable(course.id) { mutableStateOf(initialAddressExpanded) }
    var hoursExpanded by rememberSaveable(course.id) { mutableStateOf(initialHoursExpanded) }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                course.title,
                style = RodiTheme.typography.headline1,
                color = RodiTheme.colors.black,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            IconButton(onClick = onDismiss) {
                Icon(
                    painter = painterResource(R.drawable.ic_x),
                    contentDescription = "닫기",
                    tint = RodiTheme.colors.black,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                RatingRegionRow(
                    rating = course.rating,
                    region = course.regionDisplay,
                    onChevronClick = { addressExpanded = !addressExpanded },
                )
                if (addressExpanded) {
                    ExpandableAddressCard(
                        roadAddress = course.roadAddress.shortenRoadAddress(),
                        jibunAddress = course.jibunAddress.shortenJibunAddress(),
                    )
                } else {
                    ParkingMetaRow(
                        parking = parking,
                        hoursExpanded = hoursExpanded,
                        onHoursClick = { hoursExpanded = !hoursExpanded },
                    )
                    if (hoursExpanded) {
                        ParkingHoursRows(parking = parking)
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                    ParkingCapacityRow(capacity = parking?.capacity)
                    DifficultyTag(course.difficultyEnum)
                }
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(thickness = 1.dp, color = RodiTheme.colors.primary100)
            Spacer(Modifier.height(13.dp))

            ParkingFeeSection(
                parking = parking,
                modifier = Modifier.padding(horizontal = 16.dp),
            )

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = onNavigate,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .height(48.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = RodiTheme.colors.primary600,
                    contentColor = RodiTheme.colors.white,
                ),
            ) {
                Text("경로 안내", style = RodiTheme.typography.button1)
            }
        }
    }
}

@Composable
private fun ParkingMetaRow(
    parking: ParkingDetail?,
    hoursExpanded: Boolean,
    onHoursClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = parking.parkingTypeDisplay(),
            style = RodiTheme.typography.body3Medium,
            color = RodiTheme.colors.gray800,
        )
        Text("･", style = RodiTheme.typography.body3Medium, color = RodiTheme.colors.gray800)
        Row(
            modifier = Modifier.clickable(onClick = onHoursClick),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = parking.operatingSummary(),
                style = RodiTheme.typography.body3Medium,
                color = RodiTheme.colors.gray800,
            )
            Spacer(Modifier.width(4.dp))
            Icon(
                painter = painterResource(R.drawable.ic_chevron_down),
                contentDescription = if (hoursExpanded) "영업시간 접기" else "영업시간 보기",
                tint = RodiTheme.colors.gray800,
                modifier = Modifier
                    .size(14.dp)
                    .graphicsLayer { rotationZ = if (hoursExpanded) 180f else 0f },
            )
        }
    }
}

@Composable
private fun ParkingCapacityRow(capacity: Int?) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("총 주차 면수", style = RodiTheme.typography.body3Medium, color = RodiTheme.colors.gray800)
        Text("･", style = RodiTheme.typography.body3Medium, color = RodiTheme.colors.gray800)
        Text(
            text = capacity?.let { "${it}대" } ?: "해당항목없음",
            style = RodiTheme.typography.body3Medium,
            color = RodiTheme.colors.gray800,
        )
    }
}

@Composable
private fun ParkingHoursRows(parking: ParkingDetail?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 2.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        val hours = parking?.operatingHours
        ParkingInfoRow("평일", hours?.weekday.toDisplayHours())
        ParkingInfoRow("토요일", hours?.saturday.toDisplayHours())
        ParkingInfoRow("일요일", hours?.holiday.toDisplayHours())
        ParkingInfoRow("공휴일", hours?.holiday.toDisplayHours())
    }
}

@Composable
private fun ParkingFeeSection(
    parking: ParkingDetail?,
    modifier: Modifier = Modifier,
) {
    val fee = remember(parking?.feeInfo, parking?.isFree) { parking.toParkingFeeInfo() }
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("요금 안내", style = RodiTheme.typography.body3Medium, color = RodiTheme.colors.gray800)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ParkingInfoRow("초기무료", fee.initialFree)
            ParkingInfoRow("기본요금", fee.base)
            ParkingInfoRow("추가요금", fee.additional)
            ParkingInfoRow("할증기준시간", fee.surcharge)
        }
    }
}

@Composable
private fun ParkingInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = RodiTheme.typography.caption1Medium,
            color = RodiTheme.colors.gray800,
        )
        DashedInfoDivider(
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .weight(1f)
                .height(1.dp),
        )
        Text(
            text = value,
            style = RodiTheme.typography.body3SemiBold,
            color = RodiTheme.colors.gray800,
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun DashedInfoDivider(modifier: Modifier = Modifier) {
    val color = RodiTheme.colors.gray400.copy(alpha = 0.35f)
    Box(
        modifier = modifier.drawBehind {
            val segment = 6.dp.toPx()
            drawLine(
                color = color,
                start = Offset(0f, size.height / 2f),
                end = Offset(size.width, size.height / 2f),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(segment, segment), 0f),
            )
        },
    )
}

private data class ParkingFeeInfo(
    val initialFree: String,
    val base: String,
    val additional: String,
    val surcharge: String = "해당항목없음",
)

private fun ParkingDetail?.parkingTypeDisplay(): String {
    val type = this?.parkingType
    return when {
        this == null -> "공영주차장"
        isFree -> "무료 주차장"
        !type.isNullOrBlank() -> type
        else -> "공영주차장"
    }
}

private fun ParkingDetail?.operatingSummary(): String {
    val weekday = this?.operatingHours?.weekday.orEmpty()
    if (weekday.isBlank()) return "영업시간 정보 없음"
    val normalized = weekday.replace(" ", "")
    if (normalized.startsWith("00:00") && (normalized.endsWith("23:59") || normalized.endsWith("24:00"))) {
        return "24시간 영업"
    }
    return "${normalized.substringBefore("-")}에 영업 시작"
}

private fun String?.toDisplayHours(): String {
    val value = this?.trim().orEmpty()
    if (value.isBlank()) return "해당항목없음"
    return value.replace("-", " - ")
}

private fun ParkingDetail?.toParkingFeeInfo(): ParkingFeeInfo {
    if (this == null) {
        return ParkingFeeInfo(
            initialFree = "해당항목없음",
            base = "해당항목없음",
            additional = "해당항목없음",
        )
    }
    if (isFree) {
        return ParkingFeeInfo(
            initialFree = "무료",
            base = "무료",
            additional = "해당항목없음",
        )
    }

    val baseMinutes = feeInfo.extractFeeNumber("baseMinutes")
    val baseFee = feeInfo.extractFeeNumber("baseFee")
    val addMinutes = feeInfo.extractFeeNumber("addUnitMinutes")
    val addFee = feeInfo.extractFeeNumber("addUnitFee")

    return ParkingFeeInfo(
        initialFree = note?.takeIf { it.contains("무료") } ?: "해당항목없음",
        base = formatFee(baseMinutes, baseFee),
        additional = formatFee(addMinutes, addFee),
    )
}

private fun String?.extractFeeNumber(key: String): Int? {
    val value = this ?: return null
    val escapedKey = Regex.escape(key)
    val match = Regex("""["']$escapedKey["']\s*:\s*(\d+)""").find(value) ?: return null
    return match.groupValues[1].toIntOrNull()
}

private fun formatFee(minutes: Int?, fee: Int?): String {
    if (minutes == null || fee == null) return "해당항목없음"
    return "${minutes}분 ･ ${"%,d".format(fee)}원"
}

@Preview(name = "ParkingDetailContent - Free", showBackground = true, widthDp = 360, heightDp = 560)
@Composable
private fun ParkingDetailContentFreePreview() {
    BottomSheetPreviewWrapper {
        ParkingDetailContent(
            course = SampleCourses.RODI_COURSES.first { it.isParking && it.parkingDetail?.isFree == true },
            onDismiss = {},
            onNavigate = {},
        )
    }
}

@Preview(name = "ParkingDetailContent - Paid", showBackground = true, widthDp = 360, heightDp = 560)
@Composable
private fun ParkingDetailContentPaidPreview() {
    BottomSheetPreviewWrapper {
        ParkingDetailContent(
            course = SampleCourses.RODI_COURSES.first { it.isParking && it.parkingDetail?.isFree == false },
            onDismiss = {},
            onNavigate = {},
        )
    }
}

@Preview(name = "ParkingDetailContent - Address Expanded", showBackground = true, widthDp = 360, heightDp = 560)
@Composable
private fun ParkingDetailContentAddressExpandedPreview() {
    BottomSheetPreviewWrapper {
        ParkingDetailContent(
            course = SampleCourses.RODI_COURSES.first { it.isParking && it.parkingDetail?.isFree == false },
            onDismiss = {},
            onNavigate = {},
            initialAddressExpanded = true,
        )
    }
}

@Preview(name = "ParkingDetailContent - Hours Expanded", showBackground = true, widthDp = 360, heightDp = 560)
@Composable
private fun ParkingDetailContentHoursExpandedPreview() {
    BottomSheetPreviewWrapper {
        ParkingDetailContent(
            course = SampleCourses.RODI_COURSES.first { it.isParking && it.parkingDetail?.isFree == false },
            onDismiss = {},
            onNavigate = {},
            initialHoursExpanded = true,
        )
    }
}
