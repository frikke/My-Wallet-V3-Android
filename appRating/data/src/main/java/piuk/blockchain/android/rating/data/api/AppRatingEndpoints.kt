package piuk.blockchain.android.rating.data.api

import com.blockchain.api.adapters.ApiError
import com.blockchain.outcome.Outcome
import piuk.blockchain.android.rating.data.api.AppRatingApiConfig.RESPONDENT
import piuk.blockchain.android.rating.data.api.AppRatingApiConfig.RESPONDENT_HEADER_KEY
import piuk.blockchain.android.rating.data.api.AppRatingApiConfig.RESPONDENT_HEADER_MASTER_KEY
import piuk.blockchain.android.rating.data.api.AppRatingApiConfig.RESPONDENT_VAR_SURVEY_ID
import piuk.blockchain.android.rating.data.model.SurveyBody
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

internal interface AppRatingEndpoints {
    @POST(RESPONDENT)
    suspend fun postRatingData(
        @Header(RESPONDENT_HEADER_MASTER_KEY) masterKey: String,
        @Header(RESPONDENT_HEADER_KEY) key: String,

        @Path(RESPONDENT_VAR_SURVEY_ID) surveyId: String,

        @Body surveyBody: SurveyBody
    ): Outcome<ApiError, Boolean>
}
