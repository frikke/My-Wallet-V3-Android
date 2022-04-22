package piuk.blockchain.android.simplebuy

import com.blockchain.featureflag.FeatureFlag
import com.blockchain.nabu.Feature
import com.blockchain.nabu.FeatureAccess
import com.blockchain.nabu.Tier
import com.blockchain.nabu.UserIdentity
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.preferences.CurrencyPrefs
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.FiatCurrency
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.Singles

class BuyFlowNavigator(
    private val simpleBuySyncFactory: SimpleBuySyncFactory,
    private val userIdentity: UserIdentity,
    private val currencyPrefs: CurrencyPrefs,
    private val custodialWalletManager: CustodialWalletManager,
    private val entitySwitchSilverEligibilityFeatureFlag: FeatureFlag
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
        failOnUnavailableCurrency: Boolean = false
    ): Single<BuyNavigation> {
        val cryptoCurrency = preselectedCrypto
            ?: currentState.selectedCryptoAsset ?: throw IllegalStateException("CryptoCurrency is not available")

        return entitySwitchSilverEligibilityFeatureFlag.enabled
            .flatMap { enabled ->
                if (enabled) {
                    Singles.zip(
                        currencyCheck(),
                        userIdentity.userAccessForFeature(Feature.Buy)
                    ).flatMap { (currencySupported, eligibility) ->
                        if (eligibility is FeatureAccess.Blocked) {
                            Single.just(BuyNavigation.TransactionsLimitReached)
                        } else if (!currencySupported) {
                            if (!failOnUnavailableCurrency) {
                                custodialWalletManager.getSupportedFiatCurrencies().map {
                                    BuyNavigation.CurrencySelection(it, currencyPrefs.selectedFiatCurrency)
                                }
                            } else {
                                Single.just(BuyNavigation.CurrencyNotAvailable)
                            }
                        } else {
                            checkForEligibilityOrPendingOrders().switchIfEmpty(
                                stateCheck(
                                    startedFromKycResume,
                                    startedFromDashboard,
                                    startedFromApprovalDeepLink,
                                    cryptoCurrency
                                )
                            )
                        }
                    }
                } else {
                    currencyCheck().flatMap { currencySupported ->
                        if (!currencySupported) {
                            if (!failOnUnavailableCurrency) {
                                custodialWalletManager.getSupportedFiatCurrencies().map {
                                    BuyNavigation.CurrencySelection(it, currencyPrefs.selectedFiatCurrency)
                                }
                            } else {
                                Single.just(BuyNavigation.CurrencyNotAvailable)
                            }
                        } else {
                            checkForEligibilityOrPendingOrders().switchIfEmpty(
                                stateCheck(
                                    startedFromKycResume,
                                    startedFromDashboard,
                                    startedFromApprovalDeepLink,
                                    cryptoCurrency
                                )
                            )
                        }
                    }
                }
            }
    }

    private fun checkForEligibilityOrPendingOrders(): Maybe<BuyNavigation> =
        userIdentity.userAccessForFeature(Feature.SimpleBuy).flatMapMaybe { access ->
            when (access) {
                FeatureAccess.NotRequested,
                FeatureAccess.Unknown,
                is FeatureAccess.Granted -> Maybe.empty()
                is FeatureAccess.Blocked -> Maybe.just(BuyNavigation.BlockBuy(access))
            }
        }

    private fun currencyCheck(): Single<Boolean> {
        return custodialWalletManager.isCurrencySupportedForSimpleBuy(currencyPrefs.tradingCurrency)
    }
}

sealed class BuyNavigation {
    data class CurrencySelection(val currencies: List<FiatCurrency>, val selectedCurrency: FiatCurrency) :
        BuyNavigation()

    data class FlowScreenWithCurrency(val flowScreen: FlowScreen, val cryptoCurrency: AssetInfo) : BuyNavigation()
    data class BlockBuy(val access: FeatureAccess.Blocked) : BuyNavigation()
    object CurrencyNotAvailable : BuyNavigation()
    object OrderInProgressScreen : BuyNavigation()
    object TransactionsLimitReached : BuyNavigation()
}
