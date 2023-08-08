package piuk.blockchain.android.ui.customviews.account

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import com.blockchain.coincore.Coincore
import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.CryptoAsset
import com.blockchain.coincore.SingleAccount
import com.blockchain.coincore.impl.CryptoNonCustodialAccount
import com.blockchain.coincore.impl.CustodialTradingAccount
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.tablerow.custom.StackedIcon
import com.blockchain.componentlib.viewextensions.visibleIf
import com.blockchain.core.price.ExchangeRates
import com.blockchain.koin.scopedInject
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.CoinNetwork
import info.blockchain.balance.isLayer2Token
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
    private val assetCatalogue: AssetCatalogue by scopedInject()
    private val compositeDisposable = CompositeDisposable()
    private var isEnabled: Boolean? = null

    val binding: ViewAccountCryptoOverviewBinding =
        ViewAccountCryptoOverviewBinding.inflate(LayoutInflater.from(context), this, true)

    fun updateBackground(
        isFirstItemInList: Boolean,
        isLastItemInList: Boolean,
        isSelected: Boolean
    ) {
        with(binding.tableRow) {
            roundedTop = isFirstItemInList
            roundedBottom = isLastItemInList
            withBorder = isSelected
        }
    }

    fun updateItem(
        item: AccountListViewItem,
        onAccountClicked: (SingleAccount) -> Unit = {},
        cellDecorator: CellDecorator = DefaultCellDecorator()
    ) {
        compositeDisposable.clear()
        updateView(item, onAccountClicked, cellDecorator)
    }

    private fun updateView(
        item: AccountListViewItem,
        onAccountClicked: (SingleAccount) -> Unit,
        cellDecorator: CellDecorator
    ) {
        updateAccountDetails(item, onAccountClicked, cellDecorator)

        if (item.showRewardsUpsell) setInterestAccountDetails(item.account)

        val mainLogo = ImageResource.Remote(item.account.currency.logo)
        val tagLogo = item.l2Network?.nativeAssetTicker
            ?.let { assetCatalogue.fromNetworkTicker(it)?.logo }
            ?.let { ImageResource.Remote(it) }
        binding.tableRow.icon = tagLogo?.let {
            StackedIcon.SmallTag(
                main = mainLogo,
                tag = tagLogo
            )
        } ?: StackedIcon.SingleIcon(mainLogo)

        binding.tableRow.tag = item.l2Network?.shortName ?: ""
    }

    private fun setInterestAccountDetails(
        account: SingleAccount
    ) {
        with(binding.tableRow) {
            compositeDisposable += (coincore[account.currency] as CryptoAsset)
                .interestRate()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onSuccess = {
                        subtitle = resources.getString(
                            com.blockchain.stringResources.R.string.dashboard_asset_balance_rewards,
                            it
                        )
                    },
                    onError = {
                        subtitle = resources.getString(
                            com.blockchain.stringResources.R.string.dashboard_asset_actions_rewards_dsc_failed
                        )
                        Timber.e("AssetActions error loading Interest rate: $it")
                    }
                )
        }
    }

    private fun updateAccountDetails(
        item: AccountListViewItem,
        onAccountClicked: (SingleAccount) -> Unit,
        cellDecorator: CellDecorator
    ) {
        with(binding.tableRow) {
            val account = item.account
            contentDescription = "${item.title} ${item.subTitle}"
            title = item.title
            subtitle = if (item.account !is CustodialTradingAccount) {
                item.subTitle
            } else {
                ""
            }

            compositeDisposable += account.balanceRx()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onNext = { accountBalance ->
                        valueCrypto = accountBalance.total.toStringWithSymbol()
                        valueFiat = accountBalance.totalFiat?.toStringWithSymbol().orEmpty()
                        contentDescription = "${item.title} ${item.subTitle}: " +
                            "${context.getString(com.blockchain.stringResources.R.string.accessibility_balance)} " +
                            "$valueFiat $valueCrypto"
                    },
                    onError = {
                        Timber.e("Cannot get balance for ${account.label}")
                    }
                )

            compositeDisposable += cellDecorator.view(context)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onSuccess = {
                        subView = it
                    },
                    onComplete = {
                        subView = null
                    },
                    onError = {
                        subView = null
                    }
                )

            alpha = 1f

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
                            onClick = { onAccountClicked(account) }
                            alpha = 1f
                        } else {
                            alpha = .6f
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

    private val accountLabel = if (account is CustodialTradingAccount) {
        account.label.replaceAfter("Blockchain.com", "")
            .replaceBefore("Blockchain.com", "")
    } else account.label

    val title: String
        get() = if (emphasiseNameOverCurrency) accountLabel else account.currency.name

    val subTitle: String
        get() = if (emphasiseNameOverCurrency) account.currency.displayTicker else accountLabel

    val l2Network: CoinNetwork?
        get() = (account as? CryptoNonCustodialAccount)?.currency
            ?.takeIf { it.isLayer2Token }
            ?.coinNetwork
}

enum class AccountsListViewItemType {
    Crypto, Blockchain
}
