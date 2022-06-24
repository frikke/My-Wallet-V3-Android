package com.blockchain.domain.dataremediation

import com.blockchain.domain.dataremediation.model.DataRemediationError
import com.blockchain.domain.dataremediation.model.QuestionnaireNode
import com.blockchain.domain.dataremediation.model.SubmitQuestionnaireError
import com.blockchain.outcome.Outcome

interface DataRemediationService {
    suspend fun getQuestionnaire(): Outcome<DataRemediationError, List<QuestionnaireNode>>

    suspend fun submitQuestionnaire(nodes: List<QuestionnaireNode>): Outcome<SubmitQuestionnaireError, Unit>
}
