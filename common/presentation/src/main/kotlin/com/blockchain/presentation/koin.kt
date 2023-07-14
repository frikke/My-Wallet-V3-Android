package com.blockchain.presentation

import com.blockchain.mask.MaskedValueService
import com.blockchain.presentation.maskedvalue.MaskedValueImpl
import com.blockchain.presentation.rx.AndroidMainScheduler
import com.blockchain.presentation.spinner.SpinnerAnalyticsScreen
import com.blockchain.presentation.spinner.SpinnerAnalyticsTimer
import com.blockchain.presentation.spinner.SpinnerAnalyticsTimerImpl
import com.blockchain.presentation.theme.ThemeRepository
import com.blockchain.rx.IMainScheduler
import com.blockchain.theme.ThemeService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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

    single<ThemeService> {
        ThemeRepository(
            themePrefs = get()
        )
    }

    factory<SpinnerAnalyticsTimer> { (screen: SpinnerAnalyticsScreen, coroutineScope: CoroutineScope) ->
        SpinnerAnalyticsTimerImpl(
            screen = screen,
            analytics = get(),
            coroutineScope = coroutineScope,
            coroutineDispatcher = Dispatchers.IO
        )
    }
}
