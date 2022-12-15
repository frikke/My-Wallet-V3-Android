package piuk.blockchain.android.ui.brokerage.buy

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.blockchain.analytics.Analytics
import com.blockchain.api.NabuApiException
import com.blockchain.api.NabuApiExceptionFactory
import com.blockchain.commonarch.presentation.base.trackProgress
import com.blockchain.componentlib.basic.ComposeGravities
import com.blockchain.componentlib.basic.ComposeTypographies
import com.blockchain.componentlib.viewextensions.gone
import com.blockchain.componentlib.viewextensions.visible
import com.blockchain.home.presentation.navigation.HomeLaunch.KYC_STARTED
import com.blockchain.koin.buyOrder
import com.blockchain.nabu.BlockedReason
import com.blockchain.nabu.Feature
import com.blockchain.nabu.FeatureAccess
import com.blockchain.nabu.UserIdentity
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.presentation.customviews.kyc.KycUpgradeNowSheet
import com.blockchain.presentation.koin.scopedInject
import com.blockchain.presentation.openUrl
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.ExchangeRate
import info.blockchain.balance.Money
import info.blockchain.balance.asAssetInfoOrThrow
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.campaign.CampaignType
import piuk.blockchain.android.databinding.BuyIntroFragmentBinding
import piuk.blockchain.android.simplebuy.ClientErrorAnalytics
import piuk.blockchain.android.simplebuy.SimpleBuyActivity
import piuk.blockchain.android.simplebuy.sheets.BuyPendingOrdersBottomSheet
import piuk.blockchain.android.support.SupportCentreActivity
import piuk.blockchain.android.ui.base.ViewPagerFragment
import piuk.blockchain.android.ui.kyc.navhost.KycNavHostActivity
import piuk.blockchain.android.ui.resources.AssetResources
import piuk.blockchain.android.ui.transfer.BuyListAccountSorting
import piuk.blockchain.android.urllinks.URL_RUSSIA_SANCTIONS_EU5
import piuk.blockchain.android.urllinks.URL_RUSSIA_SANCTIONS_EU8
import retrofit2.HttpException

