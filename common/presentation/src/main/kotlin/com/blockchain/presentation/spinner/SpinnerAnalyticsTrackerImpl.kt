package com.blockchain.presentation.spinner

import com.blockchain.analytics.Analytics
import com.blockchain.domain.experiments.RemoteConfigService
import com.blockchain.outcome.getOrDefault
import com.blockchain.utils.awaitOutcome
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal class SpinnerAnalyticsTrackerImpl(
    private val screen: SpinnerAnalyticsScreen,
    private val analytics: Analytics,
    private val coroutineScope: CoroutineScope,
    private val coroutineDispatcher: CoroutineDispatcher,
    private val remoteConfigService: RemoteConfigService,
) : SpinnerAnalyticsTracker {

    companion object {
        private const val TIMEOUT_KEY = "blockchain_app_configuration_spinner_timeout"
    }

    private var timerJob: Job? = null
    private var timeout: Int? = null

    override fun start() {
        if (timerJob?.isActive == true) return

        timerJob?.cancel()
        timerJob = coroutineScope.launch(coroutineDispatcher) {
            delay(TimeUnit.SECONDS.toMillis(getTimeout().toLong()))

            analytics.logEvent(
                SpinnerAnalyticsEvents.SpinnerTimeout(
                    duration = getTimeout(),
                    screen = screen,
                )
            )
        }
    }

    override fun stop() {
        val timerJob = timerJob
        if (timerJob == null || timerJob.isCancelled || timerJob.isCompleted) return

        timerJob.cancel()
    }

    private suspend fun getTimeout(): Int {
        return timeout ?: remoteConfigService.getRawJson(TIMEOUT_KEY)
            .awaitOutcome().getOrDefault("5").toIntOrNull() ?: 5
            .also { timeout = it }
    }
}
