package com.blockchain.coincore.impl

import com.blockchain.coincore.AccountGroup
import com.blockchain.coincore.AssetFilter
import com.blockchain.coincore.SingleAccount
import com.blockchain.coincore.SingleAccountList
import com.blockchain.extensions.exhaustive
import com.blockchain.wallet.DefaultLabels
import info.blockchain.balance.AssetInfo

fun SingleAccountList.makeAccountGroup(
    asset: AssetInfo,
    labels: DefaultLabels,
    assetFilter: AssetFilter
): AccountGroup? =
    when (assetFilter) {
        AssetFilter.All ->
            buildAssetMasterGroup(asset, labels, this)
        AssetFilter.NonCustodial ->
            buildNonCustodialGroup(asset, labels, this)
        AssetFilter.Trading ->
            buildCustodialGroup(labels, this)
        AssetFilter.Interest ->
            buildInterestGroup(labels, this)
        AssetFilter.Custodial -> buildCustodialPlusInterestGroup(labels, this)
    }.exhaustive

private fun buildCustodialPlusInterestGroup(
    labels: DefaultLabels,
    accountList: List<SingleAccount>
): AccountGroup? {
    val grpAccounts =
        accountList.filterIsInstance<CryptoInterestAccount>() + accountList.filterIsInstance<CustodialTradingAccount>()

    return if (grpAccounts.isNotEmpty())
        CryptoAccountCustodialGroup(
            label = labels.getDefaultInterestWalletLabel(),
            accounts = grpAccounts
        )
    else
        null
}

private fun buildInterestGroup(
    labels: DefaultLabels,
    accountList: List<SingleAccount>
): AccountGroup? {
    val grpAccounts = accountList.filterIsInstance<CryptoInterestAccount>()
    return if (grpAccounts.isNotEmpty())
        CryptoAccountTradingGroup(
            labels.getDefaultInterestWalletLabel(), grpAccounts
        )
    else
        null
}

private fun buildCustodialGroup(
    labels: DefaultLabels,
    accountList: List<SingleAccount>
): AccountGroup? {
    val grpAccounts = accountList.filterIsInstance<CustodialTradingAccount>()
    return if (grpAccounts.isNotEmpty())
        CryptoAccountTradingGroup(
            labels.getDefaultCustodialWalletLabel(), grpAccounts
        )
    else
        null
}

private fun buildNonCustodialGroup(
    asset: AssetInfo,
    labels: DefaultLabels,
    accountList: List<SingleAccount>
): AccountGroup? {
    val grpAccounts = accountList.filterIsInstance<CryptoNonCustodialAccount>()
    return if (grpAccounts.isNotEmpty())
        CryptoAccountNonCustodialGroup(
            asset, labels.getDefaultCustodialWalletLabel(), grpAccounts
        )
    else
        null
}

private fun buildAssetMasterGroup(
    asset: AssetInfo,
    labels: DefaultLabels,
    accountList: List<SingleAccount>
): AccountGroup? {
    return if (accountList.isEmpty())
        null
    else
        CryptoAccountNonCustodialGroup(
            asset,
            labels.getAssetMasterWalletLabel(asset),
            accountList
        )
}
