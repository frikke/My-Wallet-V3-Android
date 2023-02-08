package piuk.blockchain.android.ui.customviews.account

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import com.blockchain.coincore.Coincore
import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.CryptoAsset
import com.blockchain.coincore.SingleAccount
import com.blockchain.coincore.impl.CustodialTradingAccount
import com.blockchain.coincore.toUserFiat
import com.blockchain.componentlib.viewextensions.gone
import com.blockchain.componentlib.viewextensions.visible
import com.blockchain.componentlib.viewextensions.visibleIf
import com.blockchain.core.price.ExchangeRates
import com.blockchain.koin.scopedInject
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
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
    private var isEnabled: Boolean? = null

    val binding: ViewAccountCryptoOverviewBinding =
        ViewAccountCryptoOverviewBinding.inflate(LayoutInflater.from(context), this, true)

    fun updateItem(
        item: AccountListViewItem,
        onAccountClicked: (SingleAccount) -> Unit = {},
        cellDecorator: CellDecorator = DefaultCellDecorator(),
    ) {
        compositeDisposable.clear()
        updateView(item, onAccountClicked, cellDecorator)
    }

    private fun updateView(
        item: AccountListViewItem,
        onAccountClicked: (SingleAccount) -> Unit,
        cellDecorator: CellDecorator,
    ) {
        updateAccountDetails(item, onAccountClicked, cellDecorator)

        if (item.showRewardsUpsell) setInterestAccountDetails(item.account)

        with(binding.assetWithAccount) {
            updateIcon(item.account)
            visible()
        }
    }

    private fun setInterestAccountDetails(
        account: SingleAccount,
    ) {
        with(binding) {
            compositeDisposable += (coincore[account.currency] as CryptoAsset)
                .interestRate()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onSuccess = {
                        assetSubtitle.text = resources.getString(R.string.dashboard_asset_balance_rewards, it)
                        assetSubtitle.visible()
                    },
                    onError = {
                        assetSubtitle.text = resources.getString(
                            R.string.dashboard_asset_actions_rewards_dsc_failed
                        )
                        assetSubtitle.visible()
                        Timber.e("AssetActions error loading Interest rate: $it")
                    }
                )
        }
    }

    private fun updateAccountDetails(
        item: AccountListViewItem,
        onAccountClicked: (SingleAccount) -> Unit,
        cellDecorator: CellDecorator,
    ) {
        with(binding) {
            val account = item.account
            root.contentDescription = "${item.title} ${item.subTitle}"
            assetTitle.text = item.title
            if (item.account !is CustodialTradingAccount) {
                assetSubtitle.apply {
                    text = item.subTitle
                    visible()
                }
            } else {
                assetSubtitle.gone()
            }

            compositeDisposable += account.balanceRx().map { it.total }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onNext = { accountBalance ->
                        walletBalanceCrypto.text = accountBalance.toStringWithSymbol()
                        walletBalanceFiat.text =
                            accountBalance.toUserFiat(exchangeRates).toStringWithSymbol()
                        root.contentDescription = "${item.title} ${item.subTitle}: " +
                            "${context.getString(R.string.accessibility_balance)} " +
                            "${walletBalanceFiat.text} ${walletBalanceCrypto.text}"
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
                }
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe {
                    setOnClickListener {
                    }
                }
                .subscribeBy(
                    onSuccess = { isEnabled ->
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
        updateItem(
            item = AccountListViewItem(state.sendingAccount),
            onAccountClicked = { }
        )
    }

    override fun setVisible(isVisible: Boolean) {
        binding.root.visibleIf { isVisible }
    }
}

class AccountListViewItem(
    val account: SingleAccount,
    private val emphasiseNameOverCurrency: Boolean = false,
    val showRewardsUpsell: Boolean = false
) {
    val type: AccountsListViewItemType
        get() = if (account is CryptoAccount) AccountsListViewItemType.Crypto else AccountsListViewItemType.Blockchain

    private val currencyName = account.currency.name

    private val accountLabel = if (account is CustodialTradingAccount)
        account.label.replaceAfter("Blockchain.com", "")
            .replaceBefore("Blockchain.com", "") else account.label

    val title: String
        get() = if (emphasiseNameOverCurrency) accountLabel else currencyName

    val subTitle: String
        get() = if (emphasiseNameOverCurrency) currencyName else accountLabel
}

enum class AccountsListViewItemType {
    Crypto, Blockchain
}
