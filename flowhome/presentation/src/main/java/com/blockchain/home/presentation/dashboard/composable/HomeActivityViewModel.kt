package com.blockchain.home.presentation.dashboard.composable

import androidx.lifecycle.ViewModel
import com.blockchain.home.presentation.SectionSize
import com.blockchain.home.presentation.activity.list.ActivityIntent
import com.blockchain.home.presentation.activity.list.ActivityViewState
import com.blockchain.home.presentation.activity.list.custodial.CustodialActivityViewModel
import com.blockchain.home.presentation.activity.list.privatekey.PrivateKeyActivityViewModel
import com.blockchain.walletmode.WalletMode
import com.blockchain.walletmode.WalletModeService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onEach

class HomeActivityViewModel(
    private val pkwViewmodel: PrivateKeyActivityViewModel,
    private val custodialActivityViewModel: CustodialActivityViewModel,
    private val walletModeService: WalletModeService
) : ViewModel() {

    @OptIn(ExperimentalCoroutinesApi::class)
    fun state(): Flow<ActivityViewState> =
        walletModeService.walletMode.onEach {
            when (it) {
                WalletMode.CUSTODIAL -> {
                    custodialActivityViewModel.onIntent(
                        ActivityIntent.LoadActivity(SectionSize.Limited(MAX_ACTIVITY_COUNT))
                    )
                }
                WalletMode.NON_CUSTODIAL -> {
                    pkwViewmodel.onIntent(
                        ActivityIntent.LoadActivity(SectionSize.Limited(MAX_ACTIVITY_COUNT))
                    )
                }
            }
        }.flatMapLatest { wMode ->
            when (wMode) {
                WalletMode.CUSTODIAL -> {
                    custodialActivityViewModel.viewState
                }
                WalletMode.NON_CUSTODIAL -> {

                    pkwViewmodel.viewState
                }
            }
        }
}

private const val MAX_ACTIVITY_COUNT = 5
