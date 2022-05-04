package piuk.blockchain.android.maintenance.presentation.appupdateapi

import android.app.Activity
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import piuk.blockchain.android.maintenance.domain.appupdateapi.AppUpdateInfoFactory

class AppUpdateSettingsTest {
    private val appUpdateManager = mockk<AppUpdateManager>()
    private val appUpdateInfoFactory = mockk<AppUpdateInfoFactory>()
    private val inAppUpdateSettings: InAppUpdateSettings = InAppUpdateSettingsImpl(appUpdateManager, appUpdateInfoFactory)

    private val activity = mockk<Activity>()
    private val appUpdateInfo = mockk<AppUpdateInfo>()

    @Before
    fun setUp() {
        coEvery { appUpdateInfoFactory.getAppUpdateInfo() } returns appUpdateInfo
        every { appUpdateManager.startUpdateFlowForResult(any(), any(), any<Activity>(), any()) } returns true
    }

    @Test
    fun `WHEN value is not null or empty, THEN ExternalUrl should be returned`() = runBlocking {
        inAppUpdateSettings.triggerOrResumeAppUpdate(activity)

        verify(exactly = 1) { appUpdateManager.startUpdateFlowForResult(any(), any(), any<Activity>(), any()) }
    }
}
