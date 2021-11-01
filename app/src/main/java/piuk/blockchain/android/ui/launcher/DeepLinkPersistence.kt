package piuk.blockchain.android.ui.launcher

import piuk.blockchain.androidcore.utils.PersistentPrefs
import timber.log.Timber

private const val KEY_DEEP_LINK_URI = "deeplink_uri"

class DeepLinkPersistence(private val prefs: PersistentPrefs) {

    fun pushDeepLink(data: String) {
        Timber.d("DeepLink: Saving uri: $data")
        prefs.setValue(KEY_DEEP_LINK_URI, data)
    }

    fun popDataFromSharedPrefs(): String? {
        val data = prefs.getValue(KEY_DEEP_LINK_URI)
        Timber.d("DeepLink: Read uri: $data")
        prefs.removeValue(KEY_DEEP_LINK_URI)
        return data
    }
}
