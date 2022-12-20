package com.blockchain.unifiedcryptowallet.domain.wallet

import com.blockchain.data.DataResource
import kotlinx.coroutines.flow.Flow

/**
 * Todo on a next PR the implementation
 */
interface NetworkWalletService {
    fun networkWalletGroup(currency: String): Flow<DataResource<NetworkWalletGroup>>
    fun networkWalletGroups(): Flow<DataResource<List<NetworkWalletGroup>>>
}
