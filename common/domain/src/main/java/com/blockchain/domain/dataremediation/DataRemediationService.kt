package com.blockchain.domain.dataremediation

import com.blockchain.domain.dataremediation.model.DataRemediationError
import com.blockchain.domain.dataremediation.model.Questionnaire
import com.blockchain.domain.dataremediation.model.QuestionnaireContext
import com.blockchain.domain.dataremediation.model.SubmitQuestionnaireError
import com.blockchain.outcome.Outcome

interface DataRemediationService {
    suspend fun getQuestionnaire(
        questionnaireContext: QuestionnaireContext
    ): Outcome<DataRemediationError, Questionnaire?>

    suspend fun submitQuestionnaire(
        questionnaire: Questionnaire
    ): Outcome<SubmitQuestionnaireError, Unit>
}
