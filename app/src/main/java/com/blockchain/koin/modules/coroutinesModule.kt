package com.blockchain.koin.modules

import com.blockchain.koin.ioDispatcher
import kotlinx.coroutines.Dispatchers
import org.koin.dsl.module

val coroutinesModule = module {

    single(ioDispatcher) { Dispatchers.IO }
}