package com.blockchain.core.referral

import com.blockchain.api.NabuApiException
import com.blockchain.api.NabuErrorStatusCodes
import com.blockchain.api.referral.data.StyleData
import com.blockchain.api.services.ReferralApiService
import com.blockchain.core.referral.dataresource.ReferralStore
import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.FreshnessStrategy.Companion.withKey
import com.blockchain.domain.common.model.PromotionStyleInfo
import com.blockchain.domain.common.model.ServerErrorAction
import com.blockchain.domain.referral.ReferralService
import com.blockchain.domain.referral.model.ReferralInfo
import com.blockchain.outcome.Outcome
import com.blockchain.outcome.fold
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.store.firstOutcome
import com.blockchain.store.mapData
import kotlinx.coroutines.flow.Flow

class ReferralRepository(
    private val referralStore: ReferralStore,
    private val referralApi: ReferralApiService,
    private val currencyPrefs: CurrencyPrefs,
) : ReferralService {

    override suspend fun fetchReferralDataLegacy(): Outcome<Throwable, ReferralInfo> =
        fetchReferralData(FreshnessStrategy.Fresh).firstOutcome()

    override fun fetchReferralData(
        freshnessStrategy: FreshnessStrategy
    ): Flow<DataResource<ReferralInfo>> {
        return referralStore
            .stream(
                freshnessStrategy.withKey(ReferralStore.Key(currencyPrefs.selectedFiatCurrency.networkTicker))
            )
            .mapData {
                it.referralResponse?.let { response ->
                    ReferralInfo.Data(
                        rewardTitle = response.rewardTitle,
                        rewardSubtitle = response.rewardSubtitle,
                        criteria = response.criteria,
                        code = response.code,
                        campaignId = response.campaignId,
                        announcementInfo = response.announcement?.toDomain(),
                        promotionInfo = response.promotion?.toDomain()
                    )
                } ?: ReferralInfo.NotAvailable
            }
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
            else -> referralApi.associateReferralCode(validatedCode)
        }
    }
}
