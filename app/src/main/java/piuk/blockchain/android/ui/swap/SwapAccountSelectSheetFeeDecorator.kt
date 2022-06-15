package piuk.blockchain.android.ui.swap

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.TradingAccount
import com.blockchain.walletmode.WalletMode
import io.reactivex.rxjava3.core.Maybe
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.customviews.account.CellDecorator

class SwapAccountSelectSheetFeeDecorator(
    private val account: BlockchainAccount,
    private val walletMode: WalletMode
) : CellDecorator {

    override fun view(context: Context): Maybe<View> {
        // in trading mode - no need to show the fee decorator
        return if (account is TradingAccount && walletMode != WalletMode.CUSTODIAL_ONLY) {
            Maybe.just(tradingAccountBadgesView(context))
        } else
            Maybe.empty()
    }

    private fun tradingAccountBadgesView(context: Context) =
        LayoutInflater.from(context).inflate(
            R.layout.trading_account_badges_layout,
            null,
            false
        )
}
