package com.blockchain.presentation

import com.blockchain.mask.MaskedValueService
import com.blockchain.presentation.maskedvalue.MaskedValueImpl
import com.blockchain.presentation.rx.AndroidMainScheduler
import com.blockchain.rx.IMainScheduler
import org.koin.dsl.module

val commonPresentationModule = module {
    single<IMainScheduler> {
        AndroidMainScheduler
    }

    single<MaskedValueService> {
        MaskedValueImpl(
            prefs = get()
        )
    }
}
