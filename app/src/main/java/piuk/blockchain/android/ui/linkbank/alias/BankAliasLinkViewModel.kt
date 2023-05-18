package piuk.blockchain.android.ui.linkbank.alias

import androidx.lifecycle.viewModelScope
import com.blockchain.api.NabuApiExceptionFactory
import com.blockchain.api.isInternetConnectionError
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.ModelState
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.commonarch.presentation.mvi_v2.NavigationEvent
import com.blockchain.componentlib.button.ButtonState
import com.blockchain.domain.paymentmethods.BankService
import com.blockchain.domain.paymentmethods.model.AliasInfo
import com.blockchain.extensions.exhaustive
import com.blockchain.outcome.fold
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import retrofit2.HttpException

data class BankAliasLinkModelState(
    val alias: String? = null,
    val aliasInfo: AliasInfo? = null,
    val error: AliasError? = null,
    val isLoading: Boolean = false
) : ModelState

sealed interface BankAliasNavigationEvent : NavigationEvent {
    object BankAccountLinkedWithAlias : BankAliasNavigationEvent
    data class UnhandledError(val error: AliasError) : BankAliasNavigationEvent
}

class BankAliasLinkViewModel(
    private val bankService: BankService,
    private val debounceSearchTimeoutInMillis: Long = SEARCH_TEXT_DEBOUNCE_TIMEOUT
) : MviViewModel<
    BankAliasLinkIntent,
    BankAliasLinkViewState,
    BankAliasLinkModelState,
    BankAliasNavigationEvent,
    ModelConfigArgs.NoArgs
    >(BankAliasLinkModelState()) {

    private val searchText = MutableStateFlow("")

    init {
        viewModelScope.launch {
            searchText
                .debounce(debounceSearchTimeoutInMillis)
                .collect { text ->
                    updateState {
                        copy(
                            alias = text,
                            aliasInfo = null,
                            error = null,
                            isLoading = false
                        )
                    }
                }
        }
    }

    override fun viewCreated(args: ModelConfigArgs.NoArgs) {}

    override suspend fun handleIntent(modelState: BankAliasLinkModelState, intent: BankAliasLinkIntent) {
        when (intent) {
            is BankAliasLinkIntent.AliasUpdated ->
                updateAlias(intent.alias)
            is BankAliasLinkIntent.LoadBeneficiaryInfo ->
                loadBeneficiaryInfo(currency = intent.currency, address = intent.address)
            is BankAliasLinkIntent.ActivateBeneficiary ->
                activateBeneficiary(intent.alias)
        }.exhaustive
    }

    override fun BankAliasLinkModelState.reduce() = BankAliasLinkViewState(
        showAliasInput = aliasInfo == null && error == null,
        alias = alias,
        aliasInfo = aliasInfo,
        error = error,
        ctaState = when {
            isLoading -> ButtonState.Loading
            alias.orEmpty().length in MIN_ALIAS_LENGTH..MAX_ALIAS_LENGTH -> ButtonState.Enabled
            else -> ButtonState.Disabled
        }
    )

    private fun updateAlias(alias: String) {
        if (alias.isEmpty()) {
            updateState {
                copy(
                    alias = alias,
                    aliasInfo = null,
                    error = null,
                    isLoading = false
                )
            }
        } else {
            searchText.value = alias
        }
    }

    private fun loadBeneficiaryInfo(currency: String, address: String) {
        viewModelScope.launch {
            updateState {
                copy(isLoading = true)
            }
            bankService.getBeneficiaryInfo(currency = currency, address = address).fold(
                onSuccess = { aliasInfo ->
                    updateState {
                        copy(
                            isLoading = false,
                            error = null,
                            aliasInfo = aliasInfo
                        )
                    }
                },
                onFailure = { exception ->
                    handleError(exception)
                }
            )
        }
    }

    private fun activateBeneficiary(alias: String) {
        viewModelScope.launch {
            updateState {
                copy(isLoading = true)
            }
            bankService.activateBeneficiary(alias).fold(
                onSuccess = {
                    updateState {
                        copy(
                            isLoading = false,
                            error = null
                        )
                    }
                    // TODO check if we still need to update state or just navigate
                    navigate(BankAliasNavigationEvent.BankAccountLinkedWithAlias)
                },
                onFailure = { exception ->
                    handleError(exception)
                }
            )
        }
    }

    private fun handleError(exception: Exception) {
        val aliasError = exception.toAliasError()
        if (aliasError is AliasError.ServerSideUxError) {
            updateState {
                copy(
                    isLoading = false,
                    aliasInfo = null,
                    error = exception.toAliasError()
                )
            }
        } else {
            updateState {
                copy(isLoading = false)
            }
            navigate(BankAliasNavigationEvent.UnhandledError(aliasError))
        }
    }

    private fun Exception.toAliasError(): AliasError =
        when {
            this is HttpException -> {
                val error = NabuApiExceptionFactory.fromResponseBody(this)
                error.getServerSideErrorInfo()?.let { serverError ->
                    AliasError.ServerSideUxError(serverError)
                } ?: AliasError.UnhandledHttpError(error)
            }
            this.isInternetConnectionError() -> AliasError.InternetConnectionError
            else -> AliasError.GeneralError(this)
        }

    companion object {
        private const val MIN_ALIAS_LENGTH = 6
        private const val MAX_ALIAS_LENGTH = 20
        private const val SEARCH_TEXT_DEBOUNCE_TIMEOUT = 300L
    }
}
