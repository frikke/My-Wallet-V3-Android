package piuk.blockchain.android.ui.kyc.address

import com.blockchain.nabu.CurrentTier
import com.blockchain.nabu.api.getuser.domain.UserService
import io.reactivex.rxjava3.core.Single

internal class CurrentTierAdapter(
    private val userService: UserService
) : CurrentTier {

    override fun usersCurrentTier(): Single<Int> =
        userService.getUser()
            .map { it.tiers?.current ?: 0 }
}
