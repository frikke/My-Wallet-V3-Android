package com.blockchain.home.data

import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.Coincore
import com.blockchain.data.DataResource
import com.blockchain.home.domain.HomeAccountsService
import com.blockchain.walletmode.WalletModeService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

class HomeAccountsRepository(private val coincore: Coincore, private val walletModeService: WalletModeService) :
    HomeAccountsService {
    override fun accounts(): Flow<DataResource<List<BlockchainAccount>>> {
        return walletModeService.walletMode.flatMapLatest { wMode ->
            coincore.activeWalletsInMode(wMode)
                .map {
                    DataResource.Data(it.accounts) as DataResource<List<BlockchainAccount>>
                }.onStart {
                    emit(DataResource.Loading)
                }.catch {
                    DataResource.Error(it as Exception)
                }
        }
    }
}
