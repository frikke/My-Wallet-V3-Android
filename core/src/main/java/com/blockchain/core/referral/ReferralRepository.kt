package com.blockchain.core.referral

import com.blockchain.api.services.ReferralApiService
import com.blockchain.domain.referral.ReferralService
import com.blockchain.domain.referral.model.ReferralInfo
import com.blockchain.nabu.Authenticator
import com.blockchain.outcome.flatMap
import com.blockchain.outcome.fold
import com.blockchain.outcome.map
import com.blockchain.outcome.mapLeft
import com.blockchain.preferences.CurrencyPrefs
import piuk.blockchain.androidcore.utils.extensions.awaitOutcome

class ReferralRepository(
    private val authenticator: Authenticator,
    private val referralApi: ReferralApiService,
    private val currencyPrefs: CurrencyPrefs
) : ReferralService {

    override suspend fun fetchReferralData(): ReferralInfo =
        authenticator.getAuthHeader().awaitOutcome()
            .mapLeft { ReferralInfo.NotAvailable }
            .flatMap { authToken ->
                referralApi.getReferralCode(
                    authorization = authToken,
                    currency = currencyPrefs.selectedFiatCurrency.symbol
                )
                    .mapLeft { ReferralInfo.NotAvailable }
                    .map { response ->
                        ReferralInfo.Data(
                            code = response.code,
                            criteria = response.criteria,
                            rewardSubtitle = response.rewardSubtitle,
                            rewardTitle = response.rewardTitle
                        )
                    }
            }
            .fold(onSuccess = { it }, onFailure = { it })
}
