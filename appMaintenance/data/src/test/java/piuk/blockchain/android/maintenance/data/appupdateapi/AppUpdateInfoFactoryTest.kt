package piuk.blockchain.android.maintenance.data.appupdateapi

import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import piuk.blockchain.android.maintenance.domain.appupdateapi.AppUpdateInfoFactory
import kotlin.test.assertEquals

class AppUpdateInfoFactoryTest {

    private val appUpdateManager = mockk<AppUpdateManager>()
    private val appUpdateInfoFactory: AppUpdateInfoFactory = AppUpdateInfoFactoryImpl(appUpdateManager)

    private val appUpdateInfo = mockk<AppUpdateInfo>()

    @Before
    fun setUp() {
        mockkStatic("piuk.blockchain.android.maintenance.data.appupdateapi.AppUpdateExtensionsKt")

        coEvery { appUpdateManager.getInfo() } returns appUpdateInfo
    }

    @Test
    fun testGetAppUpdateInfo() = runBlocking {
        val result = appUpdateInfoFactory.getAppUpdateInfo()

        assertEquals(appUpdateInfo, result)
    }
}