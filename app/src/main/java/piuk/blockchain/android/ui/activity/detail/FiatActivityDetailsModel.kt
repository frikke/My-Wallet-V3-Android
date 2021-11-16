package piuk.blockchain.android.ui.activity.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.blockchain.api.services.PaymentMethodDetails
import com.blockchain.coincore.FiatActivitySummaryItem
import com.blockchain.core.payments.PaymentsDataManager
import com.blockchain.outcome.fold
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
    private val paymentsDataManager: PaymentsDataManager,
    private val dispatcher: CoroutineDispatcher
) : ViewModel() {

    private val internalState = MutableStateFlow(FiatActivityDetailsViewState())

    val uiState: StateFlow<FiatActivityDetailsViewState> = internalState

    fun findCachedItem(currency: String, txHash: String) {
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
                paymentsDataManager.getPaymentMethodDetailsForId(activityItem.paymentMethodId.orEmpty())
                    .fold(
                        onSuccess = { paymentMethodDetails ->
                            internalState.value = internalState.value.copy(
                                activityItem = activityItem,
                                paymentDetails = paymentMethodDetails
                            )
                        },
                        onFailure = { error ->
                            Timber.e("Failed to get PaymentMethodDetails: ${error.name}")
                            // TODO Map error types to error messages
                            internalState.value = internalState.value.copy(
                                errorMessage = "Error: Something went wrong."
                            )
                        }
                    )
            }
        }
    }
}
