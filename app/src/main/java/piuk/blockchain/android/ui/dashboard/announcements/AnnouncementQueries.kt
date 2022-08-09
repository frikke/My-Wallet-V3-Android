package piuk.blockchain.android.ui.dashboard.announcements

import androidx.annotation.VisibleForTesting
import com.blockchain.api.paymentmethods.models.PaymentMethodResponse
import com.blockchain.api.services.PaymentMethodsService
import com.blockchain.auth.AuthHeaderProvider
import com.blockchain.coincore.Coincore
import com.blockchain.coincore.FiatAccount
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.domain.fiatcurrencies.FiatCurrenciesService
import com.blockchain.domain.paymentmethods.model.PaymentMethod
import com.blockchain.featureflag.FeatureFlag
import com.blockchain.nabu.Feature
import com.blockchain.nabu.UserIdentity
import com.blockchain.nabu.api.getuser.domain.UserService
import com.blockchain.nabu.api.kyc.domain.KycService
import com.blockchain.nabu.api.kyc.domain.model.KycTierLevel
import com.blockchain.nabu.api.kyc.domain.model.KycTiers
import com.blockchain.payments.googlepay.manager.GooglePayManager
import com.blockchain.payments.googlepay.manager.request.GooglePayRequestBuilder
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.remoteconfig.RemoteConfig
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.Currency
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.zipWith
import kotlinx.coroutines.rx3.rxSingle
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import piuk.blockchain.android.simplebuy.SimpleBuySyncFactory

@Serializable
data class RenamedAsset(
    val networkTicker: String,
    val oldTicker: String,
)

