package com.blockchain.mask

import kotlinx.coroutines.flow.StateFlow

interface MaskedValueService {
    val shouldMask: StateFlow<Boolean>
    fun toggleMaskState()
}
