package com.blockchain.home.presentation.accouncement

import androidx.lifecycle.viewModelScope
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.icons.Unlock
import com.blockchain.componentlib.theme.Pink600
import com.blockchain.componentlib.utils.ImageValue
import com.blockchain.componentlib.utils.TextValue
import com.blockchain.defiwalletbackup.domain.service.BackupPhraseService
import com.blockchain.home.presentation.R
import com.blockchain.home.presentation.dashboard.HomeNavEvent
import com.blockchain.walletmode.WalletModeService
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AnnouncementsViewModel(
    private val walletModeService: WalletModeService,
    private val backupPhraseService: BackupPhraseService,
) : MviViewModel<
    AnnouncementsIntent,
    AnnouncementsViewState,
    AnnouncementModelState,
    HomeNavEvent,
    ModelConfigArgs.NoArgs>(
    AnnouncementModelState()
) {
    private var announcementsJob: Job? = null

    override fun viewCreated(args: ModelConfigArgs.NoArgs) {}

    override fun reduce(state: AnnouncementModelState): AnnouncementsViewState = state.run {
        AnnouncementsViewState(
            announcements = announcements
        )
    }

    override suspend fun handleIntent(modelState: AnnouncementModelState, intent: AnnouncementsIntent) {
        when (intent) {
            AnnouncementsIntent.LoadAnnouncements -> {
                loadData()
            }
        }
    }

    private fun loadData() {
        announcementsJob?.cancel()
        announcementsJob = viewModelScope.launch {
            walletModeService.walletMode.collectLatest { walletMode ->
                val announcements = mutableListOf<Announcement>()

                backupPhraseService.shouldBackupPhraseForMode(walletMode).let { shouldBackup ->
                    if (shouldBackup) {
                        announcements.add(
                            Announcement(
                                type = AnnouncementType.PHRASE_RECOVERY,
                                title = TextValue.IntResValue(R.string.announcement_recovery_title),
                                subtitle = TextValue.IntResValue(R.string.announcement_recovery_subtitle),
                                icon = ImageValue.Local(Icons.Filled.Unlock.id, tint = Pink600),
                            )
                        )
                    }
                }

                updateState {
                    it.copy(announcements = announcements)
                }
            }
        }
    }
}
