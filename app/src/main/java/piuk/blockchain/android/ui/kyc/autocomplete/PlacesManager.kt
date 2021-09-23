package piuk.blockchain.android.ui.kyc.autocomplete

import android.content.Context
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.net.PlacesClient
import piuk.blockchain.android.BuildConfig

class PlacesManager(applicationContext: Context) {
    val client: PlacesClient
    val token: AutocompleteSessionToken

    init {
        Places.initialize(applicationContext, BuildConfig.GOOGLE_PLACES_KEY)
        client = Places.createClient(applicationContext)
        token = AutocompleteSessionToken.newInstance()
    }
}