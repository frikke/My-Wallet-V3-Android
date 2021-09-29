package piuk.blockchain.android.ui.kyc.autocomplete

import android.content.Context
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.net.PlacesClient
import piuk.blockchain.android.R

class PlacesClientProvider(val context: Context) {
    fun getClient(): PlacesClient {
        if (!Places.isInitialized()) {
            Places.initialize(context, context.getString(R.string.google_api_key))
        }
        return Places.createClient(context)
    }
}