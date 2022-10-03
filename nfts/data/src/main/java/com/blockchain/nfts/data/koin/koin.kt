package com.blockchain.nfts.data.koin

import com.blockchain.koin.payloadScopeQualifier
import org.koin.dsl.module

val nftDataModule = module {
    scope(payloadScopeQualifier) {
    }
}
