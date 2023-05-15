package piuk.blockchain.android.ui.dashboard.announcements

import com.blockchain.preferences.SessionPrefs

/**
 * Maintains a boolean flag for recording if a dialog has been dismissed.
 */

enum class DismissRule {
    CardPersistent,
    CardPeriodic,
    CardOneTime
}

interface DismissClock {
    fun now(): Long
}

class DismissRecorder(
    private val prefs: SessionPrefs,
    private val clock: DismissClock
) {
    operator fun get(key: String) = DismissEntry(key)

    inner class DismissEntry(val prefsKey: String) {
        val isDismissed: Boolean
            get() = isDismissed(prefsKey)

        fun dismiss(dismissRule: DismissRule) {
            when (dismissRule) {
                DismissRule.CardPersistent -> dismissForever(prefsKey)
                DismissRule.CardPeriodic -> dismissPeriodic(prefsKey)
                DismissRule.CardOneTime -> dismissForever(prefsKey)
            }
        }

        fun done() {
            dismissForever(prefsKey)
        }
    }

    fun dismissPeriodic(prefsKey: String) {
        prefs.deleteDismissalRecord(prefsKey) // In case there is a legacy setting
        prefs.recordDismissal(prefsKey, DISMISS_INTERVAL_PERIODIC)
    }

    fun dismissPeriodic(prefsKey: String, period: Long) {
        prefs.deleteDismissalRecord(prefsKey) // In case there is a legacy setting
        prefs.recordDismissal(prefsKey, clock.now() + period)
    }

    fun dismissForever(prefsKey: String) {
        prefs.deleteDismissalRecord(prefsKey) // In case there is a legacy setting
        prefs.recordDismissal(prefsKey, DISMISS_INTERVAL_FOREVER)
    }

    fun isDismissed(prefsKey: String): Boolean =
        try {
            val nextShow = prefs.getDismissalEntry(prefsKey)
            val now = clock.now()

            nextShow != 0L && now <= nextShow
        } catch (e: ClassCastException) {
            // Try the legacy key
            prefs.getLegacyDismissalEntry(prefsKey)
        }
    private var interval = ONE_WEEK

    @Suppress("PrivatePropertyName")
    private val DISMISS_INTERVAL_PERIODIC: Long
        get() = clock.now() + interval

    companion object {
        const val UPSELL_ANOTHER_ASSET_DISMISS_KEY = "UPSELL_ANOTHER_ASSET_DISMISSED"

        const val ONE_WEEK = 7L * 24L * 60L * 60L * 1000L
        const val ONE_MONTH = 30L * 24L * 60L * 60L * 1000L
        private const val DISMISS_INTERVAL_FOREVER = Long.MAX_VALUE
    }
}