class AnnouncementQueries(
    private val userService: UserService,
    private val kycService: KycService,
    private val sbStateFactory: SimpleBuySyncFactory,
    private val userIdentity: UserIdentity,
    private val coincore: Coincore,
    private val remoteConfig: RemoteConfig,
    private val assetCatalogue: AssetCatalogue,
    private val googlePayManager: GooglePayManager,
    private val googlePayEnabledFlag: FeatureFlag,
    private val paymentMethodsService: PaymentMethodsService,
    private val authenticator: AuthHeaderProvider,
    private val fiatCurrenciesService: FiatCurrenciesService,
    private val exchangeRatesDataManager: ExchangeRatesDataManager,
    private val currencyPrefs: CurrencyPrefs
) {
    fun hasFundedFiatWallets(): Single<Boolean> =
        coincore.allWallets().map { it.accounts }.map { it.filterIsInstance<FiatAccount>() }
            .map { it.any { fiatAccount -> fiatAccount.isFunded } }

    // Have we moved past kyc tier 1 - silver?
    fun isKycGoldStartedOrComplete(): Single<Boolean> {
        return userService.getUser()
            .map { it.tierInProgressOrCurrentTier == 2 }
            .onErrorReturn { false }
    }

    // Have we been through the Gold KYC process? ie are we Tier2InReview, Tier2Approved or Tier2Failed (cf TierJson)
    fun isGoldComplete(): Single<Boolean> =
        kycService.getTiersLegacy()
            .map { it.tierCompletedForLevel(KycTierLevel.GOLD) }

    fun isTier1Or2Verified(): Single<Boolean> =
        kycService.getTiersLegacy().map { it.isVerified() }

    fun isSimplifiedDueDiligenceEligibleAndNotVerified(): Single<Boolean> =
        userIdentity.isEligibleFor(Feature.SimplifiedDueDiligence).flatMap {
            if (!it)
                Single.just(false)
            else
                userIdentity.isVerifiedFor(Feature.SimplifiedDueDiligence).map { verified -> verified.not() }
        }

    fun isSimplifiedDueDiligenceVerified(): Single<Boolean> =
        userIdentity.isVerifiedFor(Feature.SimplifiedDueDiligence)

    fun isSimpleBuyKycInProgress(): Single<Boolean> {
        // If we have a local simple buy in progress and it has the kyc unfinished state set
        return Single.defer {
            sbStateFactory.currentState()?.let {
                Single.just(it.kycStartedButNotCompleted)
                    .zipWith(kycService.getTiersLegacy()) { kycStarted, tier ->
                        kycStarted && !tier.docsSubmittedForGoldTier()
                    }
            } ?: Single.just(false)
        }
    }

    private fun hasSelectedToAddNewCard(): Single<Boolean> =
        Single.defer {
            sbStateFactory.currentState()?.let {
                Single.just(it.selectedPaymentMethod?.id == PaymentMethod.UNDEFINED_CARD_PAYMENT_ID)
            } ?: Single.just(false)
        }

    fun isKycGoldVerifiedAndHasPendingCardToAdd(): Single<Boolean> =
        kycService.getTiersLegacy().map {
            it.isApprovedFor(KycTierLevel.GOLD)
        }.zipWith(
            hasSelectedToAddNewCard()
        ) { isGold, addNewCard ->
            isGold && addNewCard
        }

    fun getAssetFromCatalogue(): Maybe<AssetInfo> =
        remoteConfig.getRawJson(NEW_ASSET_TICKER).flatMapMaybe { ticker ->
            assetCatalogue.assetInfoFromNetworkTicker(ticker)?.let { asset ->
                Maybe.just(asset)
            }
                ?: Maybe.empty()
        }

    fun getAssetFromCatalogueByTicker(ticker: String): AssetInfo? =
        assetCatalogue.assetInfoFromNetworkTicker(ticker)

    fun getCountryCode(): Single<String> = userIdentity.getUserCountry().switchIfEmpty(Single.just(""))

    fun getRenamedAssetFromCatalogue(): Maybe<Pair<String, AssetInfo>> =
        remoteConfig.getRawJson(RENAME_ASSET_TICKER).flatMapMaybe { json ->
            val renamedAsset = Json.decodeFromString<RenamedAsset>(json)
            assetCatalogue.assetInfoFromNetworkTicker(renamedAsset.networkTicker)?.let { asset ->
                Maybe.just(Pair(renamedAsset.oldTicker, asset))
            }
                ?: Maybe.empty()
        }

    fun isGooglePayAvailable(): Single<Boolean> =
        authenticator.getAuthHeader().flatMap { authToken ->
            Single.zip(
                paymentMethodsService.getAvailablePaymentMethodsTypes(
                    authorization = authToken,
                    currency = fiatCurrenciesService.selectedTradingCurrency.networkTicker,
                    tier = null,
                    eligibleOnly = true
                ).map { list ->
                    list.any { response ->
                        response.mobilePayment?.any { payment ->
                            payment.equals(PaymentMethodResponse.GOOGLE_PAY, true)
                        } ?: false
                    }
                },
                googlePayEnabledFlag.enabled,
                checkGooglePayAvailability()
            ) { gPayPaymentMethodAvailable, gPayFlagEnabled, gPayAvailableOnDevice ->
                return@zip gPayPaymentMethodAvailable && gPayFlagEnabled && gPayAvailableOnDevice
            }
        }.map { enabled ->
            return@map enabled
        }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun checkGooglePayAvailability(): Single<Boolean> =
        rxSingle {
            googlePayManager.checkIfGooglePayIsAvailable(GooglePayRequestBuilder.buildForPaymentStatus())
        }

    fun getAssetPrice(asset: Currency) =
        exchangeRatesDataManager.getPricesWith24hDelta(asset, currencyPrefs.selectedFiatCurrency)

    companion object {
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        const val NEW_ASSET_TICKER = "new_asset_announcement_ticker"

        private const val RENAME_ASSET_TICKER = "rename_asset_announcement_ticker"
    }
}

private fun KycTiers.docsSubmittedForGoldTier(): Boolean =
    isInitialisedFor(KycTierLevel.GOLD)
