package piuk.blockchain.android.ui.swap

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.blockchain.analytics.Analytics
import com.blockchain.api.NabuApiException
import com.blockchain.api.NabuApiExceptionFactory
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.Coincore
import com.blockchain.coincore.TrendingPair
import com.blockchain.coincore.TrendingPairsProvider
import com.blockchain.coincore.toUserFiat
import com.blockchain.componentlib.alert.BlockchainSnackbar
import com.blockchain.componentlib.alert.SnackbarType
import com.blockchain.componentlib.viewextensions.gone
import com.blockchain.componentlib.viewextensions.visible
import com.blockchain.componentlib.viewextensions.visibleIf
import com.blockchain.core.kyc.domain.KycService
import com.blockchain.core.kyc.domain.model.KycTier
import com.blockchain.core.kyc.domain.model.KycTiers
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.extensions.exhaustive
import com.blockchain.nabu.BlockedReason
import com.blockchain.nabu.Feature
import com.blockchain.nabu.FeatureAccess
import com.blockchain.nabu.UserIdentity
import com.blockchain.nabu.datamanagers.CustodialOrder
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.datamanagers.Product
import com.blockchain.nabu.datamanagers.TransferLimits
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.preferences.WalletStatusPrefs
import com.blockchain.presentation.koin.scopedInject
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
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
import piuk.blockchain.android.simplebuy.ClientErrorAnalytics
import piuk.blockchain.android.simplebuy.ClientErrorAnalytics.Companion.ACTION_SWAP
import piuk.blockchain.android.support.SupportCentreActivity
import piuk.blockchain.android.ui.customviews.BlockedDueToSanctionsSheet
import piuk.blockchain.android.ui.customviews.ButtonOptions
import piuk.blockchain.android.ui.customviews.KycBenefitsBottomSheet
import piuk.blockchain.android.ui.customviews.VerifyIdentityNumericBenefitItem
import piuk.blockchain.android.ui.dashboard.sheets.KycUpgradeNowSheet
import piuk.blockchain.android.ui.kyc.navhost.KycNavHostActivity
import piuk.blockchain.android.ui.resources.AssetResources
import piuk.blockchain.android.ui.transactionflow.analytics.SwapAnalyticsEvents
import piuk.blockchain.android.ui.transactionflow.analytics.TxFlowAnalyticsAccountType
import piuk.blockchain.android.ui.transactionflow.flow.TransactionFlowActivity
import piuk.blockchain.android.urllinks.URL_RUSSIA_SANCTIONS_EU5
import piuk.blockchain.android.urllinks.URL_RUSSIA_SANCTIONS_EU8
import piuk.blockchain.android.util.openUrl
import retrofit2.HttpException

