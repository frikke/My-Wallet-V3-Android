package piuk.blockchain.android.ui.transfer.receive

import com.blockchain.coincore.CryptoAccount
import com.blockchain.commonarch.presentation.mvi.MviIntent
import info.blockchain.balance.AssetInfo
import io.reactivex.rxjava3.core.Single

sealed class ReceiveIntent : MviIntent<ReceiveState> {

    object GetAvailableAssets : ReceiveIntent() {
        override fun reduce(oldState: ReceiveState): ReceiveState = oldState.copy()
    }

    data class UpdateAssets(
        private val assets: List<AssetInfo>,
        private val loadAccountsForAsset: (AssetInfo) -> Single<List<CryptoAccount>>
    ) : ReceiveIntent() {
        override fun reduce(oldState: ReceiveState): ReceiveState =
            oldState.copy(
                assets = assets,
                loadAccountsForAsset = loadAccountsForAsset
            )
    }

    data class FilterAssets(private val searchString: String) : ReceiveIntent() {
        override fun reduce(oldState: ReceiveState): ReceiveState =
            oldState.copy(
                filterBy = searchString
            )
    }
}
