package com.blockchain.presentation.pulltorefresh

import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.RefreshStrategy
import com.blockchain.utils.CurrentTimeProvider
import java.util.concurrent.TimeUnit

object PullToRefresh {
    private val PULL_TO_REFRESH_THRESHOLD = TimeUnit.MINUTES.toMillis(2)

    fun canRefresh(lastFreshTime: Long): Boolean {
        return CurrentTimeProvider.currentTimeMillis() - lastFreshTime > PULL_TO_REFRESH_THRESHOLD
    }

    fun freshnessStrategy(
        shouldGetFresh: Boolean,
        cacheStrategy: RefreshStrategy = RefreshStrategy.RefreshIfOlderThan(amount = 5, unit = TimeUnit.MINUTES)
    ): FreshnessStrategy {
        return if (shouldGetFresh) {
            FreshnessStrategy.Fresh
        } else {
            FreshnessStrategy.Cached(cacheStrategy)
        }
    }
}
