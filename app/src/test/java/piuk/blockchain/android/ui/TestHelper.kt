package piuk.blockchain.android.ui

import com.blockchain.core.kyc.domain.model.KycLimits
import com.blockchain.core.kyc.domain.model.KycTier
import com.blockchain.core.kyc.domain.model.KycTierDetail
import com.blockchain.core.kyc.domain.model.KycTierState
import com.blockchain.core.kyc.domain.model.KycTiers
import com.blockchain.core.kyc.domain.model.TiersMap
import com.blockchain.nabu.models.responses.nabu.CurrenciesResponse
import com.blockchain.nabu.models.responses.nabu.KycState
import com.blockchain.nabu.models.responses.nabu.NabuUser
import com.blockchain.nabu.models.responses.nabu.UserState
import com.blockchain.nabu.models.responses.tokenresponse.NabuOfflineToken
import com.blockchain.testutils.USD
import com.blockchain.testutils.numberToBigInteger
import info.blockchain.balance.Currency
import info.blockchain.balance.Money

fun getBlankNabuUser(kycState: KycState = KycState.None): NabuUser = NabuUser(
    id = "",
    firstName = "",
    lastName = "",
    email = "",
    emailVerified = false,
    dob = null,
    mobile = "",
    mobileVerified = false,
    address = null,
    state = UserState.None,
    kycState = kycState,
    insertedAt = "",
    updatedAt = "",
    currencies = CurrenciesResponse(
        preferredFiatTradingCurrency = "EUR",
        usableFiatCurrencies = listOf("EUR", "USD", "GBP", "ARS"),
        defaultWalletCurrency = "BRL",
        userFiatCurrencies = listOf("EUR", "GBP")
    )
)

val validOfflineToken
    get() = NabuOfflineToken(
        "userId",
        "lifetimeToken"
    )

fun tiers(tier1State: KycTierState, tier2State: KycTierState): KycTiers {
    return KycTiers(
        TiersMap(
            mapOf(
                KycTier.BRONZE to
                    KycTierDetail(
                        KycTierState.Verified,
                        KycLimits(null, null)
                    ),
                KycTier.SILVER to
                    KycTierDetail(
                        tier1State,
                        KycLimits(null, getLimit(USD, 1000))
                    ),
                KycTier.GOLD to
                    KycTierDetail(
                        tier2State,
                        KycLimits(getLimit(USD, 25000), null)
                    )
            )
        )
    )
}

private fun getLimit(currency: Currency, value: Int) = Money.fromMinor(currency, value.numberToBigInteger())
