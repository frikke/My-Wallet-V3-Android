package com.blockchain.banking

import com.blockchain.domain.paymentmethods.model.BankPartner
import com.blockchain.domain.paymentmethods.model.LinkedBank
import info.blockchain.balance.FiatValue
import java.io.Serializable

enum class BankTransferAction {
    LINK, PAY
}

interface BankPartnerCallbackProvider {
    fun callback(partner: BankPartner, action: BankTransferAction): String
}

@kotlinx.serialization.Serializable
data class BankPaymentApproval(
    val paymentId: String,
    val authorisationUrl: String,
    val linkedBank: LinkedBank,
    val orderValue: FiatValue
) : Serializable
