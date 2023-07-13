package com.blockchain.presentation.spinner

import com.blockchain.analytics.Analytics
import com.blockchain.spinner.SpinnerAnalyticsAction
import com.blockchain.spinner.SpinnerAnalyticsScreen
import com.blockchain.spinner.SpinnerAnalyticsTimer
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.TimeUnit

enum class SpinnerAnalyticsState {
    Started, Running, Backgrounded, Ended
}

internal class SpinnerAnalyticsTimerImpl(
    private val screen: SpinnerAnalyticsScreen,
    private val analytics: Analytics,
    private val coroutineScope: CoroutineScope,
    private val coroutineDispatcher: CoroutineDispatcher
) : SpinnerAnalyticsTimer {

    private val flowId = UUID.randomUUID().toString()

    private val tickSeconds = 5
    private var elapsedTimeSeconds = 0 // 5 second jumps(tickSeconds), doesnt have to be second exact

    private val timer = flow {
        while (true) {
            emit(elapsedTimeSeconds)
            delay(TimeUnit.SECONDS.toMillis(tickSeconds.toLong()))
            elapsedTimeSeconds += tickSeconds
        }
    }

    private var timerJob: Job? = null

    private lateinit var action: SpinnerAnalyticsAction

    override fun start(
        action: SpinnerAnalyticsAction,
    ) {
        if(timerJob?.isActive == true) return

        this.action = action

        timerJob?.cancel()
        timerJob = coroutineScope.launch(coroutineDispatcher) {
            timer
                .onCompletion {
                    stop()
                }
                .collectLatest { elapsedTime ->
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

    override fun stop() {
        if(timerJob?.isCancelled == true || timerJob?.isCompleted == true) return

        timerJob?.cancel()
        analytics.logEvent(
            SpinnerAnalyticsEvents.SpinnerLaunched(
                flowId = flowId,
                duration = elapsedTimeSeconds,
                screen = screen,
                screenEvent = action,
                state = SpinnerAnalyticsState.Ended,
            )
        )
        elapsedTimeSeconds = 0
    }

    override fun backgrounded() {
        if(timerJob?.isCancelled == true || timerJob?.isCompleted == true) return

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
}
