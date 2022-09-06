package piuk.blockchain.android.ui.kyc.autocomplete

import android.content.Context
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.net.PlacesClient

class PlacesClientProvider(val context: Context, private val apiKey: String) {
    fun getClient(): PlacesClient {
        if (!Places.isInitialized()) {
            Places.initialize(context, apiKey)
        }
        return Places.createClient(context)
    }
}
