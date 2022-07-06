package com.blockchain.domain.dataremediation.model

data class Questionnaire(
    val header: QuestionnaireHeader?,
    val context: QuestionnaireContext,
    val nodes: List<QuestionnaireNode>,
    val isMandatory: Boolean,
) : java.io.Serializable

data class QuestionnaireHeader(
    val title: String,
    val description: String,
) : java.io.Serializable
