package piuk.blockchain.android.rating.data.api

import com.blockchain.api.adapters.ApiException
import com.blockchain.outcome.Outcome
import com.blockchain.outcome.map
import piuk.blockchain.android.rating.data.model.AppRatingApiKeys
import piuk.blockchain.android.rating.data.model.SurveyBody
import piuk.blockchain.android.rating.domain.model.AppRating

internal interface AppRatingApi {
    suspend fun postRatingData(apiKeys: AppRatingApiKeys, appRating: AppRating): Outcome<ApiException, Boolean>
}

internal class AppRatingApiService(
    private val appRatingEndpoints: AppRatingEndpoints,
) : AppRatingApi {
    /**
     * Post rating data to checkmarket
     */
    override suspend fun postRatingData(
        apiKeys: AppRatingApiKeys,
        appRating: AppRating
    ): Outcome<ApiException, Boolean> {
        return appRatingEndpoints.postRatingData(
            masterKey = apiKeys.masterKey,
            key = apiKeys.key,
            surveyId = apiKeys.surveyId,
            surveyBody = SurveyBody.build(appRating)
        ).map { it.data.succeeded }
    }
}
