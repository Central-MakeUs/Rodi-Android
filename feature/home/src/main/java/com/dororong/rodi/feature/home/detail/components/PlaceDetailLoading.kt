package com.dororong.rodi.feature.home.detail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.Alignment
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dororong.rodi.core.ui.components.RodiSkeleton
import com.dororong.rodi.core.ui.theme.RodiTheme
import com.dororong.rodi.feature.home.components.SheetHandle

@Composable
fun PlaceDetailLoading(
    modifier: Modifier = Modifier,
    dragHandleModifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(240.dp),
    ) {
        SheetHandle(modifier = Modifier.height(24.dp).then(dragHandleModifier))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RodiSkeleton(modifier = Modifier.width(176.dp).height(24.dp))
                Spacer(Modifier.width(4.dp))
                RodiSkeleton(modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(2.dp))
                RodiSkeleton(modifier = Modifier.width(20.dp).height(14.dp))
                Spacer(Modifier.weight(1f))
                RodiSkeleton(modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(10.dp))
            RodiSkeleton(modifier = Modifier.width(76.dp).height(18.dp))
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp)) {
                RodiSkeleton(modifier = Modifier.width(42.dp).height(20.dp))
                RodiSkeleton(modifier = Modifier.width(48.dp).height(20.dp))
            }
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(37.dp)
                    .background(RodiTheme.colors.gray100, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                RodiSkeleton(modifier = Modifier.fillMaxWidth(0.72f).height(14.dp))
            }
            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = RodiTheme.colors.gray200)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(5.dp)) {
                RodiSkeleton(modifier = Modifier.size(44.dp))
                RodiSkeleton(modifier = Modifier.weight(1f).height(44.dp))
            }
        }
    }
}

@Preview(name = "Detail loading - compact", showBackground = true, widthDp = 375, heightDp = 240)
@Composable
private fun DetailLoadingCompactPreview() {
    RodiTheme { PlaceDetailLoading() }
}

@Preview(name = "Detail loading - small", showBackground = true, widthDp = 320, heightDp = 240, fontScale = 1.3f)
@Composable
private fun DetailLoadingSmallPreview() {
    RodiTheme { PlaceDetailLoading() }
}
