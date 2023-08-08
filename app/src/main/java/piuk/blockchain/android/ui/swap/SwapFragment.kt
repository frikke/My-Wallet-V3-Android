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
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.icons.Network
import com.blockchain.componentlib.icons.User
import com.blockchain.componentlib.utils.openUrl
import com.blockchain.componentlib.viewextensions.gone
import com.blockchain.componentlib.viewextensions.visible
import com.blockchain.componentlib.viewextensions.visibleIf
import com.blockchain.core.kyc.domain.KycService
import com.blockchain.core.kyc.domain.model.KycTiers
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.earn.TxFlowAnalyticsAccountType
import com.blockchain.fiatActions.fiatactions.KycBenefitsSheetHost
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
import com.blockchain.presentation.customviews.kyc.KycUpgradeNowSheet
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
import piuk.blockchain.android.databinding.FragmentSwapBinding
import piuk.blockchain.android.simplebuy.ClientErrorAnalytics
import piuk.blockchain.android.simplebuy.ClientErrorAnalytics.Companion.ACTION_SWAP
import piuk.blockchain.android.support.SupportCentreActivity
import piuk.blockchain.android.ui.customviews.BlockedDueToSanctionsSheet
import piuk.blockchain.android.ui.customviews.ButtonOptions
import piuk.blockchain.android.ui.customviews.KycBenefitsBottomSheet
import piuk.blockchain.android.ui.customviews.VerifyIdentityNumericBenefitItem
import piuk.blockchain.android.ui.kyc.navhost.KycNavHostActivity
import piuk.blockchain.android.ui.kyc.navhost.models.KycEntryPoint
import piuk.blockchain.android.ui.resources.AssetResources
import piuk.blockchain.android.ui.transactionflow.analytics.SwapAnalyticsEvents
import piuk.blockchain.android.ui.transactionflow.flow.TransactionFlowActivity
import piuk.blockchain.android.urllinks.URL_RUSSIA_SANCTIONS_EU5
import piuk.blockchain.android.urllinks.URL_RUSSIA_SANCTIONS_EU8
import retrofit2.HttpException

// todo cleanup + new swap navigation - it's going through this (the old swap) to just end up opening the new one
// (loadSwapOrKyc) make sure kyc is still handled correctly
class SwapFragment :
    Fragment(),
    KycBenefitsSheetHost,
    KycUpgradeNowSheet.Host {

    interface Host {
        fun navigateBack()
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
            text = getString(com.blockchain.stringResources.R.string.swap_cta)
            analytics.logEvent(SwapAnalyticsEvents.NewSwapClicked)
            onClick = {
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
//            host.navigateToReceive()
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
        KycNavHostActivity.start(requireActivity(), KycEntryPoint.Swap)
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
        KycNavHostActivity.start(requireContext(), KycEntryPoint.Swap)
    }

    private fun loadSwapOrKyc(showLoading: Boolean) {
        compositeDisposable +=
            Single.zip(
                kycService.getTiersLegacy(),
                trendingPairsProvider.getTrendingPairs(),
                walletManager.getProductTransferLimits(currencyPrefs.selectedFiatCurrency, Product.TRADE),
                walletManager.getSwapTrades().onErrorReturn { emptyList() },
                coincore.walletsWithAction(action = AssetAction.Swap)
                    .map { it.isNotEmpty() },
                userIdentity.userAccessForFeature(Feature.Swap),
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
                        if (composite.tiers.isVerified()) {
                            startSwap()
                            requireActivity().finish()
                        } else {
                            showSwapUi(composite.orders, composite.hasAtLeastOneAccountToSwapFrom)
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
                                title = getString(com.blockchain.stringResources.R.string.transfer_wallets_load_error),
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
                            binding.root,
                            getString(com.blockchain.stringResources.R.string.transfer_wallets_load_error),
                            type = SnackbarType.Error
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
            title = com.blockchain.stringResources.R.string.account_restricted
            descriptionText = if (reason.message != null) {
                reason.message
            } else {
                getString(com.blockchain.stringResources.R.string.feature_not_available)
            }
            icon = Icons.Filled.User
            ctaText = com.blockchain.stringResources.R.string.contact_support
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
            title = com.blockchain.stringResources.R.string.account_restricted
            descriptionText = reason.message
            icon = Icons.Filled.User
            ctaText = com.blockchain.stringResources.R.string.common_learn_more
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
                    title = getString(com.blockchain.stringResources.R.string.swap_kyc_upsell_title),
                    description = getString(com.blockchain.stringResources.R.string.swap_kyc_upsell_desc),
                    listOfBenefits = listOf(
                        VerifyIdentityNumericBenefitItem(
                            getString(com.blockchain.stringResources.R.string.swap_kyc_upsell_1_title),
                            getString(com.blockchain.stringResources.R.string.swap_kyc_upsell_1_desc)
                        ),
                        VerifyIdentityNumericBenefitItem(
                            getString(com.blockchain.stringResources.R.string.swap_kyc_upsell_2_title),
                            getString(com.blockchain.stringResources.R.string.swap_kyc_upsell_2_desc)
                        ),
                        VerifyIdentityNumericBenefitItem(
                            getString(com.blockchain.stringResources.R.string.swap_kyc_upsell_3_title),
                            getString(com.blockchain.stringResources.R.string.swap_kyc_upsell_3_desc)
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
                    getString(com.blockchain.stringResources.R.string.swap_kyc_1_title),
                    getString(com.blockchain.stringResources.R.string.swap_kyc_1_label)
                ),
                VerifyIdentityNumericBenefitItem(
                    getString(com.blockchain.stringResources.R.string.swap_kyc_2_title),
                    getString(com.blockchain.stringResources.R.string.swap_kyc_2_label)
                ),
                VerifyIdentityNumericBenefitItem(
                    getString(com.blockchain.stringResources.R.string.swap_kyc_3_title),
                    getString(com.blockchain.stringResources.R.string.swap_kyc_3_label)
                )
            ),
            getString(com.blockchain.stringResources.R.string.swap_kyc_title),
            getString(com.blockchain.stringResources.R.string.swap_kyc_desc),
            R.drawable.ic_swap_blue_circle,
            ButtonOptions(visible = true, text = getString(com.blockchain.stringResources.R.string.swap_kyc_cta)) {
                analytics.logEvent(SwapAnalyticsEvents.VerifyNowClicked)
                startKycForResult.launch(KycNavHostActivity.newIntent(requireActivity(), KycEntryPoint.Swap))
            },
            ButtonOptions(visible = false),
            showSheetIndicator = false
        )
    }

    private fun showErrorUi() {
        Icons.Filled.User
        binding.swapViewFlipper.gone()
        binding.swapError.apply {
            title = com.blockchain.stringResources.R.string.common_empty_title
            description = com.blockchain.stringResources.R.string.common_empty_details
            icon = Icons.Filled.Network
            ctaText = com.blockchain.stringResources.R.string.common_empty_cta
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
