package piuk.blockchain.android.ui.transfer.receive

import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.SingleAccount
import com.blockchain.commonarch.presentation.mvi.MviState
import com.blockchain.core.price.Prices24HrWithDelta
import info.blockchain.balance.AssetInfo
import io.reactivex.rxjava3.core.Single

data class ReceiveItem(
    val assetInfo: AssetInfo,
    val priceWithDelta: Prices24HrWithDelta? = null
)

data class ReceiveState(
    val assets: List<ReceiveItem> = emptyList(),
    val loadAccountsForAsset: (AssetInfo) -> Single<List<CryptoAccount>> = { Single.just(emptyList()) },
    val allReceivableAccountsSource: List<SingleAccount> = emptyList(),
    val input: String = "",
    val showReceiveForAccount: CryptoAccount? = null
) : MviState
