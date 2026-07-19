package com.dororong.rodi.feature.home.detail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.requiredSizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dororong.rodi.core.domain.model.place.ParkingFeeInfo
import com.dororong.rodi.core.domain.model.place.ParkingPlaceDetail
import com.dororong.rodi.core.domain.model.place.PlaceDetail
import com.dororong.rodi.core.ui.components.button.RodiButton
import com.dororong.rodi.core.ui.theme.RodiTheme
import com.dororong.rodi.feature.home.HomePreviewData
import com.dororong.rodi.feature.home.R
import java.text.NumberFormat
import java.util.Locale

@Composable
fun ParkingDetailContent(
    place: PlaceDetail,
    isBookmarkUpdating: Boolean,
    onDismiss: () -> Unit,
    onBookmarkClick: () -> Unit,
    onNavigate: () -> Unit,
    modifier: Modifier = Modifier,
    initialAddressExpanded: Boolean = false,
    initialHoursExpanded: Boolean = false,
) {
    val parking = place.parking ?: return
    var addressExpanded by rememberSaveable(place.id) { mutableStateOf(initialAddressExpanded) }
    var hoursExpanded by rememberSaveable(place.id) { mutableStateOf(initialHoursExpanded) }

    Column(modifier = modifier.fillMaxSize()) {
        StaticParkingSheetHandle()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = place.name,
                    style = RodiTheme.typography.headline1,
                    color = RodiTheme.colors.black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Spacer(Modifier.width(4.dp))
                Icon(
                    painter = painterResource(R.drawable.ic_bookmark_detail),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(2.dp))
                Text(
                    text = place.bookmarkCount.toString(),
                    style = RodiTheme.typography.body3Medium,
                    color = RodiTheme.colors.gray700,
                )
            }
            Spacer(Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .requiredSizeIn(minWidth = 48.dp, minHeight = 48.dp)
                    .clickable(onClick = onDismiss),
                contentAlignment = Alignment.Center,
            ) {
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
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, top = 7.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ExpandableTextRow(
                    title = place.address.toDistrictAddress(),
                    expanded = addressExpanded,
                    onClick = {
                        addressExpanded = !addressExpanded
                        if (addressExpanded) hoursExpanded = false
                    },
                )
                if (addressExpanded) {
                    AddressDetails(
                        roadAddress = parking.roadAddress?.withoutDistrictPrefix().orMissing(),
                        lotAddress = parking.lotAddress?.withoutDistrictPrefix().orMissing(),
                    )
                } else {
                    ParkingHoursRow(
                        openingSummary = parking.operatingSummary(),
                        expanded = hoursExpanded,
                        onClick = {
                            hoursExpanded = !hoursExpanded
                            if (hoursExpanded) addressExpanded = false
                        },
                    )
                    if (hoursExpanded) {
                        ParkingHoursDetails(parking)
                    }
                    InlineInfoRow("총 주차 면수", parking.capacity?.let { "${it}대" }.orMissing())
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                HorizontalDivider(color = RodiTheme.colors.gray100)
                Text("요금 안내", style = RodiTheme.typography.body3Medium, color = RodiTheme.colors.gray800)
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (parking.isFree) {
                        ParkingFeeRow("주차 요금", "무료")
                    } else {
                        ParkingFeeRows(parking.feeInfo)
                    }
                }
            }
        }

        HorizontalDivider(color = RodiTheme.colors.gray200)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BookmarkButton(place.isBookmarked, onBookmarkClick, !isBookmarkUpdating)
            RodiButton("연습하러 가기", onNavigate, Modifier.weight(1f))
        }
    }
}

@Composable
private fun StaticParkingSheetHandle() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(width = 60.dp, height = 4.dp)
                .clip(RoundedCornerShape(100.dp))
                .background(RodiTheme.colors.handleBar),
        )
    }
}

