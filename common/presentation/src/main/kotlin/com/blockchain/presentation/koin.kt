package com.blockchain.presentation

import com.blockchain.presentation.rx.AndroidMainScheduler
import com.blockchain.rx.IMainScheduler
import org.koin.dsl.module

val commonPresentationModule = module {
    single<IMainScheduler> {
        AndroidMainScheduler
    }
}
