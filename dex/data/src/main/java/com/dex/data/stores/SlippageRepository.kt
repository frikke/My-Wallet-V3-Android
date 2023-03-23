package com.dex.data.stores

import com.blockchain.preferences.DexPrefs
import com.dex.domain.SlippageService

class SlippageRepository(
    private val slippagePersistence: DexPrefs
) : SlippageService {
    override suspend fun availableSlippages(): List<Double> {
        /**
         * Hardcoded
         */
        return listOf(
            0.002,
            0.005,
            0.01,
            0.03
        )
    }

    override suspend fun selectedSlippage(): Double {
        val slippages = availableSlippages()
        return slippages.getOrNull(slippagePersistence.selectedSlippageIndex) ?: run {
            val index = ((slippages.size / 2) - 1)
            slippages[index].also {
                slippagePersistence.selectedSlippageIndex = index
            }
        }
    }

    override fun updateSelectedSlippageIndex(index: Int) {
        slippagePersistence.selectedSlippageIndex = index
    }
}
