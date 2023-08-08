package piuk.blockchain.android.ui.brokerage.sell

import android.content.Context
import android.view.View
import com.blockchain.coincore.BlockchainAccount
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single
import piuk.blockchain.android.ui.customviews.account.CellDecorator

class SellCellDecorator(private val account: BlockchainAccount) : CellDecorator {
    override fun view(context: Context): Maybe<View> = Maybe.empty()

    override fun isEnabled(): Single<Boolean> = account.balanceRx().firstOrError().map { it.total.isPositive }
}
