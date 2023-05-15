package piuk.blockchain.android.util

import android.content.res.Resources
import com.blockchain.wallet.DefaultLabels
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.Currency
import info.blockchain.balance.FiatCurrency
import piuk.blockchain.android.R

internal class ResourceDefaultLabels(
    private val resources: Resources
) : DefaultLabels {

    override fun getDefaultNonCustodialWalletLabel(): String =
        resources.getString(
            com.blockchain.stringResources.R.string.default_v2_crypto_non_custodial_wallet_label
        )

    override fun getV0DefaultNonCustodialWalletLabel(asset: AssetInfo): String =
        resources.getString(
            com.blockchain.stringResources.R.string.old_default_non_custodial_wallet_label,
            asset.name
        )

    override fun getV1DefaultNonCustodialWalletLabel(asset: AssetInfo): String =
        resources.getString(
            com.blockchain.stringResources.R.string.default_v1_crypto_non_custodial_wallet_label
        )

    override fun getDefaultTradingWalletLabel(): String {
        return resources.getString(com.blockchain.stringResources.R.string.custodial_wallet_default_label)
    }

    override fun getDefaultFiatWalletLabel(): String =
        "Fiat Accounts"

    override fun getAssetMasterWalletLabel(asset: Currency): String =
        asset.name

    override fun getAllWalletLabel(): String =
        resources.getString(com.blockchain.stringResources.R.string.default_label_all_wallets)

    override fun getAllCustodialWalletsLabel(): String =
        resources.getString(com.blockchain.stringResources.R.string.default_label_all_custodial_wallets)

    override fun getAllNonCustodialWalletsLabel(): String =
        resources.getString(com.blockchain.stringResources.R.string.default_label_all_non_custodial_wallets)

    override fun getDefaultInterestWalletLabel(): String =
        resources.getString(com.blockchain.stringResources.R.string.default_label_rewards_wallet)

    override fun getDefaultExchangeWalletLabel(): String =
        resources.getString(com.blockchain.stringResources.R.string.exchange_default_account_label_1)

    override fun getDefaultStakingWalletLabel(): String =
        resources.getString(com.blockchain.stringResources.R.string.default_label_staking_wallet)

    override fun getDefaultActiveRewardsWalletLabel(): String =
        resources.getString(com.blockchain.stringResources.R.string.default_label_active_rewards_wallet)

    override fun getDefaultCustodialGroupLabel(): String =
        resources.getString(com.blockchain.stringResources.R.string.default_label_custodial_wallets)

    override fun getDefaultCustodialFiatWalletLabel(fiatCurrency: FiatCurrency): String =
        fiatCurrency.name
}
