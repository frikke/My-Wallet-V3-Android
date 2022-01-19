package piuk.blockchain.android.ui.dashboard.sheets

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.FiatAccount
import com.blockchain.coincore.NullFiatAccount
import com.blockchain.commonarch.presentation.base.SlidingModalBottomDialog
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.koin.scopedInject
import com.blockchain.notifications.analytics.LaunchOrigin
import com.blockchain.preferences.CurrencyPrefs
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.Singles
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.kotlin.subscribeBy
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.DialogSheetFiatFundsDetailBinding
import piuk.blockchain.android.ui.customviews.ToastCustom
import piuk.blockchain.android.ui.dashboard.assetdetails.AssetDetailsAnalytics
import piuk.blockchain.android.ui.dashboard.assetdetails.fiatAssetAction
import piuk.blockchain.android.ui.transactionflow.analytics.DepositAnalytics
import piuk.blockchain.android.ui.transactionflow.analytics.WithdrawAnalytics
import piuk.blockchain.android.util.gone
import piuk.blockchain.android.util.visible
import piuk.blockchain.android.util.visibleIf
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
                account.balance.firstOrError().map { it.total }.flatMap { balance ->
                    exchangeRates.exchangeRateToUserFiat(account.currency).firstOrError().map {
                        it.convert(balance) to balance
                    }
                },
                account.actions
            ).observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onSuccess = { (accountBalance, actions) ->
                        val balanceInWalletCurrency = accountBalance.first
                        val accountBalance = accountBalance.second
                        fundDetails.fundsUserFiatBalance.visibleIf {
                            prefs.selectedFiatCurrency.networkTicker != account.currency.networkTicker
                        }
                        fundDetails.fundsUserFiatBalance.text = balanceInWalletCurrency.toStringWithSymbol()
                        fundDetails.fundsBalance.text = accountBalance.toStringWithSymbol()
                        fundDetails.fundsBalance.visibleIf { accountBalance.isZero || accountBalance.isPositive }
                        fundsWithdrawHolder.visibleIf { actions.contains(AssetAction.Withdraw) }
                        fundsDepositHolder.visibleIf { actions.contains(AssetAction.FiatDeposit) }
                        fundsActivityHolder.visibleIf { actions.contains(AssetAction.ViewActivity) }
                    },
                    onError = {
                        Timber.e("Error getting fiat funds balances: $it")
                        showErrorToast()
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

    private fun handleWithdrawalChecks() {
        disposables += account.canWithdrawFunds()
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
                        ToastCustom.makeText(
                            requireContext(), getString(R.string.fiat_funds_detail_pending_withdrawal),
                            Toast.LENGTH_LONG, ToastCustom.TYPE_ERROR
                        )
                    }
                },
                onError = {
                    Timber.e("Error getting transactions for withdrawal $it")
                    ToastCustom.makeText(
                        requireContext(), getString(R.string.common_error),
                        Toast.LENGTH_LONG, ToastCustom.TYPE_ERROR
                    )
                }
            )
    }

    private fun showErrorToast() {
        ToastCustom.makeText(
            requireContext(), getString(R.string.common_error), Toast.LENGTH_SHORT,
            ToastCustom.TYPE_ERROR
        )
    }

    companion object {
        fun newInstance(fiatAccount: FiatAccount): FiatFundsDetailSheet {
            return FiatFundsDetailSheet().apply {
                account = fiatAccount
            }
        }
    }
}
