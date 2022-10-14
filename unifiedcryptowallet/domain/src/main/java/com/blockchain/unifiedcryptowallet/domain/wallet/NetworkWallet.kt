package com.blockchain.unifiedcryptowallet.domain.wallet

import com.blockchain.koin.payloadScope
import com.blockchain.unifiedcryptowallet.domain.balances.NetworkBalance
import com.blockchain.unifiedcryptowallet.domain.balances.UnifiedBalancesService
import com.blockchain.wallet.DefaultLabels
import info.blockchain.balance.Currency
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.parameter.ParametersDefinition
import org.koin.core.qualifier.Qualifier

interface NetworkWallet {
    val label: String
    val currency: Currency
    val index: Int
    val networkBalance: Flow<NetworkBalance>

    /**
     * The descriptor field will need some explanation. Over time some currencies change the
     * way that keys are derived as well as how such keys are used. Most notably, Bitcoin uses
     * a new derivation for SegWit, and the addresses are derived differently, etc etc. A big part of the
     * refactor to add SegWit to our wallet was to add this model, that each account can have multiple xpubs,
     * and this descriptor field is that same abstraction. One needs to continue to monitor all addresses,
     * as previous old addresses may receive funds in the future.
     */
    val descriptor: Int
        get() = DEFAULT_ADDRESS_DESCRIPTOR

    val style: String
        get() = SINGLE_PUB_KEY_STYLE

    suspend fun publicKey(): String

    companion object {
        const val SINGLE_PUB_KEY_STYLE = "SINGLE"
        const val EXTENDED_PUB_KEY_STYLE = "EXTENDED"
        const val DEFAULT_SINGLE_ACCOUNT_INDEX = 0
        const val DEFAULT_ADDRESS_DESCRIPTOR = 0
        const val MULTIPLE_ADDRESSES_DESCRIPTOR = 1
    }
}

class CryptoNetworkWallet(
    override val currency: Currency,
    override val index: Int,
    private val config: NetworkConfig,
    private val pubKeyService: PublicKeyService,
    private val _label: String? = null
) : NetworkWallet, KoinComponent {
    private val defaultLabels: DefaultLabels by inject()
    private val balancesService: UnifiedBalancesService by scopedInject()

    override val networkBalance: Flow<NetworkBalance>
        get() = flow {
            emit(balancesService.balanceForWallet(this@CryptoNetworkWallet))
        }

    override val label: String
        get() = _label ?: defaultLabels.getAllNonCustodialWalletsLabel()

    override val descriptor: Int
        get() = config.descriptor

    override val style: String
        get() = config.style

    override suspend fun publicKey(): String = pubKeyService.publicKey()
}

class NetworkWalletGroup(
    private val parentChainNetwork: NetworkWallet
) : KoinComponent {

    private val balancesService: UnifiedBalancesService by scopedInject()

    suspend fun publicKey(): String {
        return parentChainNetwork.publicKey()
    }

    val name: String
        get() = throw NotImplementedError()

    val networkWallets: List<NetworkWallet>
        get() = throw NotImplementedError()

    fun getNetworkWallet(currency: Currency): NetworkWallet {
        throw NotImplementedError()
    }
}

data class NetworkConfig(val descriptor: Int, val style: String)

interface PublicKeyService {
    suspend fun publicKey(): String
}

inline fun <reified T> KoinComponent.scopedInject(
    qualifier: Qualifier? = null,
    noinline parameters: ParametersDefinition? = null
): Lazy<T> =
    lazy(LazyThreadSafetyMode.SYNCHRONIZED) { payloadScope.get(qualifier, parameters) }
