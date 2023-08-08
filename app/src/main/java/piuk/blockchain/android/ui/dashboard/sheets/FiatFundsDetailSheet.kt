package piuk.blockchain.android.ui.dashboard.sheets

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.StringRes
import com.blockchain.analytics.events.LaunchOrigin
import com.blockchain.coincore.ActionState
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.FiatAccount
import com.blockchain.coincore.NullFiatAccount
import com.blockchain.coincore.StateAwareAction
import com.blockchain.commonarch.presentation.base.SlidingModalBottomDialog
import com.blockchain.componentlib.alert.BlockchainSnackbar
import com.blockchain.componentlib.alert.SnackbarType
import com.blockchain.componentlib.viewextensions.gone
import com.blockchain.componentlib.viewextensions.visible
import com.blockchain.componentlib.viewextensions.visibleIf
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.data.toObservable
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.presentation.koin.scopedInject
import com.google.android.material.snackbar.Snackbar
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.Singles
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.kotlin.subscribeBy
import piuk.blockchain.android.databinding.DialogSheetFiatFundsDetailBinding
import piuk.blockchain.android.ui.dashboard.assetdetails.AssetDetailsAnalytics
import piuk.blockchain.android.ui.dashboard.assetdetails.fiatAssetAction
import piuk.blockchain.android.ui.transactionflow.analytics.DepositAnalytics
import piuk.blockchain.android.ui.transactionflow.analytics.WithdrawAnalytics
import timber.log.Timber

class FiatFundsDetailSheet : SlidingModalBottomDialog<DialogSheetFiatFundsDetailBinding>() {

    interface Host : SlidingModalBottomDialog.Host {
        fun goToActivityFor(account: BlockchainAccount)
        fun showFundsKyc()
        fun startBankTransferWithdrawal(fiatAccount: FiatAccount)
        fun startDepositFlow(fiatAccount: FiatAccount)
    }

    override val host: Host by lazy {
        super.host as? Host ?: throw IllegalStateException(
            "Host fragment is not a FiatFundsDetailSheet.Host"
        )
    }

    private val prefs: CurrencyPrefs by scopedInject()
    private val exchangeRates: ExchangeRatesDataManager by scopedInject()
    private val disposables = CompositeDisposable()

    private var account: FiatAccount = NullFiatAccount

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): DialogSheetFiatFundsDetailBinding =
        DialogSheetFiatFundsDetailBinding.inflate(inflater, container, false)

    override fun initControls(binding: DialogSheetFiatFundsDetailBinding) {
        val currency = account.currency
        binding.apply {
            with(fundDetails) {
                fundsTitle.text = currency.name
                fundsFiatTicker.text = currency.displayTicker
                fundsIcon.setIcon(currency)
                fundsBalance.gone()
                fundsUserFiatBalance.gone()
            }
            disposables += Singles.zip(
                account.balanceRx().firstOrError().map { it.total }.flatMap { balance ->
                    exchangeRates.exchangeRateToUserFiat(account.currency).firstOrError().map {
                        it.convert(balance) to balance
                    }
                },
                account.stateAwareActions
            ).observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onSuccess = { (accountBalance, stateAwareActions) ->
                        val balanceInWalletCurrency = accountBalance.first
                        val balance = accountBalance.second
                        fundDetails.fundsUserFiatBalance.visibleIf {
                            prefs.selectedFiatCurrency.networkTicker != account.currency.networkTicker
                        }
                        fundDetails.fundsUserFiatBalance.text = balanceInWalletCurrency.toStringWithSymbol()
                        fundDetails.fundsBalance.text = balance.toStringWithSymbol()
                        fundDetails.fundsBalance.visibleIf { balance.isZero || balance.isPositive }
                        fundsWithdrawHolder.visibleIf { stateAwareActions.hasAvailableAction(AssetAction.FiatWithdraw) }
                        fundsDepositHolder.visibleIf { stateAwareActions.hasAvailableAction(AssetAction.FiatDeposit) }
                        fundsActivityHolder.visibleIf { stateAwareActions.hasAvailableAction(AssetAction.ViewActivity) }
                    },
                    onError = {
                        Timber.e("Error getting fiat funds balances: $it")
                        showErrorSnackbar()
                    }
                )

            fundsDepositHolder.setOnClickListener {
                analytics.logEvent(
                    fiatAssetAction(AssetDetailsAnalytics.FIAT_DEPOSIT_CLICKED, account.currency.networkTicker)
                )
                analytics.logEvent(DepositAnalytics.DepositClicked(LaunchOrigin.CURRENCY_PAGE))
                dismiss()
                host.startDepositFlow(account)
            }
            fundsWithdrawHolder.setOnClickListener {
                analytics.logEvent(WithdrawAnalytics.WithdrawalClicked(LaunchOrigin.CURRENCY_PAGE))
                analytics.logEvent(
                    fiatAssetAction(AssetDetailsAnalytics.FIAT_WITHDRAW_CLICKED, account.currency.networkTicker)
                )
                handleWithdrawalChecks()
            }

            fundsActivityHolder.setOnClickListener {
                analytics.logEvent(
                    fiatAssetAction(AssetDetailsAnalytics.FIAT_ACTIVITY_CLICKED, account.currency.networkTicker)
                )
                dismiss()
                host.goToActivityFor(account)
            }
        }
    }

    private fun Set<StateAwareAction>.hasAvailableAction(action: AssetAction): Boolean =
        firstOrNull { it.action == action && it.state == ActionState.Available } != null

    private fun handleWithdrawalChecks() {
        disposables += account.canWithdrawFunds().toObservable().firstOrError()
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe {
                binding.fundsSheetProgress.visible()
            }.doFinally {
                binding.fundsSheetProgress.gone()
            }.subscribeBy(
                onSuccess = {
                    if (it) {
                        dismiss()
                        host.startBankTransferWithdrawal(fiatAccount = account)
                    } else {
                        showErrorSnackbar(com.blockchain.stringResources.R.string.fiat_funds_detail_pending_withdrawal)
                    }
                },
                onError = {
                    Timber.e("Error getting transactions for withdrawal $it")
                    showErrorSnackbar()
                }
            )
    }

    private fun showErrorSnackbar(@StringRes error: Int = com.blockchain.stringResources.R.string.common_error) {
        BlockchainSnackbar.make(
            binding.root,
            getString(error),
            duration = Snackbar.LENGTH_SHORT,
            type = SnackbarType.Error
        ).show()
    }

    companion object {
        fun newInstance(fiatAccount: FiatAccount): FiatFundsDetailSheet {
            return FiatFundsDetailSheet().apply {
                account = fiatAccount
            }
        }
    }
}
