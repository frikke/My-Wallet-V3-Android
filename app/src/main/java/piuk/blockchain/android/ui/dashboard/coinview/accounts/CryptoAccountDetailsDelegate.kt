package piuk.blockchain.android.ui.dashboard.coinview.accounts

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.text.buildAnnotatedString
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.coincore.AccountGroup
import com.blockchain.coincore.ActionState
import com.blockchain.coincore.AssetFilter
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.eth.MultiChainAccount
import com.blockchain.coincore.selectFirstAccount
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.viewextensions.gone
import com.blockchain.componentlib.viewextensions.visible
import com.blockchain.componentlib.viewextensions.visibleIf
import com.blockchain.wallet.DefaultLabels
import info.blockchain.balance.Currency
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.ViewCoinviewWalletsBinding
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.ui.dashboard.coinview.AssetDetailsItem
import piuk.blockchain.android.ui.resources.AccountIcon
import piuk.blockchain.android.ui.resources.AssetResources
import java.text.DecimalFormat

/**
 * @property allowWalletsLabel weather or not to show labels, e.g. 'Wallets & Accounts'
 * @property showOnlyAssetInfo if true shows Network/Ticket, else Wallet name/Network,
 * @property allowEmbeddedCta weather or not to show CopyAddress/Receive buttons
 */
class CryptoAccountDetailsDelegate(
    private val onAccountSelected: (AssetDetailsItem.CryptoDetailsInfo) -> Unit,
    private val onCopyAddressClicked: (CryptoAccount) -> Unit,
    private val onReceiveClicked: (BlockchainAccount) -> Unit,
    private val onLockedAccountSelected: () -> Unit,
    private val labels: DefaultLabels,
    private val assetResources: AssetResources,
    private val allowWalletsLabel: Boolean,
    private val showOnlyAssetInfo: Boolean,
    private val allowEmbeddedCta: Boolean
) : AdapterDelegate<AssetDetailsItem> {
    override fun isForViewType(items: List<AssetDetailsItem>, position: Int): Boolean =
        items[position] is AssetDetailsItem.CryptoDetailsInfo

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        AssetWalletViewHolder(
            ViewCoinviewWalletsBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            onAccountSelected = onAccountSelected,
            onCopyAddressClicked = onCopyAddressClicked,
            onReceiveClicked = onReceiveClicked,
            onLockedAccountSelected = onLockedAccountSelected,
            labels = labels,
            assetResources = assetResources,
            allowWalletsLabel = allowWalletsLabel,
            showOnlyAssetInfo = showOnlyAssetInfo,
            allowEmbeddedCta = allowEmbeddedCta,
        )

    override fun onBindViewHolder(
        items: List<AssetDetailsItem>,
        position: Int,
        holder: RecyclerView.ViewHolder
    ) = (holder as AssetWalletViewHolder).bind(
        item = items[position] as AssetDetailsItem.CryptoDetailsInfo,
        isFirstItemOfCategory = items.indexOfFirst { it is AssetDetailsItem.CryptoDetailsInfo } == position,
        isOnlyItemOfCategory = items.count { it is AssetDetailsItem.CryptoDetailsInfo } == 1
    )
}

