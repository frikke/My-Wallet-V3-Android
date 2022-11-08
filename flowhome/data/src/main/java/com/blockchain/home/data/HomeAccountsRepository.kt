package com.blockchain.home.data

import com.blockchain.coincore.Coincore
import com.blockchain.coincore.SingleAccount
import com.blockchain.data.DataResource
import com.blockchain.home.domain.HomeAccountsService
import com.blockchain.walletmode.WalletModeService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

class HomeAccountsRepository(
    private val coincore: Coincore,
    private val walletModeService: WalletModeService
) : HomeAccountsService {
    override fun accounts(): Flow<DataResource<List<SingleAccount>>> {
        return walletModeService.walletMode.flatMapLatest { wMode ->
            coincore.activeWalletsInMode(wMode).map { it.accounts }
                .map {
                    DataResource.Data(it) as DataResource<List<SingleAccount>>
                }.onStart {
                    emit(DataResource.Loading)
                }.catch {
                    DataResource.Error(it as Exception)
                }
        }
    }
}
