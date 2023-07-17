package com.blockchain.presentation.spinner

import androidx.lifecycle.LifecycleOwner
import com.blockchain.analytics.Analytics
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

internal enum class SpinnerAnalyticsState {
    Started, Running, Backgrounded, Ended, Canceled
}

internal class SpinnerAnalyticsTrackerImpl(
    private val screen: SpinnerAnalyticsScreen,
    private val analytics: Analytics,
    private val coroutineScope: CoroutineScope,
    private val coroutineDispatcher: CoroutineDispatcher
) : SpinnerAnalyticsTracker {

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

    private var action: SpinnerAnalyticsAction = SpinnerAnalyticsAction.Default

    override fun updateAction(action: SpinnerAnalyticsAction) {
        this.action = action
    }

    override fun start() {
        if (timerJob?.isActive == true) return

        timerJob?.cancel()
        timerJob = coroutineScope.launch(coroutineDispatcher) {
            timer.collectLatest { elapsedTime ->
                analytics.logEvent(
                    SpinnerAnalyticsEvents.SpinnerState(
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
        val timerJob = timerJob
        if (timerJob == null || timerJob.isCancelled || timerJob.isCompleted) return

        timerJob.cancel()
        analytics.logEvent(
            SpinnerAnalyticsEvents.SpinnerState(
                flowId = flowId,
                duration = elapsedTimeSeconds,
                screen = screen,
                screenEvent = action,
                state = if (isDestroyed) SpinnerAnalyticsState.Canceled else SpinnerAnalyticsState.Ended,
            )
        )
        elapsedTimeSeconds = 0
    }

    private fun backgrounded() {
        val timerJob = timerJob

        if (timerJob == null || timerJob.isCancelled || timerJob.isCompleted) return

        analytics.logEvent(
            SpinnerAnalyticsEvents.SpinnerState(
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

    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)
        backgrounded()
    }
}
