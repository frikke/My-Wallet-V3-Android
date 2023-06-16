package com.blockchain.home.handhold

data class HandholdStepStatus(
    val step: HandholdStep,
    val status: HandholStatus
) {
    val isComplete: Boolean = status == HandholStatus.Complete
    val isIncomplete: Boolean = status == HandholStatus.Incomplete
}

enum class HandholStatus {
    Incomplete,
    Pending,
    Complete
}

enum class HandholdStep {
    VerifyEmail,
    Kyc,
    BuyCrypto
}