class BuyIntroFragment :
    ViewPagerFragment(),
    KycUpgradeNowSheet.Host {

    private var _binding: BuyIntroFragmentBinding? = null
    private val binding: BuyIntroFragmentBinding
        get() = _binding!!

    private var isInitialLoading = true

    private val custodialWalletManager: CustodialWalletManager by scopedInject()
    private val compositeDisposable = CompositeDisposable()
    private val assetResources: AssetResources by inject()
    private val analytics: Analytics by inject()
    private val userIdentity: UserIdentity by scopedInject()
    private val buyOrdering: BuyListAccountSorting by scopedInject(buyOrder)
    private var purchaseableAssets = listOf<BuyCryptoItem>()

    private val buyAdapter = BuyCryptoCurrenciesAdapter(
        assetResources = assetResources,
        onItemClick = ::onItemClick
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BuyIntroFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(binding) {
            with(buyAssetList) {
                layoutManager = LinearLayoutManager(activity)
                adapter = buyAdapter
            }
            with(buySearchEmpty) {
                text = getString(R.string.search_empty)
                gravity = ComposeGravities.Centre
                style = ComposeTypographies.Body1
            }
        }
    }

    override fun onResume() {
        checkEligibilityAndLoadBuyDetails(isInitialLoading)
        isInitialLoading = false
        super.onResume()
    }

    private fun checkEligibilityAndLoadBuyDetails(showLoading: Boolean = true) {
        compositeDisposable +=
            userIdentity.userAccessForFeature(Feature.Buy)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .trackProgress(activityIndicator.takeIf { showLoading })
                .subscribeBy(
                    onSuccess = { eligibility ->
                        when (val reason = (eligibility as? FeatureAccess.Blocked)?.reason) {
                            is BlockedReason.NotEligible -> renderBlockedDueToNotEligible(reason)
                            is BlockedReason.InsufficientTier -> renderKycUpgradeNow()
                            is BlockedReason.Sanctions -> renderBlockedDueToSanctions(reason)
                            is BlockedReason.TooManyInFlightTransactions,
                            is BlockedReason.ShouldAcknowledgeStakingWithdrawal,
                            null -> loadBuyDetails(showLoading)
                        }
                    },
                    onError = { exception ->
                        renderErrorState()

                        val nabuException: NabuApiException? = (exception as? HttpException)?.let { httpException ->
                            NabuApiExceptionFactory.fromResponseBody(httpException)
                        }

                        analytics.logEvent(
                            ClientErrorAnalytics.ClientLogError(
                                nabuApiException = nabuException,
                                errorDescription = exception.message,
                                error = ClientErrorAnalytics.NABU_ERROR,
                                source = if (exception is HttpException) {
                                    ClientErrorAnalytics.Companion.Source.NABU
                                } else {
                                    ClientErrorAnalytics.Companion.Source.CLIENT
                                },
                                title = ClientErrorAnalytics.OOPS_ERROR,
                                action = ClientErrorAnalytics.ACTION_BUY,
                                categories = nabuException?.getServerSideErrorInfo()?.categories ?: emptyList()
                            )
                        )
                    }
                )
    }

    private fun loadBuyDetails(showLoading: Boolean = true) {
        compositeDisposable += custodialWalletManager.getSupportedBuySellCryptoCurrencies()
            .map { pairs ->
                pairs.map {
                    it.source
                }.distinct()
                    .filterIsInstance<AssetInfo>()
            }.flatMap { assets ->
                buyOrdering.sort(assets)
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .trackProgress(activityIndicator.takeIf { showLoading })
            .subscribeBy(
                onSuccess = { items: List<PricedAsset> ->
                    renderBuyIntro(items)
                },
                onError = { renderErrorState() },
            )
    }

    private fun onItemClick(item: BuyCryptoItem) {
        compositeDisposable += userIdentity.userAccessForFeature(Feature.Buy).subscribeBy { accessState ->
            val blockedReason = (accessState as? FeatureAccess.Blocked)?.reason
            if (blockedReason is BlockedReason.TooManyInFlightTransactions) {
                showPendingOrdersBottomSheet(blockedReason.maxTransactions)
            } else {
                binding.buyIntroSearch.clearInput()

                startActivity(
                    SimpleBuyActivity.newIntent(
                        activity as Context,
                        item.asset,
                        launchFromNavigationBar = true,
                        launchKycResume = false
                    )
                )
            }
        }
    }

    private fun showPendingOrdersBottomSheet(maxTransactions: Int) {
        BuyPendingOrdersBottomSheet.newInstance(maxTransactions).show(
            childFragmentManager,
            BuyPendingOrdersBottomSheet.TAG
        )
    }

    private fun renderKycUpgradeNow() {
        if (childFragmentManager.findFragmentById(R.id.fragment_container) == null) {
            childFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, KycUpgradeNowSheet.newInstance())
                .commitAllowingStateLoss()
        }
        binding.viewFlipper.displayedChild = ViewFlipperItem.KYC.ordinal
    }

    private fun renderBlockedDueToNotEligible(reason: BlockedReason.NotEligible) {
        with(binding) {
            viewFlipper.displayedChild = ViewFlipperItem.EMPTY_STATE.ordinal
            customEmptyState.apply {
                title = R.string.account_restricted
                descriptionText = if (reason.message != null) {
                    reason.message
                } else {
                    getString(R.string.feature_not_available)
                }
                icon = R.drawable.ic_wallet_intro_image
                ctaText = R.string.contact_support
                ctaAction = { startActivity(SupportCentreActivity.newIntent(requireContext())) }
            }
        }
    }

    private fun renderBlockedDueToSanctions(reason: BlockedReason.Sanctions) {
        val action = {
            when (reason) {
                is BlockedReason.Sanctions.RussiaEU5 -> requireContext().openUrl(URL_RUSSIA_SANCTIONS_EU5)
                is BlockedReason.Sanctions.RussiaEU8 -> requireContext().openUrl(URL_RUSSIA_SANCTIONS_EU8)
                is BlockedReason.Sanctions.Unknown -> {}
            }
        }
        with(binding) {
            viewFlipper.displayedChild = ViewFlipperItem.EMPTY_STATE.ordinal
            customEmptyState.apply {
                title = R.string.account_restricted
                descriptionText = reason.message
                icon = R.drawable.ic_wallet_intro_image
                ctaText = R.string.common_learn_more
                ctaAction = action
            }
        }
    }

    private fun renderBuyIntro(
        pricesAssets: List<PricedAsset>
    ) {
        with(binding) {
            viewFlipper.displayedChild = ViewFlipperItem.INTRO.ordinal

            buyIntroSearch.apply {
                label = getString(R.string.search_coins_hint)
                onValueChange = { searchedText ->
                    when {
                        searchedText.isEmpty() -> updateList(purchaseableAssets)
                        else -> updateList(
                            purchaseableAssets.filter {
                                it.asset.networkTicker.contains(searchedText, true) ||
                                    it.asset.name.contains(searchedText, true)
                            }
                        )
                    }
                }
            }
        }

        purchaseableAssets = pricesAssets.map { pricedAsset ->
            BuyCryptoItem(
                asset = pricedAsset.asset,
                price = pricedAsset.priceHistory
                    .currentExchangeRate
                    .price,
                percentageDelta = pricedAsset.priceHistory.percentageDelta
            )
        }

        updateList(purchaseableAssets)
    }

    private fun updateList(items: List<BuyCryptoItem>) {
        with(binding) {
            if (items.isNotEmpty()) {
                buySearchEmpty.gone()
                buyAdapter.items = items
                with(buyAssetList) {
                    visible()
                    smoothScrollToPosition(0)
                }
            } else {
                buySearchEmpty.visible()
                buyAssetList.gone()
            }
        }
    }

    private fun renderErrorState() {
        with(binding) {
            viewFlipper.displayedChild = ViewFlipperItem.ERROR.ordinal
            buyEmpty.setDetails(
                action = {
                    checkEligibilityAndLoadBuyDetails()
                },
                onContactSupport = { requireContext().startActivity(SupportCentreActivity.newIntent(requireContext())) }
            )
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == KYC_STARTED) {
            checkEligibilityAndLoadBuyDetails(true)
        }
    }

    override fun startKycClicked() {
        KycNavHostActivity.startForResult(this, CampaignType.SimpleBuy, KYC_STARTED)
    }

    override fun onDestroyView() {
        compositeDisposable.clear()
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private enum class ViewFlipperItem {
            INTRO,
            ERROR,
            KYC,
            EMPTY_STATE
        }

        fun newInstance() = BuyIntroFragment()
    }

    override fun onSheetClosed() {
        // do nothing
    }
}

data class PriceHistory(
    val currentExchangeRate: ExchangeRate,
    val priceDelta: Double
) {
    val cryptoCurrency: AssetInfo
        get() = currentExchangeRate.from.asAssetInfoOrThrow()

    val percentageDelta: Double
        get() = priceDelta
}

data class BuyCryptoItem(
    val asset: AssetInfo,
    val price: Money,
    val percentageDelta: Double
)

sealed class PricedAsset(
    open val asset: AssetInfo,
    open val priceHistory: PriceHistory
) {
    data class NonSortedAsset(
        override val asset: AssetInfo,
        override val priceHistory: PriceHistory
    ) : PricedAsset(asset, priceHistory)

    data class SortedAsset(
        override val asset: AssetInfo,
        override val priceHistory: PriceHistory,
        val balance: Money,
        val tradingVolume: Double
    ) : PricedAsset(asset, priceHistory)
}
