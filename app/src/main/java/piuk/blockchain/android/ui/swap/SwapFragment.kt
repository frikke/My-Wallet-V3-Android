package piuk.blockchain.android.ui.swap

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewbinding.ViewBinding
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.Coincore
import com.blockchain.coincore.TrendingPair
import com.blockchain.coincore.TrendingPairsProvider
import com.blockchain.coincore.toUserFiat
import com.blockchain.commonarch.presentation.base.SlidingModalBottomDialog
import com.blockchain.componentlib.alert.abstract.SnackbarType
import com.blockchain.componentlib.viewextensions.gone
import com.blockchain.componentlib.viewextensions.visible
import com.blockchain.componentlib.viewextensions.visibleIf
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.koin.scopedInject
import com.blockchain.nabu.datamanagers.CustodialOrder
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.datamanagers.Product
import com.blockchain.nabu.datamanagers.TransferLimits
import com.blockchain.nabu.models.responses.nabu.KycTierLevel
import com.blockchain.nabu.models.responses.nabu.KycTiers
import com.blockchain.nabu.service.TierService
import com.blockchain.notifications.analytics.Analytics
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.preferences.WalletStatus
import info.blockchain.balance.Money
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.kotlin.subscribeBy
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.campaign.CampaignType
import piuk.blockchain.android.databinding.FragmentSwapBinding
import piuk.blockchain.android.ui.customviews.BlockchainSnackbar
import piuk.blockchain.android.ui.customviews.ButtonOptions
import piuk.blockchain.android.ui.customviews.KycBenefitsBottomSheet
import piuk.blockchain.android.ui.customviews.VerifyIdentityNumericBenefitItem
import piuk.blockchain.android.ui.kyc.navhost.KycNavHostActivity
import piuk.blockchain.android.ui.resources.AssetResources
import piuk.blockchain.android.ui.transactionflow.analytics.SwapAnalyticsEvents
import piuk.blockchain.android.ui.transactionflow.analytics.TxFlowAnalyticsAccountType
import piuk.blockchain.android.ui.transactionflow.flow.TransactionFlowActivity
import timber.log.Timber

