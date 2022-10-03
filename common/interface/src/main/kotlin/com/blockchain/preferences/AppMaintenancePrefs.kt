package com.blockchain.preferences

interface AppMaintenancePrefs {
    var isAppMaintenanceRemoteConfigIgnored: Boolean
    var isAppMaintenanceDebugOverrideEnabled: Boolean
    var appMaintenanceDebugJson: String
}
