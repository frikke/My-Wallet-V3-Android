package piuk.blockchain.androidcore.data.settings.datastore

import com.blockchain.utils.Optional
import info.blockchain.wallet.api.data.Settings
import io.reactivex.rxjava3.core.Observable

interface SettingsStore {

    fun getSettings(): Observable<Optional<Settings>>
}
