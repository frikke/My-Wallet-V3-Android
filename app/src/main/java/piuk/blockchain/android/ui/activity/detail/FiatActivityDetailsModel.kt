package piuk.blockchain.android.ui.activity.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.blockchain.coincore.FiatActivitySummaryItem
import com.blockchain.domain.paymentmethods.PaymentMethodService
import com.blockchain.domain.paymentmethods.model.PaymentMethodDetails
import com.blockchain.outcome.doOnFailure
import com.blockchain.outcome.doOnSuccess
import info.blockchain.balance.FiatCurrency
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import piuk.blockchain.android.domain.repositories.AssetActivityRepository
import timber.log.Timber

data class FiatActivityDetailsViewState(
    val activityItem: FiatActivitySummaryItem? = null,
    val paymentDetails: PaymentMethodDetails? = null,
    val errorMessage: String = ""
)

class FiatActivityDetailsModel(
    private val assetActivityRepository: AssetActivityRepository,
    private val paymentMethodService: PaymentMethodService,
    private val dispatcher: CoroutineDispatcher
) : ViewModel() {

    private val internalState = MutableStateFlow(FiatActivityDetailsViewState())

    val uiState: StateFlow<FiatActivityDetailsViewState> = internalState

    fun findCachedItem(currency: FiatCurrency, txHash: String) {
        viewModelScope.launch {
            withContext(dispatcher) {
                assetActivityRepository.findCachedItem(currency, txHash)?.let { activityItem ->
                    internalState.value = internalState.value.copy(
                        activityItem = activityItem
                    )
                }
            }
        }
    }

    fun loadPaymentDetails(activityItem: FiatActivitySummaryItem) {
        viewModelScope.launch {
            withContext(dispatcher) {
                paymentMethodService.getPaymentMethodDetailsForId(activityItem.paymentMethodId.orEmpty())
                    .doOnFailure { error ->
                        Timber.e("Failed to get PaymentMethodDetails: ${error.name}")
                        // TODO Map error types to error messages
                        internalState.value = internalState.value.copy(
                            errorMessage = "Error: Something went wrong."
                        )
                    }
                    .doOnSuccess { paymentMethodDetails ->
                        internalState.value = internalState.value.copy(
                            activityItem = activityItem,
                            paymentDetails = paymentMethodDetails
                        )
                    }
            }
        }
    }
}
