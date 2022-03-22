package piuk.blockchain.android.ui.kyc.reentry

import piuk.blockchain.android.ui.kyc.additional_info.TreeNode

sealed class ReentryPoint(val entryPoint: String) {
    object EmailEntry : ReentryPoint("Email Entry")
    object CountrySelection : ReentryPoint("Country Selection")
    object Profile : ReentryPoint("Profile Entry")
    object Address : ReentryPoint("Address Entry")
    data class AdditionalInfo(val root: TreeNode.Root) : ReentryPoint("Extra Info Entry")
    object MobileEntry : ReentryPoint("Mobile Entry")
    object Veriff : ReentryPoint("Veriff Splash")
}
