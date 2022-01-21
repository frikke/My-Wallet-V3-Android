package piuk.blockchain.android.ui.interest

import android.content.DialogInterface
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.Coincore
import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.InterestAccount
import com.blockchain.coincore.SingleAccount
import com.blockchain.coincore.toUserFiat
import com.blockchain.commonarch.presentation.base.SlidingModalBottomDialog
import com.blockchain.componentlib.viewextensions.gone
import com.blockchain.componentlib.viewextensions.visible
import com.blockchain.core.interest.InterestBalanceDataManager
import com.blockchain.core.price.ExchangeRates
import com.blockchain.featureflags.InternalFeatureFlagApi
import com.blockchain.koin.scopedInject
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.notifications.analytics.LaunchOrigin
import com.blockchain.utils.secondsToDays
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.Money
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.Singles
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.kotlin.subscribeBy
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.DialogSheetInterestDetailsBinding
import piuk.blockchain.android.ui.customviews.BlockchainListDividerDecor
import piuk.blockchain.android.ui.transactionflow.analytics.InterestAnalytics
import timber.log.Timber

class InterestSummarySheet : SlidingModalBottomDialog<DialogSheetInterestDetailsBinding>() {
    interface Host : SlidingModalBottomDialog.Host {
        fun goToActivityFor(account: BlockchainAccount)
        fun goToInterestDeposit(toAccount: InterestAccount)
        fun goToInterestWithdraw(fromAccount: InterestAccount)
    }

    override val host: Host by lazy {
        super.host as? Host ?: throw IllegalStateException(
            "Host fragment is not a InterestSummarySheet.Host"
        )
    }

    private lateinit var account: SingleAccount
    private lateinit var asset: AssetInfo

    override fun initBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): DialogSheetInterestDetailsBinding =
        DialogSheetInterestDetailsBinding.inflate(inflater, container, false)

    private val disposables = CompositeDisposable()
    private val interestBalance: InterestBalanceDataManager by scopedInject()
    private val custodialWalletManager: CustodialWalletManager by scopedInject()
    private val exchangeRates: ExchangeRates by scopedInject()
    private val coincore: Coincore by scopedInject()

    @Suppress("unused")
    private val features: InternalFeatureFlagApi by inject()

    private val listAdapter: InterestSummaryAdapter by lazy { InterestSummaryAdapter() }

    override fun initControls(binding: DialogSheetInterestDetailsBinding) {
        binding.interestDetailsList.apply {
            layoutManager = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
            addItemDecoration(BlockchainListDividerDecor(requireContext()))
            adapter = listAdapter
            layoutAnimation = AnimationUtils.loadLayoutAnimation(requireContext(), R.anim.layout_animation_fall_down)
        }

        binding.apply {
            interestDetailsTitle.text = account.label
            interestDetailsSheetHeader.text = asset.name
            interestDetailsLabel.text = asset.name

            interestDetailsAssetWithIcon.updateIcon(account as CryptoAccount)

            disposables += coincore.allWalletsWithActions(setOf(AssetAction.InterestDeposit)).map { accounts ->
                accounts.filter { account -> account is CryptoAccount && account.currency == asset }
            }
                .onErrorReturn { emptyList() }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy { accounts ->
                    if (accounts.isNotEmpty() && accounts.any { it is InterestAccount }) {
                        interestDetailsDepositCta.alpha = 0f
                        interestDetailsDepositCta.visible()
                        interestDetailsDepositCta.animate().alpha(1f).start()
                        interestDetailsDepositCta.text =
                            getString(R.string.tx_title_deposit, asset.displayTicker)
                        interestDetailsDepositCta.setOnClickListener {
                            analytics.logEvent(InterestAnalytics.InterestSummaryDepositCta)
                            host.goToInterestDeposit(account as InterestAccount)
                        }
                    } else {
                        interestDetailsDepositCta.gone()
                    }
                }
        }

        disposables += Singles.zip(
            interestBalance.getBalanceForAsset(asset).firstOrError(),
            custodialWalletManager.getInterestLimits(asset),
            custodialWalletManager.getInterestAccountRates(asset)
        ).observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onSuccess = { (details, limits, interestRate) ->
                    compositeToView(
                        CompositeInterestDetails(
                            totalInterest = details.totalInterest,
                            pendingInterest = details.pendingInterest,
                            balance = (details.totalBalance - details.lockedBalance) as CryptoValue,
                            lockupDuration = limits.interestLockUpDuration.secondsToDays(),
                            interestRate = interestRate,
                            nextInterestPayment = limits.nextInterestPayment
                        )
                    )
                },
                onError = {
                    Timber.e("Error loading interest summary details: $it")
                }
            )
    }

    private fun compositeToView(composite: CompositeInterestDetails) {
        with(binding) {
            if (composite.balance.isPositive) {
                interestDetailsWithdrawCta.text =
                    getString(R.string.tx_title_withdraw, asset.displayTicker)
                interestDetailsWithdrawCta.visible()
                interestDetailsWithdrawCta.setOnClickListener {
                    analytics.logEvent(
                        InterestAnalytics.InterestWithdrawalClicked(
                            currency = composite.balance.currencyCode,
                            origin = LaunchOrigin.SAVINGS_PAGE
                        )
                    )
                    analytics.logEvent(InterestAnalytics.InterestSummaryWithdrawCta)
                    host.goToInterestWithdraw(account as InterestAccount)
                }
            }
        }

        val itemList = mutableListOf<InterestSummaryInfoItem>()
        itemList.apply {
            add(
                InterestSummaryInfoItem(
                    getString(R.string.rewards_summary_total),
                    composite.totalInterest.toStringWithSymbol()
                )
            )

            val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            val formattedDate = sdf.format(composite.nextInterestPayment)
            add(InterestSummaryInfoItem(getString(R.string.rewards_summary_next_payment), formattedDate))

            add(
                InterestSummaryInfoItem(
                    getString(R.string.rewards_summary_accrued),
                    composite.pendingInterest.toStringWithSymbol()
                )
            )

            add(
                InterestSummaryInfoItem(
                    getString(R.string.rewards_summary_hold_period),
                    getString(R.string.rewards_summary_hold_period_days, composite.lockupDuration)
                )
            )

            add(InterestSummaryInfoItem(getString(R.string.rewards_summary_rate), "${composite.interestRate}%"))
        }

        composite.balance.run {
            binding.apply {
                interestDetailsCryptoValue.text = toStringWithSymbol()
                interestDetailsFiatValue.text = toUserFiat(exchangeRates)
                    .toStringWithSymbol()
            }
        }

        listAdapter.items = itemList
    }

    companion object {
        fun newInstance(
            singleAccount: CryptoAccount
        ): InterestSummarySheet =
            InterestSummarySheet().apply {
                account = singleAccount
                asset = singleAccount.currency
            }
    }

    data class InterestSummaryInfoItem(
        val title: String,
        val label: String
    )

    private data class CompositeInterestDetails(
        val balance: Money,
        val totalInterest: Money,
        val pendingInterest: Money,
        var nextInterestPayment: Date,
        val lockupDuration: Int,
        val interestRate: Double
    )

    override fun dismiss() {
        super.dismiss()
        disposables.clear()
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        disposables.clear()
    }
}
