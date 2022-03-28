package piuk.blockchain.android.simplebuy

import com.blockchain.preferences.SimpleBuyPrefs
import com.blockchain.remoteconfig.FeatureFlag
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.AssetInfo
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import piuk.blockchain.android.ui.transactionflow.engine.TransactionErrorState
import java.lang.reflect.Type

interface SimpleBuyPrefsSerializer {
    fun fetch(): SimpleBuyState?
    fun update(newState: SimpleBuyState)
    fun clear()
}

internal class SimpleBuyPrefsSerializerImpl(
    private val prefs: SimpleBuyPrefs,
    assetCatalogue: AssetCatalogue,
    private val json: Json,
    private val replaceGsonKtxFF: FeatureFlag,
) : SimpleBuyPrefsSerializer {

    private val gson = GsonBuilder()
        .registerTypeAdapter(AssetInfo::class.java, AssetTickerSerializer())
        .registerTypeAdapter(AssetInfo::class.java, AssetTickerDeserializer(assetCatalogue))
        .create()

    override fun fetch(): SimpleBuyState? = prefs.simpleBuyState()?.decode()

    override fun update(newState: SimpleBuyState) {
        prefs.updateSimpleBuyState(newState.encode())
    }

    override fun clear() {
        prefs.clearBuyState()
    }

    private fun SimpleBuyState.encode(): String {
        return if (replaceGsonKtxFF.isEnabled) {
            json.encodeToString(this)
        } else {
            gson.toJson(this)
        }
    }

    private fun String.decode(): SimpleBuyState? {
        return try {
            if (replaceGsonKtxFF.isEnabled) {
                json.decodeFromString<SimpleBuyState>(this)
            } else {
                gson.fromJson(this, SimpleBuyState::class.java)
                    // When we de-serialise via gson the object fields get overwritten - including transients -
                    // so in order to have a valid object, we should re-init any non-nullable transient fields here
                    // or copy() operations will fail
                    .copy(
                        paymentOptions = PaymentOptions(),
                        isLoading = false,
                        shouldShowUnlockHigherFunds = false,
                        paymentPending = false,
                        confirmationActionRequested = false,
                        errorState = TransactionErrorState.NONE
                    )
            }
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
        context: JsonSerializationContext
    ): JsonElement = JsonPrimitive(src.networkTicker)
}

private class AssetTickerDeserializer(
    val assetCatalogue: AssetCatalogue
) : JsonDeserializer<AssetInfo> {
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): AssetInfo = assetCatalogue.assetInfoFromNetworkTicker(
        json.asString
    ) ?: throw JsonParseException("Unknown Asset ticker")
}


