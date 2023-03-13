package com.blockchain.domain.paymentmethods.model

import com.blockchain.domain.common.model.ServerSideUxErrorInfo
import java.io.Serializable

data class LinkedBankCapabilities(
//    val deposit: LinkedBankCapability?, // not implemented
    val withdrawal: LinkedBankCapability?,
//    val brokerage: LinkedBankCapability?, // not implemented
) : Serializable

data class LinkedBankCapability(
    val enabled: Boolean,
    val ux: ServerSideUxErrorInfo?,
) : Serializable
