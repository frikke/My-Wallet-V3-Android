package piuk.blockchain.android.maintenance.data.remoteconfig

import com.blockchain.remoteconfig.RemoteConfig
import io.mockk.every
import io.mockk.mockk
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Test
import piuk.blockchain.android.maintenance.data.model.AppMaintenanceConfigDto
import kotlin.test.assertEquals

class AppMaintenanceRemoteConfigTest {

    private val remoteConfig = mockk<RemoteConfig>()
    private val json = Json

    private val appMaintenanceRemoteConfig = AppMaintenanceRemoteConfig(remoteConfig, json)

    private val configObject = AppMaintenanceConfigDto(
        bannedVersions = listOf(1000),
        minimumAppVersion = 12,
        softUpgradeVersion = 23,
        minimumOSVersion = 34,
        siteWideMaintenance = true,
        redirectToWebsite = false,
        statusUrl = "statusUrl",
        storeUrl = "storeUrl",
        websiteUrl = "websiteUrl"
    )

    private val configJson = """
        {
          "bannedVersions": [
            1000
          ],
          "minimumAppVersion": 12,
          "softUpgradeVersion": 23,
          "minimumOSVersion": 34,
          "siteWideMaintenance": true,
          "redirectToWebsite": false,
          "statusURL": "statusUrl",
          "storeURI": "storeUrl",
          "websiteUrl": "websiteUrl"
        }
    """.trimIndent()

    @Test(expected = Exception::class)
    fun `WHEN remoteConfig returns empty, THEN null should be returned`() = runBlocking {
        every { remoteConfig.getRawJson(any()) } returns Single.just("")

        val result = appMaintenanceRemoteConfig.getAppMaintenanceConfig()
    }

    @Test
    fun `WHEN remoteConfig returns configJson, THEN configObject should be returned`() = runBlocking {
        every { remoteConfig.getRawJson(any()) } returns Single.just(configJson)

        val result = appMaintenanceRemoteConfig.getAppMaintenanceConfig()

        assertEquals(configObject, result)
    }
}