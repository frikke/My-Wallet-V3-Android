package piuk.blockchain.android.ui.settings.v2

import com.blockchain.core.Database
import com.blockchain.nabu.BasicProfileInfo
import com.blockchain.nabu.Tier
import com.blockchain.nabu.UserIdentity
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.Singles
import piuk.blockchain.android.ui.home.CredentialsWiper

class SettingsInteractor internal constructor(
    private val userIdentity: UserIdentity,
    private val database: Database,
    private val credentialsWiper: CredentialsWiper
) {

    fun getSupportEligibilityAndBasicInfo(): Single<Pair<Tier, BasicProfileInfo>> =
        Singles.zip(
            userIdentity.getHighestApprovedKycTier(),
            userIdentity.getBasicProfileInformation()
        )

    fun unpairWallet(): Completable =
        Completable.fromAction {
            credentialsWiper.wipe()
            database.historicRateQueries.clear()
        }
}
