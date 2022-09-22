package com.blockchain.store_caches_inmemory

import org.koin.dsl.module

val storeCachesInMemoryModule = module {

    single {
        InMemoryCacheWiper(InMemoryCacheProvider)
    }
}