class SwapFragment :
    Fragment(),
    KycBenefitsBottomSheet.Host,
    KycUpgradeNowSheet.Host {

    interface Host {
        fun navigateBack()
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
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentSwapBinding.inflate(inflater, container, false)
        return binding.root
    }

    private val kycService: KycService by scopedInject()
    private val coincore: Coincore by scopedInject()
    private val exchangeRateDataManager: ExchangeRatesDataManager by scopedInject()
    private val trendingPairsProvider: TrendingPairsProvider by scopedInject()
    private val walletManager: CustodialWalletManager by scopedInject()
    private val userIdentity: UserIdentity by scopedInject()

    private val currencyPrefs: CurrencyPrefs by inject()
    private val walletPrefs: WalletStatusPrefs by inject()
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

        binding.swapCta.apply {
            analytics.logEvent(SwapAnalyticsEvents.NewSwapClicked)
            setOnClickListener {
                startSwap()
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
        TransactionFlowActivity.newIntent(
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
    }

    @SuppressLint("MissingSuperCall")
    override fun onSheetClosed(sheet: BottomSheetDialogFragment) {
        when (sheet) {
            is KycBenefitsBottomSheet -> walletPrefs.setSeenSwapPromo()
            is KycUpgradeNowSheet -> host.navigateBack()
            is BlockedDueToSanctionsSheet -> host.navigateBack()
        }
    }

    override fun startKycClicked() {
        KycNavHostActivity.start(requireContext(), CampaignType.Swap)
    }

    private fun loadSwapOrKyc(showLoading: Boolean) {
        compositeDisposable +=
            Single.zip(
                kycService.getTiersLegacy(),
                trendingPairsProvider.getTrendingPairs(),
                walletManager.getProductTransferLimits(currencyPrefs.selectedFiatCurrency, Product.TRADE),
                walletManager.getSwapTrades().onErrorReturn { emptyList() },
                coincore.walletsWithActions(setOf(AssetAction.Swap))
                    .map { it.isNotEmpty() },
                userIdentity.userAccessForFeature(Feature.Swap)
            ) { tiers: KycTiers,
                pairs: List<TrendingPair>,
                limits: TransferLimits,
                orders: List<CustodialOrder>,
                hasAtLeastOneAccountToSwapFrom,
                eligibility ->
                SwapComposite(
                    tiers,
                    pairs,
                    limits,
                    orders,
                    hasAtLeastOneAccountToSwapFrom,
                    eligibility
                )
            }
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { if (showLoading) showLoading() }
                .doOnTerminate { hideLoading() }
                .subscribeBy(
                    onSuccess = { composite ->
                        showSwapUi(composite.orders, composite.hasAtLeastOneAccountToSwapFrom)

                        if (composite.tiers.isVerified()) {
                            binding.swapViewFlipper.displayedChild = when {
                                composite.hasAtLeastOneAccountToSwapFrom -> SWAP_VIEW
                                else -> SWAP_NO_ACCOUNTS
                            }
                            binding.swapHeader.toggleBottomSeparator(false)

                            val onPairClicked = onTrendingPairClicked()

                            binding.swapTrending.initialise(
                                pairs = composite.pairs,
                                onSwapPairClicked = onPairClicked,
                                assetResources = assetResources
                            )

                            val eligibility = composite.eligibility
                            if (eligibility is FeatureAccess.Blocked) {
                                when (val reason = eligibility.reason) {
                                    is BlockedReason.NotEligible -> showBlockedDueToNotEligible(reason)
                                    is BlockedReason.InsufficientTier -> showKycUpgradeNow()
                                    is BlockedReason.Sanctions -> showBlockedDueToSanctions(reason)
                                    is BlockedReason.TooManyInFlightTransactions -> { // noop
                                    }
                                }.exhaustive
                            } else if (!composite.tiers.isInitialisedFor(KycTier.GOLD)) {
                                showKycUpsellIfEligible(composite.limits)
                            }
                        } else {
                            binding.swapViewFlipper.displayedChild = KYC_VIEW
                            initKycView()
                        }
                    },
                    onError = { exception ->
                        showErrorUi()
                        val nabuException: NabuApiException? = (exception as? HttpException)?.let { httpException ->
                            NabuApiExceptionFactory.fromResponseBody(httpException)
                        }

                        analytics.logEvent(
                            ClientErrorAnalytics.ClientLogError(
                                nabuApiException = nabuException,
                                errorDescription = exception.message,
                                title = getString(R.string.transfer_wallets_load_error),
                                source = if (exception is HttpException) {
                                    ClientErrorAnalytics.Companion.Source.NABU
                                } else {
                                    ClientErrorAnalytics.Companion.Source.CLIENT
                                },
                                error = if (exception is HttpException) {
                                    ClientErrorAnalytics.NABU_ERROR
                                } else ClientErrorAnalytics.OOPS_ERROR,
                                action = ACTION_SWAP,
                                categories = nabuException?.getServerSideErrorInfo()?.categories ?: emptyList()
                            )
                        )
                        BlockchainSnackbar.make(
                            binding.root, getString(R.string.transfer_wallets_load_error), type = SnackbarType.Error
                        ).show()
                    }
                )
    }

    private fun showKycUpgradeNow() {
        showBottomSheet(KycUpgradeNowSheet.newInstance())
    }

    private fun showBlockedDueToNotEligible(reason: BlockedReason.NotEligible) {
        binding.swapViewFlipper.gone()
        binding.swapError.apply {
            title = R.string.account_restricted
            descriptionText = if (reason.message != null) {
                reason.message
            } else {
                getString(R.string.feature_not_available)
            }
            icon = R.drawable.ic_wallet_intro_image
            ctaText = R.string.contact_support
            ctaAction = { startActivity(SupportCentreActivity.newIntent(requireContext())) }
            visible()
        }
    }

    private fun showBlockedDueToSanctions(reason: BlockedReason.Sanctions) {
        val action = {
            when (reason) {
                is BlockedReason.Sanctions.RussiaEU5 -> requireContext().openUrl(URL_RUSSIA_SANCTIONS_EU5)
                is BlockedReason.Sanctions.RussiaEU8 -> requireContext().openUrl(URL_RUSSIA_SANCTIONS_EU8)
                is BlockedReason.Sanctions.Unknown -> {}
            }
        }

        binding.swapViewFlipper.gone()
        binding.swapError.apply {
            title = R.string.account_restricted
            descriptionText = reason.message
            icon = R.drawable.ic_wallet_intro_image
            ctaText = R.string.common_learn_more
            ctaAction = action
            visible()
        }
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

    private fun showBottomSheet(fragment: BottomSheetDialogFragment) {
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
            TransactionFlowActivity.newIntent(
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
        binding.swapViewFlipper.gone()
        binding.swapError.apply {
            title = R.string.common_empty_title
            description = R.string.common_empty_details
            icon = R.drawable.ic_wallet_intro_image
            ctaText = R.string.common_empty_cta
            ctaAction = { loadSwapOrKyc(true) }
            visible()
        }
    }

    private fun showSwapUi(orders: List<CustodialOrder>, hasAtLeastOneAccountToSwapFrom: Boolean) {
        val pendingOrders = orders.filter { it.state.isPending }
        val hasPendingOrder = pendingOrders.isNotEmpty()

        with(binding) {
            swapViewFlipper.visible()
            swapError.gone()
            swapTrending.visibleIf { !hasPendingOrder }

            with(swapCta) {
                visible()
                isEnabled = hasAtLeastOneAccountToSwapFrom
            }

            with(pendingSwaps) {
                container.visibleIf { hasPendingOrder }
                pendingList.apply {
                    adapter =
                        PendingSwapsAdapter(
                            pendingOrders
                        ) { money: Money ->
                            money.toUserFiat(exchangeRateDataManager)
                        }
                    layoutManager = LinearLayoutManager(activity)
                }
            }
        }
    }

    private fun showLoading() {
        with(binding) {
            with(progress) {
                visible()
                playAnimation()
            }
            swapViewFlipper.gone()
            swapError.gone()
        }
    }

    private fun hideLoading() {
        with(binding.progress) {
            gone()
            pauseAnimation()
        }
    }

    companion object {
        private const val KYC_UPSELL_PERCENTAGE = 90
        private const val SWAP_VIEW = 0
        private const val SWAP_NO_ACCOUNTS = 1
        private const val KYC_VIEW = 2
        private const val TAG = "BOTTOM_SHEET"
        fun newInstance(): SwapFragment = SwapFragment()
    }

    private data class SwapComposite(
        val tiers: KycTiers,
        val pairs: List<TrendingPair>,
        val limits: TransferLimits,
        val orders: List<CustodialOrder>,
        val hasAtLeastOneAccountToSwapFrom: Boolean,
        val eligibility: FeatureAccess,
    )

    override fun onDestroyView() {
        compositeDisposable.clear()
        _binding = null
        super.onDestroyView()
    }
}
