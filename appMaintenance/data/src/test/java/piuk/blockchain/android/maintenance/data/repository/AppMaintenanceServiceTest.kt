package piuk.blockchain.android.maintenance.data.repository

import com.blockchain.outcome.Outcome
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.install.model.InstallStatus
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlin.test.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Before
import org.junit.Test
import piuk.blockchain.android.maintenance.data.model.AppMaintenanceConfigDto
import piuk.blockchain.android.maintenance.data.remoteconfig.AppMaintenanceRemoteConfig
import piuk.blockchain.android.maintenance.domain.appupdateapi.AppUpdateInfoFactory
import piuk.blockchain.android.maintenance.domain.model.AppMaintenanceConfig

@OptIn(ExperimentalCoroutinesApi::class)
class AppMaintenanceServiceTest {

    private val appMaintenanceRemoteConfig = mockk<AppMaintenanceRemoteConfig>()
    private val appUpdateInfoFactory = mockk<AppUpdateInfoFactory>()
    private val dispatcher = UnconfinedTestDispatcher()

    private val appUpdateInfo = mockk<AppUpdateInfo>()

    private val appMaintenanceRepository = AppMaintenanceRepository(
        appMaintenanceRemoteConfig,
        appUpdateInfoFactory,
        1000,
        10,
        dispatcher
    )

    private val dataDto = AppMaintenanceConfigDto(
        bannedVersions = listOf(123),
        minimumAppVersion = 500,
        softUpgradeVersion = 100,
        minimumOSVersion = 14,
        siteWideMaintenance = true,
        redirectToWebsite = true,
        statusUrl = "statusUrl",
        storeUrl = "storeUrl",
        websiteUrl = "websiteUrl"
    )

    private val expected = AppMaintenanceConfig(
        currentVersionCode = 1000,
        currentOsVersion = 10,
        playStoreVersion = 2000,
        bannedVersions = listOf(123),
        minimumAppVersion = 500,
        softUpgradeVersion = 100,
        minimumOSVersion = 14,
        siteWideMaintenance = true,
        redirectToWebsite = true,
        statusUrl = "statusUrl",
        storeUrl = "storeUrl",
        websiteUrl = "websiteUrl"
    )

    @Before
    fun setUp() {
        coEvery { appUpdateInfoFactory.getAppUpdateInfo() } returns appUpdateInfo
    }

    @Test
    fun `WHEN installStatus is PENDING, DOWNLOADING, INSTALLING, THEN true should be returned`() = runBlocking {
        every { appUpdateInfo.installStatus() } returns InstallStatus.PENDING
        var result = appMaintenanceRepository.isDownloadInProgress()
        assertEquals(true, result)

        every { appUpdateInfo.installStatus() } returns InstallStatus.DOWNLOADING
        result = appMaintenanceRepository.isDownloadInProgress()
        assertEquals(true, result)

        every { appUpdateInfo.installStatus() } returns InstallStatus.INSTALLING
        result = appMaintenanceRepository.isDownloadInProgress()
        assertEquals(true, result)
    }

    @Test
    fun `WHEN installStatus is CANCELED, DOWNLOADED, FAILED, INSTALLED, UNKNOWN, THEN false should be returned`() =
        runBlocking {
            every { appUpdateInfo.installStatus() } returns InstallStatus.CANCELED
            var result = appMaintenanceRepository.isDownloadInProgress()
            assertEquals(false, result)

            every { appUpdateInfo.installStatus() } returns InstallStatus.DOWNLOADED
            result = appMaintenanceRepository.isDownloadInProgress()
            assertEquals(false, result)

            every { appUpdateInfo.installStatus() } returns InstallStatus.FAILED
            result = appMaintenanceRepository.isDownloadInProgress()
            assertEquals(false, result)

            every { appUpdateInfo.installStatus() } returns InstallStatus.INSTALLED
            result = appMaintenanceRepository.isDownloadInProgress()
            assertEquals(false, result)

            every { appUpdateInfo.installStatus() } returns InstallStatus.UNKNOWN
            result = appMaintenanceRepository.isDownloadInProgress()
            assertEquals(false, result)
        }

    @Test
    fun `WHEN all data is available, THEN Outcome Success with data should be returned`() =
        runBlocking {
            coEvery { appMaintenanceRemoteConfig.getAppMaintenanceConfig() } returns dataDto
            coEvery { appUpdateInfo.availableVersionCode() } returns 2000

            val result = appMaintenanceRepository.getAppMaintenanceConfig()

            assertEquals(true, result is Outcome.Success)
            assertEquals(expected, (result as Outcome.Success).value)
        }

    @Test
    fun `WHEN appMaintenanceRemoteConfig returns null, THEN Outcome Failure should be returned`() =
        runBlocking {
            coEvery { appMaintenanceRemoteConfig.getAppMaintenanceConfig() } throws Throwable()
            coEvery { appUpdateInfo.availableVersionCode() } returns 2000

            val result = appMaintenanceRepository.getAppMaintenanceConfig()

            assertEquals(true, result is Outcome.Failure)
        }

    @Test
    fun `WHEN appUpdateInfo throws, THEN Outcome Failure should be returned`() =
        runBlocking {
            coEvery { appUpdateInfoFactory.getAppUpdateInfo() } throws Throwable()

            coEvery { appMaintenanceRemoteConfig.getAppMaintenanceConfig() } returns dataDto

            val result = appMaintenanceRepository.getAppMaintenanceConfig()

            assertEquals(true, result is Outcome.Failure)
        }
}
