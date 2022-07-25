package com.blockchain.nfts.koin

import com.blockchain.koin.payloadScopeQualifier
import org.koin.dsl.module

val nftPresentationModule = module {
    scope(payloadScopeQualifier) {
    }
}
