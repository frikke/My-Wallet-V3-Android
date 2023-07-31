package com.blockchain.fiatActions

import com.blockchain.commonarch.presentation.mvi_v2.MVIBottomSheet

interface QuestionnaireSheetHost : MVIBottomSheet.Host {
    fun questionnaireSubmittedSuccessfully()
    fun questionnaireSkipped()
}
