package com.blockchain.presentation.maskedvalue

import com.blockchain.mask.MaskedValueService
import com.blockchain.preferences.MaskedValuePrefs
import kotlinx.coroutines.flow.MutableStateFlow

internal class MaskedValueImpl(
    val prefs: MaskedValuePrefs
) : MaskedValueService {
    override val shouldMask = MutableStateFlow(prefs.shouldMaskValues)

    override fun toggleMaskState() {
        this.shouldMask.value = !this.shouldMask.value
        prefs.shouldMaskValues = this.shouldMask.value
    }
}
