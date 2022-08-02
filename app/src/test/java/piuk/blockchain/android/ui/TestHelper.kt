package piuk.blockchain.android.ui

import com.blockchain.nabu.models.responses.nabu.CurrenciesResponse
import com.blockchain.nabu.models.responses.nabu.KycState
import com.blockchain.nabu.models.responses.nabu.KycTierLevel
import com.blockchain.nabu.models.responses.nabu.KycTierState
import com.blockchain.nabu.models.responses.nabu.KycTiers
import com.blockchain.nabu.models.responses.nabu.Limits
import com.blockchain.nabu.models.responses.nabu.NabuUser
import com.blockchain.nabu.models.responses.nabu.Tier
import com.blockchain.nabu.models.responses.nabu.Tiers
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
        Tiers(
            mapOf(
                KycTierLevel.BRONZE to
                    Tier(
                        KycTierState.Verified,
                        Limits(null, null)
                    ),
                KycTierLevel.SILVER to
                    Tier(
                        tier1State,
                        Limits(null, getLimit(USD, 1000))
                    ),
                KycTierLevel.GOLD to
                    Tier(
                        tier2State,
                        Limits(getLimit(USD, 25000), null)
                    )
            )
        )
    )
}

private fun getLimit(currency: Currency, value: Int) = Money.fromMinor(currency, value.numberToBigInteger())
