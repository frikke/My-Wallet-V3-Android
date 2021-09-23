package piuk.blockchain.android.ui.kyc.autocomplete

import android.content.Context
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.net.PlacesClient
import piuk.blockchain.android.BuildConfig

class PlacesClientProvider(val context: Context) {
    fun getClient(): PlacesClient {
        if (!Places.isInitialized()) {
            Places.initialize(context, BuildConfig.GOOGLE_PLACES_KEY)
        }
        return Places.createClient(context)
    }
}