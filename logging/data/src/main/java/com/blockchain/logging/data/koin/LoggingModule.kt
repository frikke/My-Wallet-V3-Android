package com.blockchain.logging.data.koin

import com.blockchain.logging.EventLogger
import com.blockchain.logging.ILogger
import com.blockchain.logging.NullLogger
import com.blockchain.logging.RemoteLogger
import com.blockchain.logging.data.BuildConfig
import com.blockchain.logging.data.CompoundRemoteLogger
import com.blockchain.logging.data.FirebaseRemoteLogger
import com.blockchain.logging.data.InjectableLogging
import com.blockchain.logging.data.TimberLogger
import org.koin.dsl.bind
import org.koin.dsl.module

val loggingModule = module {
    single {
        CompoundRemoteLogger(
            listOf(
                FirebaseRemoteLogger(),
            ),
            get()
        )
    }.bind(RemoteLogger::class)

    single {
        if (BuildConfig.DEBUG) {
            TimberLogger()
        } else {
            NullLogger
        }
    }.bind(ILogger::class)

    factory {
        InjectableLogging(get())
    }.bind(EventLogger::class)
}
