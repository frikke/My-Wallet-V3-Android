package com.blockchain.presentation.backup

import com.blockchain.analytics.AnalyticsEvent
import com.blockchain.analytics.events.AnalyticsNames

sealed class BackupAnalyticsEvents(
    override val event: String,
    override val params: Map<String, String> = emptyMap()
) : AnalyticsEvent {

    object BackupSkipClicked : BackupAnalyticsEvents(
        event = AnalyticsNames.SUPERAPP_DEFI_BACKUP_SKIP_CLICKED.eventName
    )

    object BackupNowClicked : BackupAnalyticsEvents(
        event = AnalyticsNames.SUPERAPP_DEFI_BACKUP_NOW_CLICKED.eventName
    )

    object BackupToCloudClicked : BackupAnalyticsEvents(
        event = AnalyticsNames.SUPERAPP_DEFI_BACKUP_TO_CLOUD_CLICKED.eventName
    )

    object BackupManuallyClicked : BackupAnalyticsEvents(
        event = AnalyticsNames.SUPERAPP_DEFI_BACKUP_MANUALLY_CLICKED.eventName
    )

    object BackupSuccessfullViewed : BackupAnalyticsEvents(
        event = AnalyticsNames.SUPERAPP_DEFI_BACKUP_SUCCESSFUL_VIEWED.eventName
    )
}
