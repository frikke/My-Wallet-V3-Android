package piuk.blockchain.android.rating.data.api

internal object AppRatingApiConfig {
    const val URL = "https://api-eu.checkmarket.com/3/"

    const val RESPONDENT_HEADER_MASTER_KEY = "X-Master-Key"
    const val RESPONDENT_HEADER_KEY = "X-Key"
    const val RESPONDENT_VAR_SURVEY_ID = "survey_id"
    const val RESPONDENT = "surveys/{$RESPONDENT_VAR_SURVEY_ID}/respondents"
}
