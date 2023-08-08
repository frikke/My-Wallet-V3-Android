package piuk.blockchain.android.rating.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import piuk.blockchain.android.rating.domain.model.AppRating

/**
 * Useful links:
 *
 * * https://api.checkmarket.com/docs/api/v3/action/POST-3-surveys-surveyId-respondents_includeSuccessResponses
 *
 * * OS value: https://api.checkmarket.com/docs/api/v3/action/GET-3-lookup-os
 * * status id values: https://api.checkmarket.com/docs/api/v3/action/GET-3-lookup-respondentstatus
 *
 */
@Serializable
internal data class SurveyBody private constructor(
    @SerialName("LanguageCode") val languageCode: String = LANGUAGE_CODE,
    @SerialName("RespondentStatusId") val respondentStatusId: Int = RESPONDENT_STATUS_ID,
    @SerialName("Preview") val preview: Boolean = PREVIEW,
    @SerialName("OsId") val osId: Int = ANDROID_OS_ID,
    @SerialName("QuestionResponses") val questionResponses: List<SurveyQuestionResponses>
) {
    companion object {
        /**
         * We're not using the webpage for the survey so language doesn't matter
         */
        private const val LANGUAGE_CODE = "en"

        /**
         * * 0 - Partially done - we don't use this as we send all data at once
         * * 1 - Completed survey - this is the one we always use
         * * 2 - Screened out - we don't use this
         */
        private const val RESPONDENT_STATUS_ID = 1

        /**
         * * true - api will return normally (including errors) but will not store data
         * * false - normal behaviour
         */
        private const val PREVIEW = false

        /**
         * Static id for android
         */
        private const val ANDROID_OS_ID = 34

        /**
         * Builds a survey based on [appRating] contents
         * Mainly to not add the comment field as sending empty would throw an error (R0105: Empty response)
         */
        fun build(appRating: AppRating): SurveyBody {
            return SurveyBody(
                questionResponses = listOf(
                    // Static rating question id = 1 (rating)
                    SurveyQuestionResponses(
                        questionId = 1,
                        responses = listOf(SurveyResponse(responseId = appRating.rating))
                    ),

                    // Static feedback question id = 2 (feedback)
                    SurveyQuestionResponses(
                        questionId = 2,
                        responses = listOf(
                            SurveyResponse(
                                responseId = 1,
                                value = appRating.feedback
                            )
                        )
                    ),

                    // Static feedback question id = 3 (is superapp)
                    // ResponseId: 1 = true / 2 = false
                    SurveyQuestionResponses(
                        questionId = 3,
                        responses = listOf(
                            SurveyResponse(responseId = 1)
                        )
                    )
                )
            )
        }
    }
}

@Serializable
internal data class SurveyQuestionResponses(
    @SerialName("QuestionId") val questionId: Int,
    @SerialName("Responses") val responses: List<SurveyResponse>
)

@Serializable
internal data class SurveyResponse(
    @SerialName("ResponseId") val responseId: Int,
    @SerialName("Value") val value: String? = null
)
