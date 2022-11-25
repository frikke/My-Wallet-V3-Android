package com.blockchain.unifiedcryptowallet.domain.wallet

import com.blockchain.data.DataResource
import com.blockchain.domain.wallet.PubKeyStyle
import com.blockchain.koin.payloadScope
import com.blockchain.unifiedcryptowallet.domain.balances.NetworkBalance
import com.blockchain.unifiedcryptowallet.domain.balances.UnifiedBalancesService
import com.blockchain.wallet.DefaultLabels
import info.blockchain.balance.Currency
import kotlinx.coroutines.flow.Flow
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.parameter.ParametersDefinition
import org.koin.core.qualifier.Qualifier

interface NetworkWallet {
    val label: String
    val currency: Currency
    val index: Int
    val networkBalance: Flow<DataResource<NetworkBalance>>
    val isImported: Boolean
        get() = false

    val pubKeyDescriptor: String
        get() = LEGACY_DESCRIPTOR

    suspend fun publicKey(): List<PublicKey>

    companion object {
        const val DEFAULT_SINGLE_ACCOUNT_INDEX = 0
        const val DEFAULT_ADDRESS_DESCRIPTOR = 0
        const val MULTIPLE_ADDRESSES_DESCRIPTOR = 1
        const val LEGACY_DESCRIPTOR = "legacy"
    }
}

data class PublicKey(
    val address: String,
    /**
     * The descriptor field will need some explanation. Over time some currencies change the
     * way that keys are derived as well as how such keys are used. Most notably, Bitcoin uses
     * a new derivation for SegWit, and the addresses are derived differently, etc etc. A big part of the
     * refactor to add SegWit to our wallet was to add this model, that each account can have multiple xpubs,
     * and this descriptor field is that same abstraction. One needs to continue to monitor all addresses,
     * as previous old addresses may receive funds in the future.
     */
    val descriptor: Int,

    val style: PubKeyStyle
)

class CryptoNetworkWallet(
    override val currency: Currency,
    override val index: Int,
    private val config: NetworkConfig,
    private val pubKeyService: PublicKeyService,
    private val _label: String? = null
) : NetworkWallet, KoinComponent {
    private val defaultLabels: DefaultLabels by inject()
    private val balancesService: UnifiedBalancesService by scopedInject()

    override val networkBalance: Flow<DataResource<NetworkBalance>>
        get() =
            balancesService.balanceForWallet(this@CryptoNetworkWallet)

    override val label: String
        get() = _label ?: defaultLabels.getAllNonCustodialWalletsLabel()

    override suspend fun publicKey(): List<PublicKey> = pubKeyService.publicKey().map {
        PublicKey(
            address = it,
            descriptor = config.descriptor,
            style = config.style,
        )
    }
}

class NetworkWalletGroup(
    private val parentChainNetwork: NetworkWallet,
    val name: String,
    private val networkWallets: List<NetworkWallet>
) : KoinComponent {

    private val balancesService: UnifiedBalancesService by scopedInject()

    suspend fun publicKey(): List<PublicKey> {
        return parentChainNetwork.publicKey()
    }

    fun pubKeyStyle(): PubKeyStyle = PubKeyStyle.SINGLE

    fun pubKeyDescriptor(): String = parentChainNetwork.pubKeyDescriptor

    fun getNetworkWallet(currency: Currency): NetworkWallet? {
        return networkWallets.firstOrNull { it.currency.networkTicker == currency.networkTicker }
    }
}

data class NetworkConfig(val descriptor: Int, val style: PubKeyStyle)

interface PublicKeyService {
    suspend fun publicKey(): List<String>
}

inline fun <reified T> KoinComponent.scopedInject(
    qualifier: Qualifier? = null,
    noinline parameters: ParametersDefinition? = null
): Lazy<T> =
    lazy(LazyThreadSafetyMode.SYNCHRONIZED) { payloadScope.get(qualifier, parameters) }
