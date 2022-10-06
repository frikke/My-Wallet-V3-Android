package com.blockchain.analytics
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class NabuAnalyticsEvent(
    val name: String,
    val type: String,
    val originalTimestamp: String,
    val properties: Map<String, JsonElement>,
)

@Serializable
class AnalyticsContext(
    val device: DeviceInfo,
    val locale: String,
    val screen: ScreenInfo,
    val timezone: String,
    val traits: Map<String, String> = emptyMap()
)

@Serializable
class DeviceInfo(val manufacturer: String?, val model: String, val name: String)

@Serializable
class ScreenInfo(val density: Float, val height: Int, val width: Int)
