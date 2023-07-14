package com.blockchain.presentation.spinner

import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.LifecycleOwner
import com.blockchain.analytics.Analytics
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.TimeUnit

internal enum class SpinnerAnalyticsState {
    Started, Running, Backgrounded, Ended, Canceled
}

internal class SpinnerAnalyticsTimerImpl(
    private val screen: SpinnerAnalyticsScreen,
    private val analytics: Analytics,
    private val coroutineScope: CoroutineScope,
    private val coroutineDispatcher: CoroutineDispatcher
) : SpinnerAnalyticsTimer {

    private val flowId = UUID.randomUUID().toString()

    private val tickSeconds = 5
    private var elapsedTimeSeconds = 0

    private val timer = flow {
        while (true) {
            emit(elapsedTimeSeconds)
            delay(TimeUnit.SECONDS.toMillis(1L))
            elapsedTimeSeconds++
        }
    }.filter { it % tickSeconds == 0 }

    private var timerJob: Job? = null

    private lateinit var action: SpinnerAnalyticsAction

    override fun start(action: SpinnerAnalyticsAction) {
        if (timerJob?.isActive == true) return

        this.action = action

        timerJob?.cancel()
        timerJob = coroutineScope.launch(coroutineDispatcher) {
            timer.collectLatest { elapsedTime ->
                    analytics.logEvent(
                        SpinnerAnalyticsEvents.SpinnerLaunched(
                            flowId = flowId,
                            duration = elapsedTime,
                            screen = screen,
                            screenEvent = action,
                            state = if (elapsedTime == 0) SpinnerAnalyticsState.Started else SpinnerAnalyticsState.Running,
                        )
                    )
                }
        }
    }

    override fun stop(isDestroyed: Boolean) {
        if (timerJob?.isCancelled == true || timerJob?.isCompleted == true || !::action.isInitialized) return

        timerJob?.cancel()
        analytics.logEvent(
            SpinnerAnalyticsEvents.SpinnerLaunched(
                flowId = flowId,
                duration = elapsedTimeSeconds,
                screen = screen,
                screenEvent = action,
                state = if(isDestroyed) SpinnerAnalyticsState.Canceled else SpinnerAnalyticsState.Ended,
            )
        )
        elapsedTimeSeconds = 0
    }

    override fun backgrounded() {
        if (timerJob?.isCancelled == true || timerJob?.isCompleted == true || !::action.isInitialized) return

        analytics.logEvent(
            SpinnerAnalyticsEvents.SpinnerLaunched(
                flowId = flowId,
                duration = elapsedTimeSeconds,
                screen = screen,
                screenEvent = action,
                state = SpinnerAnalyticsState.Backgrounded,
            )
        )
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        stop(isDestroyed = true)
    }
}