class SwapFragment :
    Fragment(),
    KycBenefitsBottomSheet.Host,
    TradingWalletPromoBottomSheet.Host {

    interface Host {
        fun navigateToReceive()
        fun navigateToBuy()
    }

    private var _binding: FragmentSwapBinding? = null

    private val binding: FragmentSwapBinding
        get() = _binding!!

    private val host: Host
        get() = requireActivity() as Host

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSwapBinding.inflate(inflater, container, false)
        return binding.root
    }

    private val kycTierService: TierService by scopedInject()
    private val coincore: Coincore by scopedInject()
    private val exchangeRateDataManager: ExchangeRatesDataManager by scopedInject()
    private val trendingPairsProvider: TrendingPairsProvider by scopedInject()
    private val walletManager: CustodialWalletManager by scopedInject()

    private val currencyPrefs: CurrencyPrefs by inject()
    private val walletPrefs: WalletStatus by inject()
    private val analytics: Analytics by inject()
    private val assetResources: AssetResources by inject()
    private val compositeDisposable = CompositeDisposable()

    private val startActivityForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        loadSwapOrKyc(showLoading = true)
    }

    private val startKycForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        loadSwapOrKyc(showLoading = true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.swapError.setDetails {
            loadSwapOrKyc(true)
        }

        binding.swapCta.apply {
            analytics.logEvent(SwapAnalyticsEvents.NewSwapClicked)
            setOnClickListener {
                if (!walletPrefs.hasSeenTradingSwapPromo) {
                    walletPrefs.setSeenTradingSwapPromo()
                    showBottomSheet(TradingWalletPromoBottomSheet.newInstance())
                } else {
                    startSwap()
                }
            }
            gone()
        }
        binding.pendingSwaps.pendingList.addItemDecoration(
            DividerItemDecoration(
                context,
                DividerItemDecoration.VERTICAL
            )
        )
        binding.cardBuyNow.setOnClickListener {
            host.navigateToBuy()
        }
        binding.cardReceive.setOnClickListener {
            host.navigateToReceive()
        }

        analytics.logEvent(SwapAnalyticsEvents.SwapViewedEvent)
        loadSwapOrKyc(showLoading = true)
    }

    private fun startSwap() = startActivityForResult.launch(
        TransactionFlowActivity.newInstance(
            context = requireActivity(),
            action = AssetAction.Swap
        )
    )

    override fun verificationCtaClicked() {
        analytics.logEvent(SwapAnalyticsEvents.SwapSilverLimitSheetCta)
        walletPrefs.setSeenSwapPromo()
        KycNavHostActivity.start(requireActivity(), CampaignType.Swap)
    }

    override fun onSheetClosed() {
        walletPrefs.setSeenSwapPromo()
    }

    override fun startNewSwap() {
        startSwap()
    }

    private fun loadSwapOrKyc(showLoading: Boolean) {
        compositeDisposable +=
            Single.zip(
                kycTierService.tiers(),
                trendingPairsProvider.getTrendingPairs(),
                walletManager.getProductTransferLimits(currencyPrefs.selectedFiatCurrency, Product.TRADE),
                walletManager.getSwapTrades().onErrorReturn { emptyList() },
                coincore.allWalletsWithActions(setOf(AssetAction.Swap))
                    .map { it.isNotEmpty() }
            ) { tiers: KycTiers,
                pairs: List<TrendingPair>,
                limits: TransferLimits,
                orders: List<CustodialOrder>,
                hasAtLeastOneAccountToSwapFrom ->
                SwapComposite(
                    tiers,
                    pairs,
                    limits,
                    orders,
                    hasAtLeastOneAccountToSwapFrom
                )
            }
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { if (showLoading) showLoading() }
                .doOnTerminate { hideLoading() }
                .subscribeBy(
                    onSuccess = { composite ->
                        showSwapUi(composite.orders, composite.hasAtLeastOneAccountToSwapFrom)

                        if (composite.tiers.isVerified()) {
                            binding.swapViewFlipper.displayedChild =
                                if (composite.hasAtLeastOneAccountToSwapFrom) SWAP_VIEW
                                else SWAP_NO_ACCOUNTS
                            binding.swapHeader.toggleBottomSeparator(false)

                            val onPairClicked = onTrendingPairClicked()

                            binding.swapTrending.initialise(
                                pairs = composite.pairs,
                                onSwapPairClicked = onPairClicked,
                                assetResources = assetResources
                            )

                            if (!composite.tiers.isInitialisedFor(KycTierLevel.GOLD)) {
                                showKycUpsellIfEligible(composite.limits)
                            }
                        } else {
                            binding.swapViewFlipper.displayedChild = KYC_VIEW
                            initKycView()
                        }
                    },
                    onError = {
                        showErrorUi()

                        BlockchainSnackbar.make(
                            binding.root, getString(R.string.transfer_wallets_load_error), type = SnackbarType.Error
                        ).show()

                        Timber.e("Error loading swap kyc service $it")
                    }
                )
    }

    private fun showKycUpsellIfEligible(limits: TransferLimits) {
        val usedUpLimitPercent = (limits.maxLimit / limits.maxOrder).toFloat() * 100
        if (usedUpLimitPercent >= KYC_UPSELL_PERCENTAGE && !walletPrefs.hasSeenSwapPromo) {
            analytics.logEvent(SwapAnalyticsEvents.SwapSilverLimitSheet)
            val fragment = KycBenefitsBottomSheet.newInstance(
                KycBenefitsBottomSheet.BenefitsDetails(
                    title = getString(R.string.swap_kyc_upsell_title),
                    description = getString(R.string.swap_kyc_upsell_desc),
                    listOfBenefits = listOf(
                        VerifyIdentityNumericBenefitItem(
                            getString(R.string.swap_kyc_upsell_1_title),
                            getString(R.string.swap_kyc_upsell_1_desc)
                        ),
                        VerifyIdentityNumericBenefitItem(
                            getString(R.string.swap_kyc_upsell_2_title),
                            getString(R.string.swap_kyc_upsell_2_desc)
                        ),
                        VerifyIdentityNumericBenefitItem(
                            getString(R.string.swap_kyc_upsell_3_title),
                            getString(R.string.swap_kyc_upsell_3_desc)
                        )
                    )
                )
            )
            showBottomSheet(fragment)
        }
    }

    private fun <T : ViewBinding> showBottomSheet(fragment: SlidingModalBottomDialog<T>) {
        childFragmentManager.beginTransaction().add(fragment, TAG).commit()
    }

    private fun onTrendingPairClicked(): (TrendingPair) -> Unit = { pair ->
        analytics.logEvent(SwapAnalyticsEvents.TrendingPairClicked)
        analytics.logEvent(
            SwapAnalyticsEvents.SwapAccountsSelected(
                inputCurrency = pair.sourceAccount.currency,
                outputCurrency = pair.destinationAccount.currency,
                sourceAccountType = TxFlowAnalyticsAccountType.fromAccount(pair.sourceAccount),
                targetAccountType = TxFlowAnalyticsAccountType.fromAccount(pair.destinationAccount),
                werePreselected = true
            )
        )

        startActivityForResult.launch(
            TransactionFlowActivity.newInstance(
                context = requireActivity(),
                sourceAccount = pair.sourceAccount,
                target = pair.destinationAccount,
                action = AssetAction.Swap
            )
        )
    }

    private fun initKycView() {
        binding.swapKycBenefits.initWithBenefits(
            listOf(
                VerifyIdentityNumericBenefitItem(
                    getString(R.string.swap_kyc_1_title),
                    getString(R.string.swap_kyc_1_label)
                ),
                VerifyIdentityNumericBenefitItem(
                    getString(R.string.swap_kyc_2_title),
                    getString(R.string.swap_kyc_2_label)
                ),
                VerifyIdentityNumericBenefitItem(
                    getString(R.string.swap_kyc_3_title),
                    getString(R.string.swap_kyc_3_label)
                )
            ),
            getString(R.string.swap_kyc_title),
            getString(R.string.swap_kyc_desc),
            R.drawable.ic_swap_blue_circle,
            ButtonOptions(visible = true, text = getString(R.string.swap_kyc_cta)) {
                analytics.logEvent(SwapAnalyticsEvents.VerifyNowClicked)
                startKycForResult.launch(KycNavHostActivity.newIntent(requireActivity(), CampaignType.Swap))
            },
            ButtonOptions(visible = false),
            showSheetIndicator = false
        )
    }

    private fun showErrorUi() {
        binding.swapError.visible()
    }

    private fun showSwapUi(orders: List<CustodialOrder>, hasAtLeastOneAccountToSwapFrom: Boolean) {
        val pendingOrders = orders.filter { it.state.isPending }
        val hasPendingOrder = pendingOrders.isNotEmpty()
        binding.swapViewFlipper.visible()
        binding.swapError.gone()
        binding.swapCta.visible()
        binding.swapCta.isEnabled = hasAtLeastOneAccountToSwapFrom
        binding.swapTrending.visibleIf { !hasPendingOrder }
        binding.pendingSwaps.container.visibleIf { hasPendingOrder }
        binding.pendingSwaps.pendingList.apply {
            adapter =
                PendingSwapsAdapter(
                    pendingOrders
                ) { money: Money ->
                    money.toUserFiat(exchangeRateDataManager)
                }
            layoutManager = LinearLayoutManager(activity)
        }
    }

    private fun showLoading() {
        binding.progress.visible()
        binding.progress.playAnimation()
    }

    private fun hideLoading() {
        binding.progress.gone()
        binding.progress.pauseAnimation()
    }

    companion object {
        private const val KYC_UPSELL_PERCENTAGE = 90
        private const val SWAP_VIEW = 0
        private const val SWAP_NO_ACCOUNTS = 1
        private const val KYC_VIEW = 2
        private const val TAG = "BOTTOM_SHEET"
        fun newInstance(): SwapFragment =
            SwapFragment()
    }

    private data class SwapComposite(
        val tiers: KycTiers,
        val pairs: List<TrendingPair>,
        val limits: TransferLimits,
        val orders: List<CustodialOrder>,
        val hasAtLeastOneAccountToSwapFrom: Boolean
    )

    override fun onDestroyView() {
        compositeDisposable.clear()
        _binding = null
        super.onDestroyView()
    }
}
