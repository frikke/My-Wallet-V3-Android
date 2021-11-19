package com.blockchain.preferences

interface DashboardPrefs {
    var isOnBoardingComplete: Boolean
    var isCustodialIntroSeen: Boolean
    var remainingSendsWithoutBackup: Int
    var dashboardAssetOrder: List<String>
    var hasTappedFabButton: Boolean
}
