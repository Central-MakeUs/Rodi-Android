package com.dororong.rodi.core.data.source.remote.model.kakao

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class KakaoKeywordSearchResponse(
    val documents: List<KakaoKeywordDocument> = emptyList(),
)

@Serializable
data class KakaoKeywordDocument(
    val id: String,
    @SerialName("place_name") val placeName: String,
    @SerialName("address_name") val addressName: String = "",
    @SerialName("road_address_name") val roadAddressName: String = "",
    val x: String,
    val y: String,
)

@Serializable
data class KakaoAddressSearchResponse(
    val documents: List<KakaoAddressDocument> = emptyList(),
)

@Serializable
data class KakaoAddressDocument(
    @SerialName("address_name") val addressName: String,
    @SerialName("address_type") val addressType: String = "",
    @SerialName("road_address") val roadAddress: KakaoRoadAddress? = null,
    val address: KakaoJibunAddress? = null,
    val x: String,
    val y: String,
)

@Serializable
data class KakaoRoadAddress(
    @SerialName("address_name") val addressName: String = "",
)

@Serializable
data class KakaoJibunAddress(
    @SerialName("address_name") val addressName: String = "",
)

@Serializable
data class KakaoCoordinateAddressResponse(
    val documents: List<KakaoCoordinateAddressDocument> = emptyList(),
)

@Serializable
data class KakaoCoordinateAddressDocument(
    val address: KakaoJibunAddress? = null,
    @SerialName("road_address") val roadAddress: KakaoRoadAddress? = null,
)
