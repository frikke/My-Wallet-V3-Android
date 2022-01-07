package thepit

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single

interface PitLinking {

    val state: Observable<PitLinkingState>

    fun sendWalletAddressToThePit()

    // Helper method, for all the MVP clients:
    fun isPitLinked(): Single<Boolean>
}

data class PitLinkingState(
    val isLinked: Boolean = false,
    val emailVerified: Boolean = false,
    val email: String? = null
)
