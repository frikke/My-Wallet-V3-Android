package com.blockchain.home.handhold

data class HandholdTasksStatus(
    val task: HandholdTask,
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

enum class HandholdTask {
    VerifyEmail,
    Kyc,
    BuyCrypto
}