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
            buildTradingGroup(labels, this)
        AssetFilter.Staking ->
            buildStakingGroup(labels, this)
        AssetFilter.Interest ->
            buildInterestGroup(labels, this)
        AssetFilter.Custodial -> buildAllCustodialAccountsGroup(labels, this)
    }.exhaustive

private fun buildAllCustodialAccountsGroup(
    labels: DefaultLabels,
    accountList: List<SingleAccount>
): AccountGroup? {
    val grpAccounts =
        accountList.filterIsInstance<CustodialInterestAccount>() +
            accountList.filterIsInstance<CustodialTradingAccount>() +
            accountList.filterIsInstance<CustodialStakingAccount>()

    return if (grpAccounts.isNotEmpty()) {
        CryptoAccountCustodialGroup(
            label = labels.getDefaultCustodialGroupLabel(),
            accounts = grpAccounts
        )
    } else {
        null
    }
}

private fun buildInterestGroup(
    labels: DefaultLabels,
    accountList: List<SingleAccount>
): AccountGroup? {
    val grpAccounts = accountList.filterIsInstance<CustodialInterestAccount>()
    return if (grpAccounts.isNotEmpty())
        CryptoAccountCustodialSingleGroup(
            labels.getDefaultInterestWalletLabel(), grpAccounts
        )
    else
        null
}

private fun buildTradingGroup(
    labels: DefaultLabels,
    accountList: List<SingleAccount>
): AccountGroup? {
    val grpAccounts = accountList.filterIsInstance<CustodialTradingAccount>()
    return if (grpAccounts.isNotEmpty()) {
        CryptoAccountCustodialSingleGroup(
            labels.getDefaultTradingWalletLabel(), grpAccounts
        )
    } else {
        null
    }
}

private fun buildStakingGroup(
    labels: DefaultLabels,
    accountList: List<SingleAccount>
): AccountGroup? {
    val grpAccounts = accountList.filterIsInstance<CustodialStakingAccount>()
    return if (grpAccounts.isNotEmpty()) {
        CryptoAccountCustodialSingleGroup(
            labels.getDefaultStakingWalletLabel(), grpAccounts
        )
    } else {
        null
    }
}

private fun buildNonCustodialGroup(
    asset: AssetInfo,
    labels: DefaultLabels,
    accountList: List<SingleAccount>
): AccountGroup? {
    val grpAccounts = accountList.filterIsInstance<CryptoNonCustodialAccount>()
    return if (grpAccounts.isNotEmpty()) {
        CryptoAccountNonCustodialGroup(
            asset, labels.getDefaultTradingWalletLabel(), grpAccounts
        )
    } else {
        null
    }
}

private fun buildAssetMasterGroup(
    asset: AssetInfo,
    labels: DefaultLabels,
    accountList: List<SingleAccount>
): AccountGroup? {
    return if (accountList.isEmpty()) {
        null
    } else {
        CryptoAccountNonCustodialGroup(
            asset,
            labels.getAssetMasterWalletLabel(asset),
            accountList
        )
    }
}
