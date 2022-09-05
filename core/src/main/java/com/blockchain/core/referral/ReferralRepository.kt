package com.blockchain.core.referral

import com.blockchain.api.NabuApiException
import com.blockchain.api.NabuErrorStatusCodes
import com.blockchain.api.referral.data.StyleData
import com.blockchain.api.services.ReferralApiService
import com.blockchain.domain.common.model.PromotionStyleInfo
import com.blockchain.domain.common.model.ServerErrorAction
import com.blockchain.domain.referral.ReferralService
import com.blockchain.domain.referral.model.ReferralInfo
import com.blockchain.nabu.Authenticator
import com.blockchain.outcome.Outcome
import com.blockchain.outcome.flatMap
import com.blockchain.outcome.fold
import com.blockchain.preferences.CurrencyPrefs
import piuk.blockchain.androidcore.utils.extensions.awaitOutcome

class ReferralRepository(
    private val authenticator: Authenticator,
    private val referralApi: ReferralApiService,
    private val currencyPrefs: CurrencyPrefs,
) : ReferralService {

    override suspend fun fetchReferralData(): Outcome<Throwable, ReferralInfo> =
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
                                        campaignId = response.campaignId,
                                        announcementInfo = response.announcement?.toDomain(),
                                        promotionInfo = response.promotion?.toDomain()
                                    )
                                )
                            } else {
                                Outcome.Success(ReferralInfo.NotAvailable)
                            }
                        },
                        onFailure = { apiError ->
                            Outcome.Failure(apiError)
                        }
                    )
            }

    private fun StyleData?.toDomain(): PromotionStyleInfo =
        PromotionStyleInfo(
            title = this?.title.orEmpty(),
            message = this?.message.orEmpty(),
            iconUrl = this?.icon?.url.orEmpty(),
            headerUrl = this?.header?.media?.url.orEmpty(),
            backgroundUrl = this?.style?.background?.media?.url.orEmpty(),
            foregroundColorScheme = this?.style?.foreground?.color?.hsb
                ?: emptyList(),
            actions = this?.actions?.map {
                ServerErrorAction(
                    it.title,
                    it.url.orEmpty()
                )
            } ?: emptyList()
        )

    override suspend fun isReferralCodeValid(code: String): Outcome<Throwable, Boolean> =
        referralApi.validateReferralCode(code)
            .fold(
                onSuccess = {
                    Outcome.Success(true)
                },
                onFailure = { apiError ->
                    if ((apiError as? NabuApiException)?.getErrorStatusCode() == NabuErrorStatusCodes.NotFound
                    ) {
                        Outcome.Success(false)
                    } else {
                        Outcome.Failure(apiError)
                    }
                }
            )

    override suspend fun associateReferralCodeIfPresent(validatedCode: String?): Outcome<Throwable, Unit> {
        return when {
            validatedCode.isNullOrEmpty() -> Outcome.Success(Unit)
            else -> authenticator.getAuthHeader()
                .awaitOutcome()
                .flatMap { authToken ->
                    referralApi.associateReferralCode(authToken, validatedCode)
                }
        }
    }
}
