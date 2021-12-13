package piuk.blockchain.android.ui.settings.v2

import info.blockchain.wallet.api.data.Settings
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.Singles
import piuk.blockchain.androidcore.data.settings.Email
import piuk.blockchain.androidcore.data.settings.EmailSyncUpdater
import piuk.blockchain.androidcore.data.settings.SettingsDataManager

class ProfileInteractor internal constructor(
    private val emailUpdater: EmailSyncUpdater,
    private val settingsDataManager: SettingsDataManager
) {

    // TODO make call to the fields that have changed
    fun saveProfile(email: String, phone: String): Single<Pair<Email, Settings>> =
        Singles.zip(
            emailUpdater.updateEmailAndSync(email),
            settingsDataManager.updateSms(phone).singleOrError()
        )
}
