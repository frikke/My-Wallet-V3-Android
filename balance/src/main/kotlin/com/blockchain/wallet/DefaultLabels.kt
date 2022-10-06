package com.blockchain.wallet

import info.blockchain.balance.AssetInfo
import info.blockchain.balance.Currency
import info.blockchain.balance.FiatCurrency

interface DefaultLabels {

    fun getAllWalletLabel(): String
    fun getAllCustodialWalletsLabel(): String
    fun getAllNonCustodialWalletsLabel(): String
    fun getAssetMasterWalletLabel(asset: Currency): String
    fun getDefaultNonCustodialWalletLabel(): String
    fun getOldDefaultNonCustodialWalletLabel(asset: AssetInfo): String
    fun getDefaultCustodialWalletLabel(): String
    fun getDefaultFiatWalletLabel(): String
    fun getDefaultInterestWalletLabel(): String
    fun getDefaultExchangeWalletLabel(): String
    fun getDefaultStakingWalletLabel(): String
    fun getDefaultCustodialFiatWalletLabel(fiatCurrency: FiatCurrency): String
    fun getDefaultCustodialGroupLabel(): String
}
