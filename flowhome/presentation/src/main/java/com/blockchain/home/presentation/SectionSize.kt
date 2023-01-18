package com.blockchain.home.presentation

// todo move to be available for other modules: activities and other dashboard stuff
sealed class SectionSize(open val size: Int) {
    object All : SectionSize(size = Int.MAX_VALUE)
    data class Limited(override val size: Int) : SectionSize(size = size)
}