@Composable
private fun ExpandableTextRow(title: String, expanded: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = RodiTheme.typography.body3Medium,
            color = RodiTheme.colors.gray800,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.width(4.dp))
        ChevronIcon(expanded)
    }
}

@Composable
private fun ParkingHoursRow(
    openingSummary: String,
    expanded: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "주차",
            style = RodiTheme.typography.caption1Medium,
            color = RodiTheme.colors.gray600,
            modifier = Modifier
                .background(RodiTheme.colors.gray200, RoundedCornerShape(2.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp),
        )
        Spacer(Modifier.width(4.dp))
        Text("･", style = RodiTheme.typography.body3Medium, color = RodiTheme.colors.gray800)
        Spacer(Modifier.width(4.dp))
        Text(
            text = openingSummary,
            style = RodiTheme.typography.body3Medium,
            color = RodiTheme.colors.gray800,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.width(4.dp))
        ChevronIcon(expanded)
    }
}

@Composable
private fun ChevronIcon(expanded: Boolean) {
    Icon(
        painter = painterResource(R.drawable.ic_chevron_down),
        contentDescription = if (expanded) "접기" else "펼치기",
        tint = RodiTheme.colors.gray800,
        modifier = Modifier
            .size(14.dp)
            .graphicsLayer { rotationZ = if (expanded) 180f else 0f },
    )
}

@Composable
private fun InlineInfoRow(label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = RodiTheme.typography.body3Medium, color = RodiTheme.colors.gray800)
        Spacer(Modifier.width(4.dp))
        Text("･", style = RodiTheme.typography.body3Medium, color = RodiTheme.colors.gray800)
        Spacer(Modifier.width(4.dp))
        Text(value, style = RodiTheme.typography.body3Medium, color = RodiTheme.colors.gray800)
    }
}

@Composable
private fun AddressDetails(
    roadAddress: String,
    lotAddress: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(RodiTheme.colors.primary50, RoundedCornerShape(8.dp))
            .border(1.dp, RodiTheme.colors.primary200, RoundedCornerShape(8.dp))
            .padding(start = 10.dp, top = 10.dp, end = 10.dp, bottom = 11.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AddressDetailRow("도로명", roadAddress)
        AddressDetailRow("지번", lotAddress)
    }
}

@Composable
private fun AddressDetailRow(label: String, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = RodiTheme.typography.body3Medium, color = RodiTheme.colors.gray600)
        Text(value, style = RodiTheme.typography.body3Medium, color = RodiTheme.colors.gray800)
    }
}

@Composable
private fun ParkingHoursDetails(parking: ParkingPlaceDetail) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ParkingDetailRow("평일", parking.operatingHours?.weekday.toDisplayHours())
        ParkingDetailRow("토요일", parking.operatingHours?.saturday.toDisplayHours())
        ParkingDetailRow("일요일", parking.operatingHours?.holiday.toDisplayHours())
        ParkingDetailRow("공휴일", parking.operatingHours?.holiday.toDisplayHours())
    }
}

@Composable
private fun ParkingFeeRows(fee: ParkingFeeInfo?) {
    fee?.baseMinutes?.let { baseMinutes ->
        fee.baseFee?.let { baseFee ->
            ParkingFeeRow("기본요금", "${baseMinutes}분 ${baseFee.won()}")
        }
    }
    fee?.addUnitMinutes?.let { addUnitMinutes ->
        fee.addUnitFee?.let { addUnitFee ->
            ParkingFeeRow("추가요금", "${addUnitMinutes}분당 ${addUnitFee.won()}")
        }
    }
}

@Composable
private fun ParkingFeeRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = RodiTheme.typography.caption1Medium, color = RodiTheme.colors.gray800)
        Spacer(Modifier.width(8.dp))
        DottedDivider(Modifier.weight(1f))
        Spacer(Modifier.width(8.dp))
        Text(value, style = RodiTheme.typography.body3SemiBold, color = RodiTheme.colors.gray800)
    }
}

