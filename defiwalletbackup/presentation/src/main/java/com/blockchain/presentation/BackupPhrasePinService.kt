package com.blockchain.presentation

import com.blockchain.commonarch.presentation.base.BlockchainActivity

interface BackupPhrasePinService {
    fun init(activity: BlockchainActivity)
    fun verifyPin(callback: (successful: Boolean, secondPassword: String?) -> Unit)
}
