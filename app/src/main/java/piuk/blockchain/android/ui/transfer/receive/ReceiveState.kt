package piuk.blockchain.android.ui.transfer.receive

import com.blockchain.coincore.CryptoAccount
import com.blockchain.commonarch.presentation.mvi.MviState
import info.blockchain.balance.AssetInfo
import io.reactivex.rxjava3.core.Single

data class ReceiveState(
    val assets: List<AssetInfo> = emptyList(),
    val loadAccountsForAsset: (AssetInfo) -> Single<List<CryptoAccount>> = { Single.just(emptyList()) },
    val filterBy: String = ""
) : MviState
