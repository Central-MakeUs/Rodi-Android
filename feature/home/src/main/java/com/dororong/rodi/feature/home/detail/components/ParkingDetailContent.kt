package com.dororong.rodi.feature.home.detail.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dororong.rodi.core.domain.model.place.ParkingFeeInfo
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = place.name,
                style = RodiTheme.typography.headline1,
                color = RodiTheme.colors.black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
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
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ExpandableRow(
                title = place.address,
                expanded = addressExpanded,
                onClick = {
                    addressExpanded = !addressExpanded
                    if (addressExpanded) hoursExpanded = false
                },
            )
            if (addressExpanded) {
                InfoRow("도로명", parking.roadAddress.orMissing())
                InfoRow("지번", parking.lotAddress.orMissing())
            }
            ExpandableRow(
                title = parking.parkingType.orMissing(),
                expanded = hoursExpanded,
                onClick = {
                    hoursExpanded = !hoursExpanded
                    if (hoursExpanded) addressExpanded = false
                },
            )
            if (hoursExpanded) {
                InfoRow("평일", parking.operatingHours?.weekday.orMissing())
                InfoRow("토요일", parking.operatingHours?.saturday.orMissing())
                InfoRow("일요일·공휴일", parking.operatingHours?.holiday.orMissing())
            }
            InfoRow("총 주차 면수", parking.capacity?.let { "${it}대" }.orMissing())
            HorizontalDivider(color = RodiTheme.colors.primary100)
            Text("요금 안내", style = RodiTheme.typography.body3Medium, color = RodiTheme.colors.gray800)
            if (parking.isFree) {
                InfoRow("주차 요금", "무료")
            } else {
                ParkingFeeRows(parking.feeInfo)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BookmarkButton(place.isBookmarked, onBookmarkClick, !isBookmarkUpdating)
            RodiButton("연습하러 가기", onNavigate, Modifier.weight(1f))
        }
    }
}

@Composable
private fun ExpandableRow(title: String, expanded: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = RodiTheme.typography.body3Medium,
            color = RodiTheme.colors.gray800,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(4.dp))
        Icon(
            painter = painterResource(R.drawable.ic_chevron_down),
            contentDescription = if (expanded) "접기" else "펼치기",
            tint = RodiTheme.colors.gray800,
            modifier = Modifier
                .size(16.dp)
                .graphicsLayer { rotationZ = if (expanded) 180f else 0f },
        )
    }
}

@Composable
private fun ParkingFeeRows(fee: ParkingFeeInfo?) {
    val baseMinutes = fee?.baseMinutes
    val baseFee = fee?.baseFee
    val addUnitMinutes = fee?.addUnitMinutes
    val addUnitFee = fee?.addUnitFee
    InfoRow("초기무료", "해당항목없음")
    InfoRow(
        "기본요금",
        if (baseMinutes != null && baseFee != null) {
            "${baseMinutes}분 ${baseFee.won()}"
        } else "해당항목없음",
    )
    InfoRow(
        "추가요금",
        if (addUnitMinutes != null && addUnitFee != null) {
            "${addUnitMinutes}분당 ${addUnitFee.won()}"
        } else "해당항목없음",
    )
    InfoRow("할증기준시간", "해당항목없음")
    InfoRow("일일권", fee?.dayTicketFee?.won().orMissing())
    InfoRow("월정기", fee?.monthlyFee?.won().orMissing())
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(label, style = RodiTheme.typography.caption1Medium, color = RodiTheme.colors.gray700)
        Spacer(Modifier.weight(1f))
        Text(value, style = RodiTheme.typography.body3SemiBold, color = RodiTheme.colors.gray800)
    }
}

private fun String?.orMissing(): String = this?.takeIf(String::isNotBlank) ?: "해당항목없음"
private fun Int.won(): String = "${NumberFormat.getNumberInstance(Locale.KOREA).format(this)}원"

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
