package piuk.blockchain.android.rating.data.api

import com.blockchain.api.adapters.ApiError
import com.blockchain.outcome.Outcome
import piuk.blockchain.android.rating.data.model.AppRatingApiKeys
import piuk.blockchain.android.rating.data.model.SurveyBody
import piuk.blockchain.android.rating.domain.model.AppRating

internal class AppRatingApi(
    private val appRatingEndpoints: AppRatingEndpoints,
) {
    /**
     * Post rating data to checkmaster
     */
    suspend fun postRatingData(
        apiKeys: AppRatingApiKeys,
        appRating: AppRating
    ): Outcome<ApiError, Boolean> {
        return appRatingEndpoints.postRatingData(
            masterKey = apiKeys.masterKey,
            key = apiKeys.key,
            surveyId = apiKeys.surveyId,
            surveyBody = SurveyBody.build(appRating)
        )
    }
}
