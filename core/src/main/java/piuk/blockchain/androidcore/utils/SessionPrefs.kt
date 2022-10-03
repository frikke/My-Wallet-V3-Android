package piuk.blockchain.androidcore.utils

interface SessionPrefs {

    val deviceId: String // Pre-IDV device identifier
    var devicePreIDVCheckFailed: Boolean // Pre-IDV check has failed! Don't show 'gold' announce cards etc

    var metadataUri: String
    var keySchemeUrl: String
    var analyticsReportedNabuUser: String
    var analyticsReportedWalletKey: String
    var deeplinkUri: String
    fun clearDeeplinkUri()

    fun clear()

    fun unPairWallet()

    // Allow QA to randomise device ids when testing kyc
    var qaRandomiseDeviceId: Boolean

    fun recordDismissal(key: String, time: Long)
    fun deleteDismissalRecord(key: String)
    fun getDismissalEntry(key: String): Long
    fun getLegacyDismissalEntry(key: String): Boolean
}
