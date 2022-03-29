package com.blockchain.preferences

interface DashboardPrefs {
    var isOnboardingComplete: Boolean
    var isCustodialIntroSeen: Boolean
    var isPrivateKeyIntroSeen: Boolean
    var isRewardsIntroSeen: Boolean
    var remainingSendsWithoutBackup: Int
    var dashboardAssetOrder: List<String>
    var hasTappedFabButton: Boolean
}
