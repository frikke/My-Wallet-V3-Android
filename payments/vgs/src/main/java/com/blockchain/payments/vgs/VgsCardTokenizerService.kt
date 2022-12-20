package com.blockchain.payments.vgs

import android.content.Context
import com.blockchain.payments.core.CardBillingAddress
import com.verygoodsecurity.vgscollect.view.InputFieldView
import com.verygoodsecurity.vgscollect.widget.ExpirationDateEditText

interface VgsCardTokenizerService {
    fun init(context: Context, vaultId: String)
    fun bindCardDetails(
        name: InputFieldView,
        cardNumber: InputFieldView,
        expiration: ExpirationDateEditText,
        cvv: InputFieldView,
        cardTokenId: String
    )

    fun bindAddressDetails(billingAddress: CardBillingAddress)
    fun isInitialised(): Boolean
    fun isValid(): Boolean
    suspend fun submit(): String
    fun destroy()
}
