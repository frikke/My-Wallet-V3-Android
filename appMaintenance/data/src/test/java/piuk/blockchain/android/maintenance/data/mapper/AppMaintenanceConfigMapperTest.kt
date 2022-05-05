package piuk.blockchain.android.maintenance.data.mapper

import com.google.android.play.core.appupdate.AppUpdateInfo
import io.mockk.every
import io.mockk.mockk
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import org.junit.Test
import piuk.blockchain.android.maintenance.data.model.AppMaintenanceConfigDto
import piuk.blockchain.android.maintenance.domain.model.AppMaintenanceConfig

class AppMaintenanceConfigMapperTest {

    private val appUpdateInfo = mockk<AppUpdateInfo>()
    private val currentVersionCode = 1000
    private val currentOsVersion = 10
    private val configDto = AppMaintenanceConfigDto(
        bannedVersions = listOf(123),
        playStoreVersion = 2000,
        minimumAppVersion = 500,
        softUpgradeVersion = 100,
        minimumOSVersion = 14,
        siteWideMaintenance = true,
        redirectToWebsite = true,
        statusUrl = "statusUrl",
        storeUrl = "storeUrl",
        inAppUpdateFallbackUrl = "inAppUpdateFallbackUrl",
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

    @Test
    fun testMap() = runBlocking {
        every { appUpdateInfo.availableVersionCode() } returns 2000

        val result = AppMaintenanceConfigMapper.map(appUpdateInfo, configDto, currentVersionCode, currentOsVersion)

        assertEquals(expected, result)
    }
}
