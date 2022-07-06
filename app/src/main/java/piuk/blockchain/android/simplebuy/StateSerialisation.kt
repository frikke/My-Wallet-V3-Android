package piuk.blockchain.android.simplebuy

import com.blockchain.preferences.SimpleBuyPrefs
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
    private val json: Json,
) : SimpleBuyPrefsSerializer {

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
