package piuk.blockchain.android.ui.settings.v2

import com.blockchain.nabu.BasicProfileInfo
import com.blockchain.nabu.Feature
import com.blockchain.nabu.UserIdentity
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.Singles

class SettingsInteractor internal constructor(
    private val userIdentity: UserIdentity
) {

    fun checkContactSupportEligibility(): Single<Pair<Boolean, BasicProfileInfo>> =
        Singles.zip(
            userIdentity.isEligibleFor(Feature.SimpleBuy),
            userIdentity.getBasicProfileInformation()
        )
}
