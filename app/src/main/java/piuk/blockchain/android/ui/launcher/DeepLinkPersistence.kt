package piuk.blockchain.android.ui.launcher

import piuk.blockchain.androidcore.utils.SessionPrefs
import timber.log.Timber

class DeepLinkPersistence(private val sessionPrefs: SessionPrefs) {

    fun pushDeepLink(data: String) {
        Timber.d("DeepLink: Saving uri: $data")
        sessionPrefs.deeplinkUri = data
    }

    fun popDataFromSharedPrefs(): String {
        val data = sessionPrefs.deeplinkUri
        Timber.d("DeepLink: Read uri: $data")
        sessionPrefs.clearDeeplinkUri()
        Timber.d("DeepLink: Cleared uri: $data")
        return data
    }
}
