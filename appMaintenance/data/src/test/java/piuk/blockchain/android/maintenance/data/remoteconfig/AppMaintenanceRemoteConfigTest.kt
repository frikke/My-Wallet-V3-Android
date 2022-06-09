package piuk.blockchain.android.maintenance.data.remoteconfig

import com.blockchain.preferences.AppMaintenancePrefs
import com.blockchain.remoteconfig.RemoteConfig
import io.mockk.every
import io.mockk.mockk
import io.reactivex.rxjava3.core.Single
import kotlin.test.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Before
import org.junit.Test
import piuk.blockchain.android.maintenance.data.model.AppMaintenanceConfigDto

@ExperimentalCoroutinesApi
class AppMaintenanceRemoteConfigTest {

    private val remoteConfig = mockk<RemoteConfig>()
    private val json = Json
    private val appMaintenancePrefs = mockk<AppMaintenancePrefs>()

    private val appMaintenanceRemoteConfig = AppMaintenanceRemoteConfig(remoteConfig, json, appMaintenancePrefs)

    private val configObject = AppMaintenanceConfigDto(
        bannedVersions = listOf(1000),
        playStoreVersion = 20,
        minimumAppVersion = 12,
        softUpgradeVersion = 23,
        minimumOSVersion = 34,
        siteWideMaintenance = true,
        redirectToWebsite = false,
        statusUrl = "statusUrl",
        storeUrl = "storeUrl",
        inAppUpdateFallbackUrl = "inAppUpdateFallbackUrl",
        websiteUrl = "websiteUrl"
    )

    private val configJson = """
        {
          "bannedVersions": [
            1000
          ],
          "playStoreVersion": 20,
          "minimumAppVersion": 12,
          "softUpgradeVersion": 23,
          "minimumOSVersion": 34,
          "siteWideMaintenance": true,
          "redirectToWebsite": false,
          "statusURL": "statusUrl",
          "storeURI": "storeUrl",
          "inAppUpdateFallbackUrl": "inAppUpdateFallbackUrl",
          "websiteUrl": "websiteUrl"
        }
    """.trimIndent()

    @Before
    fun setUp() {
        every { appMaintenancePrefs.isAppMaintenanceDebugOverrideEnabled } returns false
    }

    @Test(expected = Exception::class)
    fun `WHEN remoteConfig returns empty, THEN null should be returned`() = runTest {
        every { remoteConfig.getRawJson(any()) } returns Single.just("")

        appMaintenanceRemoteConfig.getAppMaintenanceConfig()
    }

    @Test
    fun `WHEN remoteConfig returns configJson, THEN configObject should be returned`() = runTest {
        every { remoteConfig.getRawJson(any()) } returns Single.just(configJson)

        val result = appMaintenanceRemoteConfig.getAppMaintenanceConfig()

        assertEquals(configObject, result)
    }
}
