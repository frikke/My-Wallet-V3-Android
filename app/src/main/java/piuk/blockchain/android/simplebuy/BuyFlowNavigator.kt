package piuk.blockchain.android.simplebuy

import com.blockchain.domain.fiatcurrencies.FiatCurrenciesService
import com.blockchain.nabu.BlockedReason
import com.blockchain.nabu.Feature
import com.blockchain.nabu.FeatureAccess
import com.blockchain.nabu.Tier
import com.blockchain.nabu.UserIdentity
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.outcome.map
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.FiatCurrency
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.Singles
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.rx3.asCoroutineDispatcher
import piuk.blockchain.androidcore.utils.extensions.rxSingleOutcome

class BuyFlowNavigator(
    private val simpleBuySyncFactory: SimpleBuySyncFactory,
    private val userIdentity: UserIdentity,
    private val fiatCurrenciesService: FiatCurrenciesService,
    private val custodialWalletManager: CustodialWalletManager,
) {

    val currentState: SimpleBuyState
        get() = simpleBuySyncFactory.currentState() ?: SimpleBuyState()

    private fun stateCheck(
        startedFromKycResume: Boolean,
        startedFromNavigationBar: Boolean,
        startedFromApprovalDeepLink: Boolean,
        cryptoCurrency: AssetInfo
    ): Single<BuyNavigation> {
        return if (
            startedFromKycResume ||
            currentState.currentScreen == FlowScreen.KYC ||
            currentState.currentScreen == FlowScreen.KYC_VERIFICATION
        ) {

            Single.zip(
                userIdentity.isVerifiedFor(Feature.TierLevel(Tier.GOLD)),
                userIdentity.isKycInProgress(),
                userIdentity.isRejectedForTier(Feature.TierLevel(Tier.GOLD))
            ) { verifiedGold, kycInProgress, rejectedGold ->
                when {
                    verifiedGold -> BuyNavigation.FlowScreenWithCurrency(
                        FlowScreen.ENTER_AMOUNT, cryptoCurrency
                    )
                    kycInProgress || rejectedGold -> BuyNavigation.FlowScreenWithCurrency(
                        FlowScreen.KYC_VERIFICATION, cryptoCurrency
                    )
                    else -> BuyNavigation.FlowScreenWithCurrency(FlowScreen.KYC, cryptoCurrency)
                }
            }
        } else {
            when {
                startedFromApprovalDeepLink -> {
                    Single.just(BuyNavigation.OrderInProgressScreen)
                }
                startedFromNavigationBar -> {
                    Single.just(
                        BuyNavigation.FlowScreenWithCurrency(FlowScreen.ENTER_AMOUNT, cryptoCurrency)
                    )
                }
                else -> {
                    Single.just(
                        BuyNavigation.FlowScreenWithCurrency(currentState.currentScreen, cryptoCurrency)
                    )
                }
            }
        }
    }

    fun navigateTo(
        startedFromKycResume: Boolean,
        startedFromDashboard: Boolean,
        startedFromApprovalDeepLink: Boolean,
        preselectedCrypto: AssetInfo?,
        failOnUnavailableCurrency: Boolean = false,
    ): Single<BuyNavigation> {
        val cryptoCurrency = preselectedCrypto
            ?: currentState.selectedCryptoAsset ?: throw IllegalStateException("CryptoCurrency is not available")

        return Singles.zip(
            allowedBuyFiatCurrencies(cryptoCurrency),
            userIdentity.userAccessForFeature(Feature.Buy)
        ).flatMap { (allowedBuyFiatCurrencies, eligibility) ->
            val canBuyWithSelectedTradingCurrency =
                allowedBuyFiatCurrencies.contains(fiatCurrenciesService.selectedTradingCurrency)

            if (eligibility is FeatureAccess.Blocked) {
                Single.just(BuyNavigation.BlockBuy(eligibility.reason))
            } else if (!canBuyWithSelectedTradingCurrency) {
                if (allowedBuyFiatCurrencies.isNotEmpty() && !failOnUnavailableCurrency) {
                    Single.just(
                        BuyNavigation.CurrencySelection(
                            allowedBuyFiatCurrencies,
                            fiatCurrenciesService.selectedTradingCurrency
                        )
                    )
                } else {
                    Single.just(BuyNavigation.CurrencyNotAvailable)
                }
            } else {
                stateCheck(
                    startedFromKycResume,
                    startedFromDashboard,
                    startedFromApprovalDeepLink,
                    cryptoCurrency
                )
            }
        }
    }

    private fun allowedBuyFiatCurrencies(asset: AssetInfo): Single<List<FiatCurrency>> =
        Single.zip(
            custodialWalletManager.availableFiatCurrenciesForTrading(asset),
            rxSingleOutcome(Schedulers.io().asCoroutineDispatcher()) {
                fiatCurrenciesService.getTradingCurrencies().map { it.allAvailable }
            }
        ) { availableFiatCurrenciesForTrading, availableTradingCurrencies ->
            availableTradingCurrencies.intersect(availableFiatCurrenciesForTrading.toSet()).toList()
        }
}

sealed class BuyNavigation {
    data class CurrencySelection(val currencies: List<FiatCurrency>, val selectedCurrency: FiatCurrency) :
        BuyNavigation()

    data class FlowScreenWithCurrency(val flowScreen: FlowScreen, val cryptoCurrency: AssetInfo) : BuyNavigation()
    data class BlockBuy(val reason: BlockedReason) : BuyNavigation()
    object CurrencyNotAvailable : BuyNavigation()
    object OrderInProgressScreen : BuyNavigation()
}
