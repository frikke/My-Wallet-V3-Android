package piuk.blockchain.android.ui.transactionflow.flow.adapter

import androidx.recyclerview.widget.DiffUtil
import com.blockchain.coincore.TxConfirmationValue
import com.blockchain.coincore.UserEditable
import com.blockchain.core.price.ExchangeRates
import com.blockchain.featureflag.FeatureFlag
import com.blockchain.koin.activeRewardsWithdrawalsFeatureFlag
import info.blockchain.balance.FiatCurrency
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import piuk.blockchain.android.ui.adapters.AdapterDelegatesManager
import piuk.blockchain.android.ui.adapters.DelegationAdapter
import piuk.blockchain.android.ui.transactionflow.engine.TransactionModel
import piuk.blockchain.android.ui.transactionflow.flow.TxConfirmReadOnlyMapperCheckout

class ConfirmTransactionDelegateAdapter(
    model: TransactionModel,
    mapper: TxConfirmReadOnlyMapperCheckout,
    exchangeRates: ExchangeRates,
    selectedCurrency: FiatCurrency,
    onTooltipClicked: (TxConfirmationValue) -> Unit,
    coroutineScope: CoroutineScope
) : DelegationAdapter<TxConfirmationValue>(AdapterDelegatesManager(), emptyList()), KoinComponent {

    private val activeRewardsWithdrawalsFF: FeatureFlag by inject(activeRewardsWithdrawalsFeatureFlag)

    override var items: List<TxConfirmationValue> = emptyList()
        set(value) {
            val diffResult =
                DiffUtil.calculateDiff(TxConfirmationValueDiffUtil(this.items, value))
            field = value
            diffResult.dispatchUpdatesTo(this)
        }

    init {
        // Add all necessary AdapterDelegate objects here
        with(delegatesManager) {
            // New checkout screens:
            addAdapterDelegate(SimpleConfirmationCheckoutDelegate(mapper))
            addAdapterDelegate(ComplexConfirmationCheckoutDelegate(mapper))
            addAdapterDelegate(ExpandableSimpleConfirmationCheckout(mapper))
            addAdapterDelegate(ExpandableSingleValueConfirmationAdapter(mapper))
            addAdapterDelegate(HeaderConfirmationDelegate())
            addAdapterDelegate(ExpandableComplexConfirmationCheckout(mapper, onTooltipClicked))
            addAdapterDelegate(CompoundExpandableFeeConfirmationCheckoutDelegate(mapper))

            addAdapterDelegate(ConfirmNoteItemDelegate(model))
            addAdapterDelegate(ConfirmXlmMemoItemDelegate(model))
            addAdapterDelegate(ConfirmAgreementWithTAndCsItemDelegate(model))

            addAdapterDelegate(LargeTransactionWarningItemDelegate(model))
            addAdapterDelegate(InvoiceCountdownTimerDelegate())
            addAdapterDelegate(ConfirmInfoItemValidationStatusDelegate())
            addAdapterDelegate(QuoteCountdownConfirmationDelegate())
            addAdapterDelegate(TooltipConfirmationCheckoutDelegate(mapper, onTooltipClicked))
            addAdapterDelegate(ReadMoreDisclaimerDelegate(mapper, onTooltipClicked))

            coroutineScope.launch {
                if (activeRewardsWithdrawalsFF.coEnabled().not()) {
                    addAdapterDelegate(
                        ConfirmAgreementToWithdrawalBlockedItemDelegate(
                            model
                        )
                    )

                    addAdapterDelegate(
                        ConfirmAgreementToTransferItemDelegate(
                            model,
                            exchangeRates,
                            selectedCurrency
                        )
                    )
                } else {
                    addAdapterDelegate(
                        ConfirmAgreementToTransferItemDelegate(
                            model,
                            exchangeRates,
                            selectedCurrency
                        )
                    )
                }
            }
        }
    }
}

/**
 * In case of something being UserEditable, we want the model to update the ui only once. Then when the user updates
 * the UI, we want only the model to get updated to avoid any racing conditions of having both the user and the model
 * updating the confirmation value
 *
 */

class TxConfirmationValueDiffUtil(
    private val oldItems: List<TxConfirmationValue>,
    private val newItems: List<TxConfirmationValue>
) :
    DiffUtil.Callback() {
    override fun getOldListSize(): Int = oldItems.size

    override fun getNewListSize(): Int = newItems.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
        when {
            oldItems[oldItemPosition] is UserEditable && newItems[newItemPosition] is UserEditable -> true
            oldItems[oldItemPosition] is TxConfirmationValue.QuoteCountDown &&
                newItems[newItemPosition] is TxConfirmationValue.QuoteCountDown -> false
            else -> oldItems[oldItemPosition] == newItems[newItemPosition]
        }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean = when {
        oldItems[oldItemPosition] is UserEditable && newItems[newItemPosition] is UserEditable -> true
        oldItems[oldItemPosition] is TxConfirmationValue.QuoteCountDown &&
            newItems[newItemPosition] is TxConfirmationValue.QuoteCountDown -> false
        else -> oldItems[oldItemPosition] == newItems[newItemPosition]
    }
}
