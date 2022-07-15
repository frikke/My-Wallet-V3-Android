package com.blockchain.core.referral

import com.blockchain.api.NabuErrorStatusCodes
import com.blockchain.api.adapters.ApiError
import com.blockchain.api.services.ReferralApiService
import com.blockchain.domain.referral.ReferralService
import com.blockchain.domain.referral.model.ReferralInfo
import com.blockchain.featureflag.FeatureFlag
import com.blockchain.nabu.Authenticator
import com.blockchain.outcome.Outcome
import com.blockchain.outcome.flatMap
import com.blockchain.outcome.fold
import com.blockchain.outcome.mapError
import com.blockchain.preferences.CurrencyPrefs
import piuk.blockchain.androidcore.utils.extensions.awaitOutcome

class ReferralRepository(
    private val authenticator: Authenticator,
    private val referralApi: ReferralApiService,
    private val currencyPrefs: CurrencyPrefs,
    private val referralFlag: FeatureFlag,
) : ReferralService {

    override suspend fun fetchReferralData(): Outcome<Throwable, ReferralInfo> =
        if (referralFlag.coEnabled()) {
            authenticator.getAuthHeader()
                .awaitOutcome()
                .flatMap { authToken ->
                    referralApi.getReferralCode(
                        authorization = authToken,
                        currency = currencyPrefs.selectedFiatCurrency.networkTicker
                    )
                        .fold(
                            onSuccess = { response ->
                                if (response != null) {
                                    Outcome.Success(
                                        ReferralInfo.Data(
                                            rewardTitle = response.rewardTitle,
                                            rewardSubtitle = response.rewardSubtitle,
                                            criteria = response.criteria,
                                            code = response.code,
                                            campaignId = response.campaignId
                                        )
                                    )
                                } else {
                                    Outcome.Success(ReferralInfo.NotAvailable)
                                }
                            },
                            onFailure = { apiError ->
                                Outcome.Failure(apiError.exception)
                            }
                        )
                }
        } else {
            Outcome.Success(ReferralInfo.NotAvailable)
        }

    override suspend fun isReferralCodeValid(code: String): Outcome<Throwable, Boolean> =
        referralApi.validateReferralCode(code)
            .fold(
                onSuccess = {
                    Outcome.Success(true)
                },
                onFailure = { apiError ->
                    if (apiError is ApiError.KnownError &&
                        apiError.statusCode == NabuErrorStatusCodes.NotFound
                    ) {
                        Outcome.Success(false)
                    } else {
                        Outcome.Failure(apiError.exception)
                    }
                }
            )

    override suspend fun associateReferralCodeIfPresent(validatedCode: String?): Outcome<Throwable, Unit> {
        return when {
            !referralFlag.coEnabled() -> Outcome.Success(Unit)
            validatedCode.isNullOrEmpty() -> Outcome.Success(Unit)
            else -> authenticator.getAuthHeader()
                .awaitOutcome()
                .flatMap { authToken ->
                    referralApi.associateReferralCode(authToken, validatedCode)
                        .mapError { it.exception }
                }
        }
    }
}
