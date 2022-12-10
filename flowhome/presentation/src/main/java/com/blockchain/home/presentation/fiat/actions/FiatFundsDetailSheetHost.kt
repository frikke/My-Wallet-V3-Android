package com.blockchain.home.presentation.fiat.actions

import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.FiatAccount
import com.blockchain.commonarch.presentation.base.SlidingModalBottomDialog

interface FiatFundsDetailSheetHost : SlidingModalBottomDialog.Host {
    fun goToActivityFor(account: BlockchainAccount)
    fun showFundsKyc()
    fun startBankTransferWithdrawal(fiatAccount: FiatAccount)
    fun startDepositFlow(fiatAccount: FiatAccount)
}
