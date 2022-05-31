package com.blockchain.presentation.viewmodel

import com.blockchain.commonarch.presentation.mvi_v2.Intent

sealed interface DefaultPhraseIntent : Intent<DefaultPhraseModelState> {
    object LoadDefaultPhrase : DefaultPhraseIntent
    object MnemonicCopied : DefaultPhraseIntent
    object ResetCopy : DefaultPhraseIntent
}
