package com.blockchain.home.presentation.accouncement

import androidx.lifecycle.viewModelScope
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.icons.Unlock
import com.blockchain.componentlib.theme.Pink600
import com.blockchain.componentlib.utils.ImageValue
import com.blockchain.componentlib.utils.TextValue
import com.blockchain.data.DataResource
import com.blockchain.data.RefreshStrategy
import com.blockchain.data.dataOrElse
import com.blockchain.data.filter
import com.blockchain.data.map
import com.blockchain.data.updateDataWith
import com.blockchain.defiwalletbackup.domain.service.BackupPhraseService
import com.blockchain.extensions.minus
import com.blockchain.featureflag.FeatureFlag
import com.blockchain.home.announcements.Announcement
import com.blockchain.home.announcements.AnnouncementsService
import com.blockchain.home.announcements.ConsumeAnnouncementAction
import com.blockchain.home.presentation.dashboard.HomeNavEvent
import com.blockchain.presentation.pulltorefresh.PullToRefresh
import com.blockchain.walletmode.WalletMode
import com.blockchain.walletmode.WalletModeService
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AnnouncementsViewModel(
    private val walletModeService: WalletModeService,
    private val backupPhraseService: BackupPhraseService,
    private val announcementsService: AnnouncementsService,
    private val iterableAnnouncementsFF: FeatureFlag
) : MviViewModel<
    AnnouncementsIntent,
    AnnouncementsViewState,
    AnnouncementModelState,
    HomeNavEvent,
    ModelConfigArgs.NoArgs
    >(
    AnnouncementModelState()
) {
    private var remoteAnnouncementsJob: Job? = null
    private var remoteAnnouncementsConfirmationJob: Job? = null
    private var localAnnouncementsJob: Job? = null

    override fun viewCreated(args: ModelConfigArgs.NoArgs) {}

    override fun AnnouncementModelState.reduce() = AnnouncementsViewState(
        remoteAnnouncements = remoteAnnouncements.forMode(walletMode),
        hideAnnouncementsConfirmation = hideAnnouncementsConfirmation,
        animateHideAnnouncementsConfirmation = animateHideAnnouncementsConfirmation,
        localAnnouncements = localAnnouncements
    )

    override suspend fun handleIntent(modelState: AnnouncementModelState, intent: AnnouncementsIntent) {
        when (intent) {
            is AnnouncementsIntent.LoadAnnouncements -> {
                updateState {
                    copy(walletMode = intent.walletMode)
                }
                updateRemoteAnnouncementsConfirmation(
                    announcements = modelState.remoteAnnouncements,
                    withDelay = false
                )

                loadRemoteAnnouncements(forceRefresh = false)
                loadLocalAnnouncements()
            }

            is AnnouncementsIntent.DeleteAnnouncement -> {
                updateRemoteAnnouncementsConfirmation(
                    announcements = modelState.remoteAnnouncements.remove(intent.announcement)
                )

                viewModelScope.launch {
                    announcementsService.consumeAnnouncement(
                        announcement = intent.announcement,
                        action = ConsumeAnnouncementAction.DELETED
                    )
                }
            }

            is AnnouncementsIntent.AnnouncementClicked -> {
                updateRemoteAnnouncementsConfirmation(
                    announcements = modelState.remoteAnnouncements.remove(intent.announcement)
                )

                viewModelScope.launch {
                    // track clicked + consume so backend removes it from future responses
                    announcementsService.trackClicked(
                        announcement = intent.announcement
                    )

                    announcementsService.consumeAnnouncement(
                        announcement = intent.announcement,
                        action = ConsumeAnnouncementAction.CLICKED
                    )
                }
            }

            AnnouncementsIntent.Refresh -> {
                loadRemoteAnnouncements(forceRefresh = true)
            }
        }
    }

    private fun loadRemoteAnnouncements(forceRefresh: Boolean) {
        remoteAnnouncementsJob?.cancel()
        remoteAnnouncementsJob = viewModelScope.launch {
            if (iterableAnnouncementsFF.coEnabled()) {
                announcementsService.announcements(
                    PullToRefresh.freshnessStrategy(
                        shouldGetFresh = forceRefresh,
                        RefreshStrategy.RefreshIfOlderThan(amount = 15, unit = TimeUnit.MINUTES)
                    )
                ).collectLatest { dataResource ->
                    val sorted = dataResource.map {
                        it.sortedWith(
                            compareBy<Announcement> { it.priority }
                                .thenByDescending { it.createdAt }
                        )
                    }
                    updateState {
                        copy(remoteAnnouncements = remoteAnnouncements.updateDataWith(sorted))
                    }

                    updateRemoteAnnouncementsConfirmation(
                        announcements = sorted.forMode(modelState.walletMode)
                    )

                    // mark latest as seen since it's shown first
                    (sorted.forMode(modelState.walletMode).map { it.lastOrNull() } as? DataResource.Data)
                        ?.data?.let {
                            announcementsService.trackSeen(it)
                        }
                }
            } else {
                updateState {
                    copy(remoteAnnouncements = DataResource.Data(emptyList()))
                }
            }
        }
    }

    private fun updateRemoteAnnouncementsConfirmation(
        announcements: DataResource<List<Announcement>>,
        withDelay: Boolean = true
    ) {
        remoteAnnouncementsConfirmationJob?.cancel()
        remoteAnnouncementsConfirmationJob = viewModelScope.launch {
            val shouldHideConfirmation = announcements
                .forMode(modelState.walletMode)
                .map { it.isEmpty() }
                .dataOrElse(false)

            if (shouldHideConfirmation) {
                if (withDelay) {
                    delay(CONFIRMATION_DELAY)
                }
                updateState {
                    copy(
                        hideAnnouncementsConfirmation = true,
                        animateHideAnnouncementsConfirmation = withDelay
                    )
                }
            } else {
                updateState {
                    copy(
                        hideAnnouncementsConfirmation = false,
                        animateHideAnnouncementsConfirmation = true
                    )
                }
            }
        }
    }

    private fun loadLocalAnnouncements() {
        localAnnouncementsJob?.cancel()
        localAnnouncementsJob = viewModelScope.launch {
            walletModeService.walletMode.collectLatest { walletMode ->
                val announcements = mutableListOf<LocalAnnouncement>()

                backupPhraseService.shouldBackupPhraseForMode(walletMode).let { shouldBackup ->
                    if (shouldBackup) {
                        announcements.add(
                            LocalAnnouncement(
                                type = LocalAnnouncementType.PHRASE_RECOVERY,
                                title = TextValue.IntResValue(
                                    com.blockchain.stringResources.R.string.announcement_recovery_title
                                ),
                                subtitle = TextValue.IntResValue(
                                    com.blockchain.stringResources.R.string.announcement_recovery_subtitle
                                ),
                                icon = ImageValue.Local(Icons.Filled.Unlock.id, tint = Pink600)
                            )
                        )
                    }
                }

                updateState {
                    copy(localAnnouncements = announcements)
                }
            }
        }
    }

    private fun DataResource<List<Announcement>>.forMode(
        walletMode: WalletMode?
    ) = filter {
        it.eligibleModes.contains(walletMode)
    }

    private fun DataResource<List<Announcement>>.remove(
        announcement: Announcement
    ) = map {
        it.minus { it == announcement }
    }

    companion object {
        private val CONFIRMATION_DELAY = TimeUnit.SECONDS.toMillis(1)
    }
}
