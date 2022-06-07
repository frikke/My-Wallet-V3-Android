package piuk.blockchain.android.rating.data.api

import com.blockchain.api.adapters.ApiError
import com.blockchain.outcome.Outcome
import piuk.blockchain.android.rating.data.model.RespondentResponse
import piuk.blockchain.android.rating.data.model.SurveyBody
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

private const val RESPONDENT_HEADER_MASTER_KEY = "X-Master-Key"
private const val RESPONDENT_HEADER_KEY = "X-Key"
private const val RESPONDENT_VAR_SURVEY_ID = "survey_id"
private const val RESPONDENT = "3/surveys/{$RESPONDENT_VAR_SURVEY_ID}/respondents"

internal interface AppRatingEndpoints {
    @POST(RESPONDENT)
    suspend fun postRatingData(
        @Header(RESPONDENT_HEADER_MASTER_KEY) masterKey: String,
        @Header(RESPONDENT_HEADER_KEY) key: String,

        @Path(RESPONDENT_VAR_SURVEY_ID) surveyId: String,

        @Body surveyBody: SurveyBody
    ): Outcome<ApiError, RespondentResponse>
}
