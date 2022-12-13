package piuk.blockchain.android.walletmode

import com.blockchain.analytics.TraitsService
import com.blockchain.walletmode.WalletMode
import com.blockchain.walletmode.WalletModeService
import kotlinx.coroutines.rx3.await

class WalletModeTraitsRepository(private val walletModeService: Lazy<WalletModeService>) : TraitsService {
    override suspend fun traits(): Map<String, String> {
        val walletMode = walletModeService.value.walletModeSingle.await()
        return mapOf(
            "is_superapp_mvp" to (walletMode != WalletMode.UNIVERSAL).toString(),
            "app_mode" to walletMode.toTraitsString(),
        )
    }
}

private fun WalletMode.toTraitsString(): String {
    return when (this) {
        WalletMode.UNIVERSAL -> "UNIVERSAL"
        WalletMode.CUSTODIAL_ONLY -> "TRADING"
        WalletMode.NON_CUSTODIAL_ONLY -> "PKW"
    }
}
