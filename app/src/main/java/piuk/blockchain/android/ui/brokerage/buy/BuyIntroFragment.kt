package piuk.blockchain.android.ui.brokerage.buy

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import com.blockchain.analytics.Analytics
import com.blockchain.api.NabuApiException
import com.blockchain.api.NabuApiExceptionFactory
import com.blockchain.commonarch.presentation.base.trackProgress
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
import piuk.blockchain.android.simplebuy.ClientErrorAnalytics
import piuk.blockchain.android.simplebuy.SimpleBuyActivity
import piuk.blockchain.android.simplebuy.sheets.BuyPendingOrdersBottomSheet
import piuk.blockchain.android.support.SupportCentreActivity
import piuk.blockchain.android.ui.base.ViewPagerFragment
import piuk.blockchain.android.ui.kyc.navhost.KycNavHostActivity
import piuk.blockchain.android.ui.transfer.BuyListAccountSorting
import piuk.blockchain.android.urllinks.URL_RUSSIA_SANCTIONS_EU5
import piuk.blockchain.android.urllinks.URL_RUSSIA_SANCTIONS_EU8
import retrofit2.HttpException

class BuyIntroFragment :
    ViewPagerFragment(),
    KycUpgradeNowSheet.Host {

    private var isInitialLoading = true

    private val custodialWalletManager: CustodialWalletManager by scopedInject()
    private val compositeDisposable = CompositeDisposable()
    private val analytics: Analytics by inject()
    private val userIdentity: UserIdentity by scopedInject()
    private val buyOrdering: BuyListAccountSorting by scopedInject(buyOrder)
    private var purchaseableAssets = listOf<BuyCryptoItem>()
    private var buyViewState by mutableStateOf<BuyViewState>(BuyViewState.Loading)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                BuyIntroScreen(
                    buyViewState = buyViewState,
                    onSearchValueUpdated = { searchedText ->
                        when {
                            searchedText.isEmpty() -> updateList(purchaseableAssets)
                            else -> updateList(
                                purchaseableAssets.filter {
                                    it.asset.networkTicker.contains(searchedText, true) ||
                                        it.asset.name.contains(searchedText, true)
                                }
                            )
                        }
                    },
                    onListItemClicked = { item -> onItemClick(item) },
                    onEmptyStateClicked = { reason ->
                        when (reason) {
                            is BlockedReason.Sanctions.RussiaEU5 -> requireContext().openUrl(URL_RUSSIA_SANCTIONS_EU5)
                            is BlockedReason.Sanctions.RussiaEU8 -> requireContext().openUrl(URL_RUSSIA_SANCTIONS_EU8)
                            is BlockedReason.NotEligible -> startActivity(
                                SupportCentreActivity.newIntent(requireContext())
                            )
                            else -> {
                                checkEligibilityAndLoadBuyDetails(false)
                            }
                        }
                    },
                    onErrorRetryClicked = {
                        checkEligibilityAndLoadBuyDetails()
                    },
                    onErrorContactSupportClicked = {
                        SupportCentreActivity.newIntent(requireContext())
                    },
                    fragmentManager = childFragmentManager
                )
            }
        }
    }

    override fun onResume() {
        if (purchaseableAssets.isNotEmpty()) {
            buyViewState = BuyViewState.ShowAssetList(purchaseableAssets)
        }
        checkEligibilityAndLoadBuyDetails(isInitialLoading)
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

                        isInitialLoading = false
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
        if (isInitialLoading || purchaseableAssets.isEmpty()) {
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
        } else {
            buyViewState = BuyViewState.ShowAssetList(purchaseableAssets)
        }
    }

    private fun onItemClick(item: BuyCryptoItem) {
        compositeDisposable += userIdentity.userAccessForFeature(Feature.Buy).subscribeBy { accessState ->
            val blockedReason = (accessState as? FeatureAccess.Blocked)?.reason
            if (blockedReason is BlockedReason.TooManyInFlightTransactions) {
                showPendingOrdersBottomSheet(blockedReason.maxTransactions)
            } else {
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
        buyViewState = BuyViewState.ShowKyc
    }

    private fun renderBlockedDueToNotEligible(reason: BlockedReason.NotEligible) {
        buyViewState = BuyViewState.ShowEmptyState(
            reason = reason,
            title = R.string.account_restricted,
            description = reason.message ?: getString(R.string.feature_not_available),
            icon = R.drawable.ic_wallet_intro_image,
            ctaText = R.string.contact_support
        )
    }

    private fun renderBlockedDueToSanctions(reason: BlockedReason.Sanctions) {
        buyViewState = BuyViewState.ShowEmptyState(
            reason = reason,
            title = R.string.account_restricted,
            icon = R.drawable.ic_wallet_intro_image,
            ctaText = R.string.common_learn_more,
            description = reason.message
        )
    }

    private fun renderBuyIntro(
        pricesAssets: List<PricedAsset>
    ) {
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
        buyViewState = BuyViewState.ShowAssetList(items)
    }

    private fun renderErrorState() {
        buyViewState = BuyViewState.ShowError
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
    }

    companion object {
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

sealed class BuyViewState {
    class ShowAssetList(val list: List<BuyCryptoItem>) : BuyViewState()
    object ShowKyc : BuyViewState()
    class ShowEmptyState(
        val reason: BlockedReason,
        @StringRes val title: Int,
        val description: String,
        @StringRes val ctaText: Int,
        @DrawableRes
        val icon: Int
    ) : BuyViewState()

    object ShowError : BuyViewState()

    object Loading : BuyViewState()
}
