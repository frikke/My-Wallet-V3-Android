package piuk.blockchain.android.ui.transactionflow.flow.adapter

import com.blockchain.coincore.TxConfirmationValue
import com.blockchain.core.price.ExchangeRates
import piuk.blockchain.android.ui.adapters.AdapterDelegatesManager
import piuk.blockchain.android.ui.adapters.DelegationAdapter
import piuk.blockchain.android.ui.transactionflow.engine.TransactionModel
import piuk.blockchain.android.ui.transactionflow.flow.TxConfirmReadOnlyMapperCheckout

class ConfirmTransactionDelegateAdapter(
    model: TransactionModel,
    mapper: TxConfirmReadOnlyMapperCheckout,
    exchangeRates: ExchangeRates,
    selectedCurrency: String
) : DelegationAdapter<TxConfirmationValue>(AdapterDelegatesManager(), emptyList()) {
    init {
        // Add all necessary AdapterDelegate objects here
        with(delegatesManager) {
            // New checkout screens:
            addAdapterDelegate(SimpleConfirmationCheckoutDelegate(mapper))
            addAdapterDelegate(ComplexConfirmationCheckoutDelegate(mapper))
            addAdapterDelegate(ExpandableSimpleConfirmationCheckout(mapper))
            addAdapterDelegate(ExpandableComplexConfirmationCheckout(mapper))
            addAdapterDelegate(CompoundExpandableFeeConfirmationCheckoutDelegate(mapper))

            addAdapterDelegate(ConfirmNoteItemDelegate(model))
            addAdapterDelegate(ConfirmXlmMemoItemDelegate(model))
            addAdapterDelegate(ConfirmAgreementWithTAndCsItemDelegate(model))
            addAdapterDelegate(
                ConfirmAgreementToTransferItemDelegate(
                    model,
                    exchangeRates,
                    selectedCurrency
                )
            )
            addAdapterDelegate(LargeTransactionWarningItemDelegate(model))
            addAdapterDelegate(InvoiceCountdownTimerDelegate())
            addAdapterDelegate(ConfirmInfoItemValidationStatusDelegate())
        }
    }
}
