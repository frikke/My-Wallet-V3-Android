package piuk.blockchain.android.ui.kyc.address

import com.blockchain.nabu.CurrentTier
import com.blockchain.nabu.datamanagers.NabuDataUserProvider
import io.reactivex.rxjava3.core.Single

internal class CurrentTierAdapter(
    private val nabuDataUserProvider: NabuDataUserProvider
) : CurrentTier {

    override fun usersCurrentTier(): Single<Int> =
        nabuDataUserProvider.getUser()
            .map { it.tiers?.current ?: 0 }
}
