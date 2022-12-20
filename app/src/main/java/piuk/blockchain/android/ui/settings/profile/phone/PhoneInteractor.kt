package piuk.blockchain.android.ui.settings.profile.phone

import com.blockchain.api.services.WalletSettingsService
import com.blockchain.core.settings.SettingsDataManager
import com.blockchain.nabu.NabuUserSync
import com.blockchain.nabu.api.getuser.data.GetUserStore
import com.blockchain.preferences.AuthPrefs
import com.blockchain.utils.thenSingle
import info.blockchain.wallet.api.data.Settings
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single

class PhoneInteractor internal constructor(
    private val authPrefs: AuthPrefs,
    private val settingsDataManager: SettingsDataManager,
    private val nabuUserSync: NabuUserSync,
    private val getUserStore: GetUserStore
) {

    fun fetchProfileSettings(): Single<WalletSettingsService.UserInfoSettings> =
        settingsDataManager.fetchWalletSettings(
            guid = authPrefs.walletGuid,
            sharedKey = authPrefs.sharedKey
        )

    val cachedSettings: Single<Settings>
        get() = settingsDataManager.getSettings().first(Settings())

    fun savePhoneNumber(mobileWithPrefix: String): Single<Settings> =
        settingsDataManager.updateSms(mobileWithPrefix, forceJson = true)
            .doOnSuccess {
                getUserStore.invalidate()
            }
            .flatMap { settings ->
                syncPhoneNumberWithNabu().thenSingle {
                    Single.just(settings)
                }
            }

    fun verifyPhoneNumber(code: String): Completable {
        return settingsDataManager.verifySms(code)
            .flatMapCompletable { syncPhoneNumberWithNabu() }
    }

    /*
    Eventually "resend-sms" without having to save the phone number in order to get a SMS,
    keep an eye: https://blockchain.atlassian.net/browse/WS-170
*/
    fun resendCodeSMS(mobileWithPrefix: String): Single<Settings> =
        settingsDataManager.updateSms(mobileWithPrefix, forceJson = true)
            .flatMap { settings ->
                syncPhoneNumberWithNabu().thenSingle {
                    Single.just(settings)
                }
            }

    private fun syncPhoneNumberWithNabu(): Completable {
        return nabuUserSync.syncUser()
    }
}
