package com.blockchain.presentation.viewmodel

import com.blockchain.commonarch.presentation.mvi_v2.Intent
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.ModelState
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.commonarch.presentation.mvi_v2.NavigationEvent
import com.blockchain.commonarch.presentation.mvi_v2.ViewState
import java.util.Locale

class DefaultPhraseViewModel : MviViewModel<DefaultPhraseViewModel.DefaultPhraseIntent,
    DefaultPhraseViewModel.DefaultPhraseViewState,
    DefaultPhraseViewModel.DefaultPhraseModelState,
    DefaultPhraseViewModel.DefaultPhraseNavigationEvent,
    ModelConfigArgs.NoArgs>(
    initialState = DefaultPhraseModelState()
) {
    override fun viewCreated(args: ModelConfigArgs.NoArgs) {}

    override fun reduce(state: DefaultPhraseModelState): DefaultPhraseViewState {
        return with(state) {
            DefaultPhraseViewState(
                showProgress = isLoading,
                keyWords = mnemonic,
                warning = if (hasBackup) BackUpPhraseWarning.NONE else BackUpPhraseWarning.NO_BACKUP
            )
        }
    }

    override suspend fun handleIntent(modelState: DefaultPhraseModelState, intent: DefaultPhraseIntent) {
        when (intent) {
            is DefaultPhraseIntent.LoadDefaultPhrase -> {
                updateState {
                    modelState.copy(
                        mnemonic = mnemonic()
                    )
                }
            }
        }
    }

    data class DefaultPhraseViewState(
        val showProgress: Boolean,
        val keyWords: List<String>,
        val warning: BackUpPhraseWarning = BackUpPhraseWarning.NONE
    ) : ViewState

    data class DefaultPhraseModelState(
        val isLoading: Boolean = false,
        val mnemonic: List<String> = emptyList(),
        val hasBackup: Boolean = false
    ) : ModelState

    sealed class DefaultPhraseIntent : Intent<DefaultPhraseModelState> {
        object LoadDefaultPhrase : DefaultPhraseIntent() {
            override fun isValidFor(modelState: DefaultPhraseModelState): Boolean {
                return modelState.isLoading
            }
        }
    }

    enum class BackUpPhraseWarning {
        NONE, NO_BACKUP
    }

    sealed class DefaultPhraseNavigationEvent : NavigationEvent {
        object BackupManually : DefaultPhraseNavigationEvent()
    }

    private fun mnemonic(): List<String> {
        val locales = Locale.getISOCountries().toList()
        return locales.map {
            Locale("", it).displayCountry
        }.shuffled().subList(0, 12)
    }
}