package piuk.blockchain.android.ui.customviews.account

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import com.blockchain.coincore.Coincore
import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.CryptoAsset
import com.blockchain.coincore.InterestAccount
import com.blockchain.coincore.NullCryptoAccount
import com.blockchain.coincore.toUserFiat
import com.blockchain.componentlib.viewextensions.visible
import com.blockchain.componentlib.viewextensions.visibleIf
import com.blockchain.core.price.ExchangeRates
import com.blockchain.koin.scopedInject
import info.blockchain.balance.Money
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.kotlin.subscribeBy
import org.koin.core.component.KoinComponent
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.ViewAccountCryptoOverviewBinding
import piuk.blockchain.android.ui.transactionflow.analytics.TxFlowAnalytics
import piuk.blockchain.android.ui.transactionflow.engine.TransactionModel
import piuk.blockchain.android.ui.transactionflow.engine.TransactionState
import piuk.blockchain.android.ui.transactionflow.flow.customisations.EnterAmountCustomisations
import piuk.blockchain.android.ui.transactionflow.plugin.EnterAmountWidget
import timber.log.Timber

class AccountInfoCrypto @JvmOverloads constructor(
    ctx: Context,
    attr: AttributeSet? = null,
    defStyle: Int = 0
) : ConstraintLayout(ctx, attr, defStyle), KoinComponent, EnterAmountWidget {

    private val exchangeRates: ExchangeRates by scopedInject()
    private val coincore: Coincore by scopedInject()
    private val compositeDisposable = CompositeDisposable()
    private var accountBalance: Money? = null
    private var isEnabled: Boolean? = null
    private var interestRate: Double? = null
    private var displayedAccount: CryptoAccount = NullCryptoAccount()

    val binding: ViewAccountCryptoOverviewBinding =
        ViewAccountCryptoOverviewBinding.inflate(LayoutInflater.from(context), this, true)

    /**
     * @param isSenderAccount if true, cell title/subtitle are switched
     */
    fun updateAccount(
        account: CryptoAccount,
        onAccountClicked: (CryptoAccount) -> Unit = {},
        cellDecorator: CellDecorator = DefaultCellDecorator()
    ) {
        compositeDisposable.clear()
        updateView(account, onAccountClicked, cellDecorator)
    }

    private fun updateView(
        account: CryptoAccount,
        onAccountClicked: (CryptoAccount) -> Unit,
        cellDecorator: CellDecorator
    ) {
        val accountsAreTheSame = displayedAccount.isTheSameWith(account)
        updateAccountDetails(account, accountsAreTheSame, onAccountClicked, cellDecorator)

        (account as? InterestAccount)?.let { setInterestAccountDetails(account, accountsAreTheSame) }

        with(binding.assetWithAccount) {
            updateIcon(account)
            visible()
        }
        displayedAccount = account
    }

    private fun setInterestAccountDetails(
        account: CryptoAccount,
        accountsAreTheSame: Boolean
    ) {
        with(binding) {
            compositeDisposable += coincore[account.currency]
                .interestRate()
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { assetSubtitle.text = resources.getString(R.string.empty) }
                .doOnSuccess {
                    interestRate = it
                }.startWithValueIfCondition(value = interestRate, condition = accountsAreTheSame)
                .subscribeBy(
                    onNext = {
                        assetSubtitle.text = resources.getString(R.string.dashboard_asset_balance_rewards, it)
                    },
                    onError = {
                        assetSubtitle.text = resources.getString(
                            R.string.dashboard_asset_actions_rewards_dsc_failed
                        )
                        Timber.e("AssetActions error loading Interest rate: $it")
                    }
                )
        }
    }

    private fun updateAccountDetails(
        account: CryptoAccount,
        accountsAreTheSame: Boolean,
        onAccountClicked: (CryptoAccount) -> Unit,
        cellDecorator: CellDecorator
    ) {

        with(binding) {
            root.contentDescription = "$ACCOUNT_INFO_CRYPTO_VIEW_ID${account.currency.networkTicker}_${account.label}"
            val crypto = account.currency

            assetTitle.text = crypto.name
            assetSubtitle.text =  account.label


            compositeDisposable += account.balance.firstOrError().map { it.total }
                .doOnSuccess {
                    accountBalance = it
                }.startWithValueIfCondition(
                    value = accountBalance,
                    alternativeValue = Money.zero(account.currency),
                    condition = accountsAreTheSame
                )
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe {
                    walletBalanceCrypto.text = ""
                    walletBalanceFiat.text = ""
                }
                .subscribeBy(
                    onNext = { accountBalance ->
                        walletBalanceCrypto.text = accountBalance.toStringWithSymbol()
                        walletBalanceFiat.text =
                            accountBalance.toUserFiat(exchangeRates).toStringWithSymbol()
                    },
                    onError = {
                        Timber.e("Cannot get balance for ${account.label}")
                    }
                )
            compositeDisposable += cellDecorator.view(container.context)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onSuccess = {
                        container.addViewToBottomWithConstraints(
                            view = it,
                            bottomOfView = assetSubtitle,
                            startOfView = assetSubtitle,
                            endOfView = walletBalanceCrypto
                        )
                    },
                    onComplete = {
                        container.removePossibleBottomView()
                    },
                    onError = {
                        container.removePossibleBottomView()
                    }
                )

            container.alpha = 1f
            compositeDisposable += cellDecorator.isEnabled()
                .doOnSuccess {
                    isEnabled = it
                }.startWithValueIfCondition(value = isEnabled, condition = accountsAreTheSame)
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe {
                    setOnClickListener {
                    }
                }
                .subscribeBy(
                    onNext = { isEnabled ->
                        if (isEnabled) {
                            setOnClickListener {
                                onAccountClicked(account)
                            }
                            container.alpha = 1f
                        } else {
                            container.alpha = .6f
                        }
                    }
                )
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
        // Do nothing
    }

    override fun update(state: TransactionState) {
        updateAccount(
            account = state.sendingAccount as CryptoAccount,
            onAccountClicked = { }
        )
    }

    override fun setVisible(isVisible: Boolean) {
        binding.root.visibleIf { isVisible }
    }

    companion object {
        private const val ACCOUNT_INFO_CRYPTO_VIEW_ID = "AccountInfoCrypto_"
    }
}

private fun <T> Single<T>.startWithValueIfCondition(
    value: T?,
    alternativeValue: T? = null,
    condition: Boolean
): Observable<T> =
    if (!condition)
        this.toObservable()
    else {
        when {
            value != null -> this.toObservable().startWithItem(value)
            alternativeValue != null -> this.toObservable().startWithItem(alternativeValue)
            else -> this.toObservable()
        }
    }