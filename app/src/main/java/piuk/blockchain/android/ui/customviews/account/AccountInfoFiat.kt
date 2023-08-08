package piuk.blockchain.android.ui.customviews.account

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import com.blockchain.coincore.FiatAccount
import com.blockchain.componentlib.viewextensions.gone
import com.blockchain.componentlib.viewextensions.visible
import com.blockchain.componentlib.viewextensions.visibleIf
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.koin.scopedInject
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.presentation.koin.scopedInject
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.kotlin.subscribeBy
import org.koin.core.component.KoinComponent
import piuk.blockchain.android.databinding.ViewAccountFiatOverviewBinding
import piuk.blockchain.android.ui.transactionflow.analytics.TxFlowAnalytics
import piuk.blockchain.android.ui.transactionflow.engine.TransactionModel
import piuk.blockchain.android.ui.transactionflow.engine.TransactionState
import piuk.blockchain.android.ui.transactionflow.flow.customisations.EnterAmountCustomisations
import piuk.blockchain.android.ui.transactionflow.plugin.EnterAmountWidget

class AccountInfoFiat @JvmOverloads constructor(
    ctx: Context,
    attr: AttributeSet? = null,
    defStyle: Int = 0
) : ConstraintLayout(ctx, attr, defStyle), KoinComponent, EnterAmountWidget {

    private val exchangeRates: ExchangeRatesDataManager by scopedInject()
    private val currencyPrefs: CurrencyPrefs by scopedInject()
    private val compositeDisposable = CompositeDisposable()

    val binding: ViewAccountFiatOverviewBinding =
        ViewAccountFiatOverviewBinding.inflate(LayoutInflater.from(context), this, true)

    fun updateAccount(account: FiatAccount, cellDecorator: CellDecorator, onAccountClicked: (FiatAccount) -> Unit) {
        compositeDisposable.clear()
        updateView(account, cellDecorator, onAccountClicked)
    }

    private fun updateView(
        account: FiatAccount,
        cellDecorator: CellDecorator,
        onAccountClicked: (FiatAccount) -> Unit
    ) {
        with(binding) {
            contentDescription = "$ACCOUNT_INFO_FIAT_VIEW_ID${account.currency.networkTicker}_${account.label}"
            val userFiat = currencyPrefs.selectedFiatCurrency

            currencyName.text = account.currency.name
            icon.setIcon(account.currency)
            assetSubtitle.text = account.currency.networkTicker

            compositeDisposable += account.balanceRx().firstOrError().map { it.total }
                .flatMap { balance ->
                    exchangeRates.exchangeRateToUserFiat(account.currency).firstOrError().map { exchangeRate ->
                        balance to exchangeRate.convert(balance)
                    }
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy { (balanceInAccountCurrency, balanceInWalletCurrency) ->
                    if (userFiat == account.currency) {
                        walletBalanceExchangeFiat.gone()
                        walletBalanceFiat.text = balanceInAccountCurrency.toStringWithSymbol()
                    } else {
                        walletBalanceExchangeFiat.visible()
                        walletBalanceFiat.text = balanceInWalletCurrency.toStringWithSymbol()
                        walletBalanceExchangeFiat.text = balanceInAccountCurrency.toStringWithSymbol()
                    }
                }

            setOnClickListener { }
            compositeDisposable += cellDecorator.isEnabled()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { isEnabled ->
                    if (isEnabled) {
                        setOnClickListener { onAccountClicked(account) }
                        container.alpha = 1f
                    } else {
                        container.alpha = .6f
                        setOnClickListener { }
                    }
                }
        }
    }

    fun dispose() {
        compositeDisposable.clear()
    }

    override fun initControl(
        model: TransactionModel,
        customiser: EnterAmountCustomisations,
        analytics: TxFlowAnalytics
    ) {
        // No need to initialise
    }

    override fun update(state: TransactionState) {
        updateAccount(state.sendingAccount as FiatAccount, DefaultCellDecorator()) { }
    }

    override fun setVisible(isVisible: Boolean) {
        binding.root.visibleIf { isVisible }
    }

    companion object {
        private const val ACCOUNT_INFO_FIAT_VIEW_ID = "FiatAccountView_"
    }
}
