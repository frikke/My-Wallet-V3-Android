package piuk.blockchain.android.ui.transfer.receive

import com.blockchain.coincore.CryptoAccount
import info.blockchain.balance.AssetInfo
import io.reactivex.rxjava3.core.Single
import piuk.blockchain.android.ui.base.mvi.MviState

data class ReceiveState(
    val assets: List<AssetInfo> = emptyList(),
    val loadAccountsForAsset: (AssetInfo) -> Single<List<CryptoAccount>> = { Single.just(emptyList()) },
    val filterBy: String = ""
) : MviState