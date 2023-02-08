package com.blockchain.earn.domain.models

enum class EarnRewardsFrequency {
    Daily,
    Weekly,
    Monthly,
    Unknown;

    companion object {
        private const val DAILY = "Daily"
        private const val WEEKLY = "Weekly"
        private const val MONTHLY = "Monthly"

        fun String.toRewardsFrequency(): EarnRewardsFrequency =
            when (this) {
                DAILY -> Daily
                WEEKLY -> Weekly
                MONTHLY -> Monthly
                else -> Unknown
            }
    }
}
