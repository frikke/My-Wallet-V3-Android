package com.blockchain.store_persisters_persistedjsonsqldelight

import com.blockchain.store_caches_persistedjsonsqldelight.PersistedJsonSqlDelightCacheWiper
import com.blockchain.store_caches_persistedjsonsqldelight.SqlDelightStoreIdScopedPersister
import com.squareup.sqldelight.android.AndroidSqliteDriver
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import store.StorePersisterDataQueries

val storePersistersJsonSqlDelightModule = module {

    single<Database> {
        Database(AndroidSqliteDriver(Database.Schema, androidContext(), "store_persister.db"))
    }

    single<StorePersisterDataQueries> {
        get<Database>().storePersisterDataQueries
    }

    factory<SqlDelightStoreIdScopedPersister> { params ->
        SqlDelightStoreIdScopedPersisterImpl(
            storeId = params.get(),
            storePersisterDataQueries = get()
        )
    }

    single<PersistedJsonSqlDelightCacheWiper> {
        PersistedJsonSqlDelightCacheWiperImpl(get())
    }
}
