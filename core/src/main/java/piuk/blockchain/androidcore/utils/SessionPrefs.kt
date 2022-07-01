package piuk.blockchain.androidcore.utils

interface SessionPrefs {

    val deviceId: String // Pre-IDV device identifier
    var devicePreIDVCheckFailed: Boolean // Pre-IDV check has failed! Don't show 'gold' announce cards etc

    var metadataUri: String
    var keySchemeUrl: String
    var analyticsReportedNabuUser: String
    var analyticsReportedWalletKey: String

    fun clear()

    fun unPairWallet()

    // Allow QA to randomise device ids when testing kyc
    var qaRandomiseDeviceId: Boolean
}
