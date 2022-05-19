package exchange

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single

interface ExchangeLinking {

    val state: Observable<ExchangeLinkingState>

    // Helper method, for all the MVP clients:
    fun isExchangeLinked(): Single<Boolean>
}

data class ExchangeLinkingState(
    val isLinked: Boolean = false,
    val emailVerified: Boolean = false,
    val email: String? = null
)
