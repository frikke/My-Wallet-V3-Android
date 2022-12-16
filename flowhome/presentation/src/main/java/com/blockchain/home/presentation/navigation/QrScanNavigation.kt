package com.blockchain.home.presentation.navigation

import android.os.Parcelable
import androidx.activity.result.ActivityResultLauncher
import info.blockchain.balance.AssetInfo
import kotlinx.parcelize.Parcelize

sealed class QrExpected : Parcelable {
    @Parcelize
    object AnyAssetAddressQr : QrExpected()

    @Parcelize
    data class AssetAddressQr(val assetTicker: String) : QrExpected()

    @Parcelize
    object BitPayQr : QrExpected()

    @Parcelize
    object WalletConnectQr : QrExpected()

    @Parcelize
    object ImportWalletKeysQr : QrExpected() // Import a wallet.

    @Parcelize
    object WebLoginQr : QrExpected() // New auth

    companion object {
        val IMPORT_KEYS_QR = setOf(ImportWalletKeysQr)
        val WEB_LOGIN_QR = setOf(WebLoginQr)
        val MAIN_ACTIVITY_QR = setOf(AnyAssetAddressQr, BitPayQr, WalletConnectQr)

        @Suppress("FunctionName")
        fun ASSET_ADDRESS_QR(asset: AssetInfo) = setOf(AssetAddressQr(asset.networkTicker))
    }
}

interface QrScanNavigation {
    fun registerForQrScan(onScan: (String) -> Unit = {}): ActivityResultLauncher<Set<QrExpected>>
    fun launchQrScan()
    fun processQrResult(decodedData: String)
}