private class AssetWalletViewHolder(
    private val binding: ViewCoinviewWalletsBinding,
    private val onAccountSelected: (AssetDetailsItem.CryptoDetailsInfo) -> Unit,
    private val onCopyAddressClicked: (CryptoAccount) -> Unit,
    private val onReceiveClicked: (BlockchainAccount) -> Unit,
    private val onLockedAccountSelected: () -> Unit,
    private val labels: DefaultLabels,
    private val assetResources: AssetResources,
    private val allowWalletsLabel: Boolean,
    private val showOnlyAssetInfo: Boolean,
    private val allowEmbeddedCta: Boolean,
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(
        item: AssetDetailsItem.CryptoDetailsInfo,
        isFirstItemOfCategory: Boolean,
        isOnlyItemOfCategory: Boolean
    ) {
        val asset = getAsset(item.account, item.balance.currencyCode)

        with(binding) {
            assetDetailsAvailable.onClick = {
                onAccountSelected(item)
            }
            assetDetailsNotAvailable.onClick = {
                onLockedAccountSelected()
            }
            val walletLabel = when (item.assetFilter) {
                AssetFilter.NonCustodial -> item.account.label
                AssetFilter.Trading -> labels.getDefaultCustodialWalletLabel()
                AssetFilter.Interest -> labels.getDefaultInterestWalletLabel()
                else -> throw IllegalArgumentException("Filer not supported for account label")
            }
            val account = when (item.account) {
                is CryptoAccount -> item.account
                is AccountGroup -> item.account.selectFirstAccount()
                else -> throw IllegalStateException(
                    "Unsupported account type for asset details ${item.account}"
                )
            }
            val accountIcon = AccountIcon(account, assetResources)

            if (item.actions.any { it.state == ActionState.Available }) {
                assetDetailsNotAvailable.gone()
                assetDetailsAvailable.apply {
                    visible()
                    titleStart = buildAnnotatedString {
                        append(
                            if (showOnlyAssetInfo) account.currency.name
                            else walletLabel
                        )
                    }
                    titleEnd = buildAnnotatedString { append(item.fiatBalance.toStringWithSymbol()) }
                    bodyEnd = buildAnnotatedString { append(item.balance.toStringWithSymbol()) }
                    bodyStart = buildAnnotatedString {
                        append(
                            if (showOnlyAssetInfo) {
                                account.currency.displayTicker
                            } else {
                                when (item.assetFilter) {
                                    AssetFilter.NonCustodial -> {
                                        if (account is MultiChainAccount) {
                                            context.getString(
                                                R.string.coinview_multi_nc_desc,
                                                account.l1Network.networkName
                                            )
                                        } else {
                                            context.getString(R.string.coinview_nc_desc)
                                        }
                                    }
                                    AssetFilter.Trading -> context.getString(R.string.coinview_c_available_desc)
                                    AssetFilter.Interest -> {
                                        val interestFormat = DecimalFormat("0.#")
                                        val interestRate = interestFormat.format(item.interestRate)
                                        binding.root.context.getString(
                                            R.string.coinview_interest_with_balance, interestRate
                                        )
                                    }
                                    else -> throw IllegalArgumentException("Not a supported filter")
                                }
                            }
                        )
                    }

                    if (showOnlyAssetInfo) {
                        startImageResource = ImageResource.Remote(url = account.currency.logo, shape = CircleShape)
                    } else {
                        accountIcon.indicator?.let {
                            startImageResource =
                                ImageResource.LocalWithBackgroundAndExternalResources(it, asset.colour, "#FFFFFF", 1F)
                        }
                    }
                }

                if (allowEmbeddedCta && isOnlyItemOfCategory) {
                    ctaContainer.visible()
                    copyAddressButton.apply {
                        text = context.getString(R.string.copy_address)
                        icon = ImageResource.Local(R.drawable.ic_copy)
                        onClick = { onCopyAddressClicked(account) }
                    }
                    receiveButton.apply {
                        text = context.getString(R.string.common_receive)
                        icon = ImageResource.Local(R.drawable.ic_qr_scan)
                        onClick = { onReceiveClicked(item.account) }
                    }
                } else {
                    ctaContainer.gone()
                }
            } else {
                assetDetailsAvailable.gone()
                assetDetailsNotAvailable.apply {
                    visible()
                    primaryText = walletLabel
                    secondaryText = when (item.assetFilter) {
                        AssetFilter.NonCustodial -> context.getString(R.string.coinview_nc_desc)
                        AssetFilter.Trading -> context.getString(R.string.coinview_c_unavailable_desc, asset.name)
                        AssetFilter.Interest -> {
                            val interestFormat = DecimalFormat("0.#")
                            val interestRate = interestFormat.format(item.interestRate)
                            context.getString(
                                R.string.coinview_interest_no_balance, interestRate
                            )
                        }
                        else -> throw IllegalArgumentException("Not a supported filter")
                    }
                    if (showOnlyAssetInfo) {
                        startImageResource = ImageResource.Remote(url = account.currency.logo, shape = CircleShape)
                    } else {
                        accountIcon.indicator?.let {
                            startImageResource =
                                ImageResource.LocalWithBackground(it, R.color.grey_400, R.color.white, 1F)
                        }
                    }
                    endImageResource =
                        ImageResource.LocalWithBackground(R.drawable.ic_lock, R.color.grey_400, R.color.white, 1F)
                }

                ctaContainer.gone()
            }

            if (allowWalletsLabel) {
                walletsLabel.apply {
                    visibleIf { isFirstItemOfCategory }
                    title = context.getString(R.string.coinview_accounts_label)
                }
            }
        }
    }

    private fun getAsset(account: BlockchainAccount, currency: String): Currency =
        when (account) {
            is CryptoAccount -> account.currency
            is AccountGroup -> account.accounts.filterIsInstance<CryptoAccount>()
                .firstOrNull()?.currency ?: throw IllegalStateException(
                "No crypto accounts found in ${this::class.java} with currency $currency "
            )
            else -> null
        } ?: throw IllegalStateException("Unsupported account type ${this::class.java}")
}

private fun List<AssetDetailsItem>.indexOfFirstItemOfCategory(category: AssetFilter) = indexOfFirst { item ->
    item is AssetDetailsItem.CryptoDetailsInfo && item.assetFilter == category
}
