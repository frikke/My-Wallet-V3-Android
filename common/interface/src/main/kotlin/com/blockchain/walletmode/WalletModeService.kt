package com.blockchain.walletmode

import info.blockchain.balance.AssetCategory

interface WalletModeService {
    fun enabledWalletTypes(): Set<AssetCategory>
    fun updateEnabledWalletTypes(types: Set<AssetCategory>)
}
