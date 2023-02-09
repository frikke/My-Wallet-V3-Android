package com.blockchain.preferences

interface DashboardPrefs {
    var isOnboardingComplete: Boolean
    var isCustodialIntroSeen: Boolean
    var isPrivateKeyIntroSeen: Boolean
    var isRewardsIntroSeen: Boolean
    var isStakingIntroSeen: Boolean
    var isActiveRewardsIntroSeen: Boolean
    var remainingSendsWithoutBackup: Int
}
