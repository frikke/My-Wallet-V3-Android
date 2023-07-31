package com.dex.domain

interface SlippageService {
    suspend fun availableSlippages(): List<Double>
    suspend fun selectedSlippage(): Double
    fun updateSelectedSlippageIndex(index: Int)
}
