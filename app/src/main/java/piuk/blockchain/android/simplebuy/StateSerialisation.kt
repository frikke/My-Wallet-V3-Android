package piuk.blockchain.android.simplebuy

import com.blockchain.preferences.SimpleBuyPrefs
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.AssetInfo
import java.lang.reflect.Type
import java.time.ZonedDateTime
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

interface SimpleBuyPrefsSerializer {
    fun fetch(): SimpleBuyState?
    fun update(newState: SimpleBuyState)
    fun clear()
}

internal class SimpleBuyPrefsSerializerImpl(
    private val prefs: SimpleBuyPrefs,
    assetCatalogue: AssetCatalogue,
    private val json: Json,
) : SimpleBuyPrefsSerializer {

    private val gson = GsonBuilder()
        .registerTypeAdapter(AssetInfo::class.java, AssetTickerSerializer())
        .registerTypeAdapter(AssetInfo::class.java, AssetTickerDeserializer(assetCatalogue))
        .registerTypeAdapter(
            ZonedDateTime::class.java,
            object : TypeAdapter<ZonedDateTime>() {
                override fun write(out: JsonWriter?, value: ZonedDateTime?) {
                    out?.value(value.toString())
                }

                override fun read(`in`: JsonReader): ZonedDateTime {
                    return ZonedDateTime.parse(`in`.nextString())
                }
            }
        )
        .enableComplexMapKeySerialization()
        .create()

    override fun fetch(): SimpleBuyState? = prefs.simpleBuyState()?.decode()

    override fun update(newState: SimpleBuyState) = prefs.updateSimpleBuyState(newState.encode())

    override fun clear() = prefs.clearBuyState()

    private fun SimpleBuyState.encode(): String = json.encodeToString(this)

    private fun String.decode(): SimpleBuyState? {
        return try {
            json.decodeFromString<SimpleBuyState>(this)
        } catch (t: Throwable) {
            prefs.clearBuyState()
            null
        }
    }
}

private class AssetTickerSerializer : JsonSerializer<AssetInfo> {
    override fun serialize(
        src: AssetInfo,
        typeOfSrc: Type,
        context: JsonSerializationContext,
    ): JsonElement = JsonPrimitive(src.networkTicker)
}

private class AssetTickerDeserializer(
    val assetCatalogue: AssetCatalogue,
) : JsonDeserializer<AssetInfo> {
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext,
    ): AssetInfo = assetCatalogue.assetInfoFromNetworkTicker(
        json.asString
    ) ?: throw JsonParseException("Unknown Asset ticker")
}
