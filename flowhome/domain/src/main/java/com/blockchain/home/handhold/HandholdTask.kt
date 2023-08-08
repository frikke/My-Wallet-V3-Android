package com.blockchain.home.handhold

data class HandholdTasksStatus(
    val task: HandholdTask,
    val status: HandholdStatus
) {
    val isComplete: Boolean = status == HandholdStatus.Complete
    val isIncomplete: Boolean = status == HandholdStatus.Incomplete
}

enum class HandholdStatus {
    Incomplete,
    Pending,
    Complete
}

enum class HandholdTask {
    VerifyEmail,
    Kyc,
    BuyCrypto
}

fun HandholdTask.isMandatory() = when (this) {
    HandholdTask.VerifyEmail -> false
    HandholdTask.Kyc -> false
    HandholdTask.BuyCrypto -> true
}
