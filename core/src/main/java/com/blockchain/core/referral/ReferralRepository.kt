package com.blockchain.core.referral

import com.blockchain.api.NabuErrorStatusCodes
import com.blockchain.api.adapters.ApiError
import com.blockchain.api.services.ReferralApiService
import com.blockchain.domain.referral.ReferralService
import com.blockchain.domain.referral.model.ReferralInfo
import com.blockchain.domain.referral.model.ReferralValidity
import com.blockchain.nabu.Authenticator
import com.blockchain.outcome.Outcome
import com.blockchain.outcome.flatMap
import com.blockchain.outcome.fold
import com.blockchain.preferences.CurrencyPrefs
import piuk.blockchain.androidcore.utils.extensions.awaitOutcome

class ReferralRepository(
    private val authenticator: Authenticator,
    private val referralApi: ReferralApiService,
    private val currencyPrefs: CurrencyPrefs
) : ReferralService {

    override suspend fun fetchReferralData(): Outcome<Throwable, ReferralInfo> =
        authenticator.getAuthHeader()
            .awaitOutcome()
            .flatMap { authToken ->
                referralApi.getReferralCode(
                    authorization = authToken,
                    currency = currencyPrefs.selectedFiatCurrency.symbol
                )
                    .fold(
                        onSuccess = { response ->
                            Outcome.Success(
                                ReferralInfo.Data(
                                    rewardTitle = response.rewardTitle,
                                    rewardSubtitle = response.rewardSubtitle,
                                    criteria = response.criteria,
                                    code = response.code,
                                )
                            )
                        },
                        onFailure = { apiError ->
                            if (apiError is ApiError.KnownError &&
                                apiError.statusCode == NabuErrorStatusCodes.Forbidden
                            ) {
                                Outcome.Success(ReferralInfo.NotAvailable)
                            } else {
                                Outcome.Failure(apiError.throwable)
                            }
                        }
                    )
            }

    override suspend fun validateReferralCode(code: String): Outcome<Throwable, ReferralValidity> {
        return authenticator.getAuthHeader()
            .awaitOutcome()
            .flatMap {
                referralApi.validateReferralCode(it, code)
                    .fold(
                        onSuccess = { Outcome.Success(ReferralValidity.VALID) },
                        onFailure = { apiError ->
                            if (apiError is ApiError.KnownError &&
                                apiError.statusCode == NabuErrorStatusCodes.NotFound
                            ) {
                                Outcome.Success(ReferralValidity.INVALID)
                            } else {
                                Outcome.Failure(apiError.throwable)
                            }
                        }
                    )
            }
    }
}
