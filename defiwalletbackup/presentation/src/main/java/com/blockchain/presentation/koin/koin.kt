package com.blockchain.presentation.koin

import com.blockchain.koin.payloadScopeQualifier
import com.blockchain.presentation.viewmodel.BackupPhraseViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val backupPhrasePresentationModule = module {
    scope(payloadScopeQualifier) {
        viewModel {
            BackupPhraseViewModel(backupPhraseService = get())
        }
    }
}
