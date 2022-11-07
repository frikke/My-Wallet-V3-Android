package piuk.blockchain.android.ui.transfer.receive

import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.SingleAccount
import com.blockchain.commonarch.presentation.mvi.MviIntent
import info.blockchain.balance.AssetInfo
import io.reactivex.rxjava3.core.Single

sealed class ReceiveIntent : MviIntent<ReceiveState> {

    class GetAvailableAssets(val startForTicker: String?) : ReceiveIntent() {
        override fun reduce(oldState: ReceiveState): ReceiveState = oldState
    }

    data class UpdateAssets(
        val assets: List<ReceiveItem>,
        private val loadAccountsForAsset: (AssetInfo) -> Single<List<CryptoAccount>>,
    ) : ReceiveIntent() {
        override fun reduce(oldState: ReceiveState): ReceiveState =
            oldState.copy(
                assets = assets,
                loadAccountsForAsset = loadAccountsForAsset
            )
    }

    data class UpdateAccounts(
        val accounts: List<SingleAccount>,
    ) : ReceiveIntent() {
        override fun reduce(oldState: ReceiveState): ReceiveState =
            oldState.copy(
                allReceivableAccountsSource = accounts
            )
    }

    data class FilterAssets(private val searchString: String) : ReceiveIntent() {
        override fun reduce(oldState: ReceiveState): ReceiveState =
            oldState.copy(
                input = searchString
            )
    }

    class GetStartingAccountForAsset(val cryptoTicker: String, val accounts: List<SingleAccount>) : ReceiveIntent() {
        override fun reduce(oldState: ReceiveState): ReceiveState = oldState
    }

    class UpdateReceiveForAsset(val account: CryptoAccount) : ReceiveIntent() {
        override fun reduce(oldState: ReceiveState): ReceiveState =
            oldState.copy(showReceiveForAccount = account)
    }

    object ResetReceiveForAccount : ReceiveIntent() {
        override fun reduce(oldState: ReceiveState): ReceiveState = oldState.copy(showReceiveForAccount = null)
    }
}
