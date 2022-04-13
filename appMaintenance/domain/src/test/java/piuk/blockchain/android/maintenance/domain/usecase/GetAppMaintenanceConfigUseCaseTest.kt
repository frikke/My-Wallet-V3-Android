package piuk.blockchain.android.maintenance.domain.usecase

import com.blockchain.outcome.Outcome
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Test
import piuk.blockchain.android.maintenance.domain.model.AppMaintenanceConfig
import piuk.blockchain.android.maintenance.domain.model.AppMaintenanceStatus
import piuk.blockchain.android.maintenance.domain.model.UpdateLocation
import piuk.blockchain.android.maintenance.domain.repository.AppMaintenanceRepository
import kotlin.test.assertEquals

class GetAppMaintenanceConfigUseCaseTest {

    private val repository = mockk<AppMaintenanceRepository>()
    private val useCase by lazy { GetAppMaintenanceConfigUseCase(repository = repository) }

    private val data = AppMaintenanceConfig(
        currentVersionCode = 0,
        currentOsVersion = 0,
        playStoreVersion = 0,
        bannedVersions = listOf(),
        minimumAppVersion = 0,
        softUpgradeVersion = 0,
        minimumOSVersion = 0,
        siteWideMaintenance = false,
        redirectToWebsite = false,
        statusUrl = "statusUrl",
        storeUrl = "storeUrl",
        websiteUrl = "websiteUrl"
    )

    @Test
    fun `WHEN repository returns failure, THEN Unknown should be returned`() = runBlocking {
        coEvery { repository.getAppMaintenanceConfig() } returns Outcome.Failure(Throwable())

        val result: AppMaintenanceStatus = useCase()

        assertEquals(AppMaintenanceStatus.NonActionable.Unknown, result)
    }

    @Test
    fun `WHEN siteWideMaintenance is true, THEN SiteWideMaintenance should be returned`() = runBlocking {
        coEvery { repository.getAppMaintenanceConfig() } returns Outcome.Success(data.copy(siteWideMaintenance = true))

        val result: AppMaintenanceStatus = useCase()

        assertEquals(AppMaintenanceStatus.Actionable.SiteWideMaintenance("statusUrl"), result)
    }

    @Test
    fun `WHEN redirectToWebsite is true, THEN RedirectToWebsite should be returned`() = runBlocking {
        coEvery { repository.getAppMaintenanceConfig() } returns Outcome.Success(data.copy(redirectToWebsite = true))

        val result: AppMaintenanceStatus = useCase()

        assertEquals(AppMaintenanceStatus.Actionable.RedirectToWebsite("websiteUrl"), result)
    }

    @Test
    fun `WHEN currentVersion is banned && playStoreVersion is banned, THEN RedirectToWebsite should be returned`() =
        runBlocking {
            coEvery { repository.getAppMaintenanceConfig() } returns
                Outcome.Success(
                    data.copy(
                        bannedVersions = listOf(1000, 2000),
                        currentVersionCode = 1000,
                        playStoreVersion = 2000
                    )
                )

            val result: AppMaintenanceStatus = useCase()

            assertEquals(AppMaintenanceStatus.Actionable.RedirectToWebsite("websiteUrl"), result)
        }

    @Test
    fun `WHEN currentVersion is banned && playStoreVersion is not, THEN MandatoryUpdate should be returned`() =
        runBlocking {
            coEvery { repository.getAppMaintenanceConfig() } returns
                Outcome.Success(
                    data.copy(
                        bannedVersions = listOf(1000),
                        currentVersionCode = 1000,
                        playStoreVersion = 2000
                    )
                )

            val result: AppMaintenanceStatus = useCase()

            assertEquals(
                AppMaintenanceStatus.Actionable.MandatoryUpdate(UpdateLocation.ExternalUrl("storeUrl")),
                result
            )
        }

    @Test
    fun `WHEN currentVersion is lower than minimumAppVersion, THEN MandatoryUpdate should be returned`() = runBlocking {
        coEvery { repository.getAppMaintenanceConfig() } returns
            Outcome.Success(data.copy(currentVersionCode = 1000, minimumAppVersion = 2000))

        val result: AppMaintenanceStatus = useCase()

        assertEquals(
            AppMaintenanceStatus.Actionable.MandatoryUpdate(UpdateLocation.ExternalUrl("storeUrl")),
            result
        )
    }

    @Test
    fun `WHEN currentVersion is lower than softUpgradeVersion, THEN OptionalUpdate should be returned`() = runBlocking {
        coEvery { repository.getAppMaintenanceConfig() } returns
            Outcome.Success(data.copy(currentVersionCode = 1000, softUpgradeVersion = 2000))

        val result: AppMaintenanceStatus = useCase()

        assertEquals(
            AppMaintenanceStatus.Actionable.OptionalUpdate(UpdateLocation.ExternalUrl("storeUrl")),
            result
        )
    }

    @Test
    fun `WHEN no condition is met, THEN AllClear should be returned`() = runBlocking {
        coEvery { repository.getAppMaintenanceConfig() } returns Outcome.Success(data)

        val result: AppMaintenanceStatus = useCase()

        assertEquals(AppMaintenanceStatus.NonActionable.AllClear, result)
    }
}