@Composable
private fun ParkingDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = RodiTheme.typography.caption1Medium, color = RodiTheme.colors.gray800)
        Spacer(Modifier.width(8.dp))
        DottedDivider(Modifier.weight(1f))
        if (value.isNotBlank()) {
            Spacer(Modifier.width(8.dp))
            Text(value, style = RodiTheme.typography.body3SemiBold, color = RodiTheme.colors.gray800)
        }
    }
}

@Composable
private fun DottedDivider(modifier: Modifier = Modifier) {
    val color = RodiTheme.colors.gray400.copy(alpha = 0.35f)
    Box(
        modifier = modifier
            .height(1.dp)
            .drawBehind {
                drawLine(
                    color = color,
                    start = Offset(0f, size.height / 2f),
                    end = Offset(size.width, size.height / 2f),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(
                        intervals = floatArrayOf(6.dp.toPx(), 6.dp.toPx()),
                    ),
                )
            },
    )
}

private fun String?.orMissing(): String = this?.takeIf(String::isNotBlank) ?: "해당항목없음"
private fun Int.won(): String = "${NumberFormat.getNumberInstance(Locale.KOREA).format(this)}원"

private fun ParkingPlaceDetail.operatingSummary(): String {
    val weekday = operatingHours?.weekday?.replace(" ", "").orEmpty()
    if (weekday.isBlank()) return "영업시간 정보 없음"
    if (weekday.startsWith("00:00") && (weekday.endsWith("23:59") || weekday.endsWith("24:00"))) {
        return "24시간 영업"
    }
    return "${weekday.substringBefore("-")}에 영업 시작"
}

private fun String?.toDisplayHours(): String {
    val value = this?.trim().orEmpty()
    return if (value.isBlank()) "해당항목없음" else value.replace("-", " - ")
}

private fun String.toDistrictAddress(): String {
    val districtTokens = trim().split(Regex("\\s+")).takeWhile {
        it.endsWith("시") || it.endsWith("도") || it.endsWith("군") || it.endsWith("구")
    }
    return districtTokens.joinToString(" ") { token ->
        token
            .removeSuffix("특별자치시")
            .removeSuffix("특별시")
            .removeSuffix("광역시")
            .ifBlank { token }
    }.ifBlank { this }
}

private fun String.withoutDistrictPrefix(): String {
    val tokens = trim().split(Regex("\\s+"))
    val localStart = tokens.indexOfFirst { token ->
        !(token.endsWith("시") || token.endsWith("도") || token.endsWith("군") || token.endsWith("구"))
    }
    return if (localStart < 0) this else tokens.drop(localStart).joinToString(" ")
}

@Preview(name = "Parking detail - paid", showBackground = true, widthDp = 375, heightDp = 400)
@Composable
private fun ParkingPaidPreview() {
    RodiTheme { ParkingDetailContent(HomePreviewData.parkingDetail, false, {}, {}, {}) }
}

@Preview(name = "Parking detail - address", showBackground = true, widthDp = 375, heightDp = 400)
@Composable
private fun ParkingAddressPreview() {
    RodiTheme {
        ParkingDetailContent(HomePreviewData.parkingDetail, false, {}, {}, {}, initialAddressExpanded = true)
    }
}

@Preview(name = "Parking detail - hours", showBackground = true, widthDp = 375, heightDp = 400)
@Composable
private fun ParkingHoursPreview() {
    RodiTheme {
        ParkingDetailContent(HomePreviewData.parkingDetail, false, {}, {}, {}, initialHoursExpanded = true)
    }
}

@Preview(name = "Parking detail - missing", showBackground = true, widthDp = 320, heightDp = 400, fontScale = 1.3f)
@Composable
private fun ParkingMissingPreview() {
    RodiTheme { ParkingDetailContent(HomePreviewData.parkingMissingFields, false, {}, {}, {}) }
}
