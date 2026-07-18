package com.dororong.rodi.feature.home.map

import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.label.LabelLayer
import com.kakao.vectormap.label.LabelLayerOptions

private const val BROWSE_LAYER_ID = "rodi-browse-layer"
private const val DETAIL_LAYER_ID = "rodi-detail-layer"
private const val CURRENT_LOCATION_LAYER_ID = "rodi-current-location-layer"

internal fun KakaoMap.browseLabelLayer(): LabelLayer? = labelManager?.let { manager ->
    manager.getLayer(BROWSE_LAYER_ID) ?: manager.addLayer(
        LabelLayerOptions.from(BROWSE_LAYER_ID)
            .setZOrder(5_100)
            .setClickable(true),
    )
}

internal fun KakaoMap.detailLabelLayer(): LabelLayer? = labelManager?.let { manager ->
    manager.getLayer(DETAIL_LAYER_ID) ?: manager.addLayer(
        LabelLayerOptions.from(DETAIL_LAYER_ID)
            .setZOrder(5_200)
            .setClickable(false),
    )
}

internal fun KakaoMap.currentLocationLabelLayer(): LabelLayer? = labelManager?.let { manager ->
    manager.getLayer(CURRENT_LOCATION_LAYER_ID) ?: manager.addLayer(
        LabelLayerOptions.from(CURRENT_LOCATION_LAYER_ID)
            .setZOrder(5_300)
            .setClickable(false),
    )
}

fun KakaoMap.clearBrowseLabels() {
    labelManager?.getLayer(BROWSE_LAYER_ID)?.removeAll()
}

fun KakaoMap.clearCurrentLocationMarker() {
    labelManager?.getLayer(CURRENT_LOCATION_LAYER_ID)?.removeAll()
}
