package com.blockchain.home.presentation.allassets

// todo move to be available for other modules: activities and other dashboard stuff
sealed class SectionSize(open val size: Int) {
    companion object {
        const val DEFAULT_SIZE = 8
    }

    object All : SectionSize(size = Int.MAX_VALUE)
    data class Limited(override val size: Int = DEFAULT_SIZE) : SectionSize(size = size)
}
