package piuk.blockchain.android.ui.start

import com.blockchain.android.testutils.rxInit
import com.blockchain.coincore.loader.AssetCatalogueImpl
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.enviroment.EnvironmentConfig
import com.blockchain.nabu.datamanagers.ApiStatus
import com.blockchain.preferences.OnboardingPrefs
import com.blockchain.preferences.SecurityPrefs
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.rxjava3.core.Single
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito
import piuk.blockchain.android.util.RootUtil

class LandingPresenterTest {

    private lateinit var subject: LandingPresenter
    private val view: LandingView = mock()
    private val apiStatus: ApiStatus = mock {
        on { isHealthy() }.thenReturn(Single.just(true))
    }
    private val environmentSettings: EnvironmentConfig =
        mock(defaultAnswer = Mockito.RETURNS_DEEP_STUBS)

    private val assetCatalogue: AssetCatalogueImpl = mock()
    private val exchangeRatesDataManager: ExchangeRatesDataManager = mock()

    private val prefs: SecurityPrefs = mock()
    private val onboardingPrefs: OnboardingPrefs = mock()
    private val rootUtil: RootUtil = mock()

    @get:Rule
    val rx = rxInit {
        mainTrampoline()
        ioTrampoline()
        computationTrampoline()
    }

    @Before
    fun setUp() {
        subject =
            LandingPresenter(
                environmentSettings,
                prefs,
                onboardingPrefs,
                rootUtil,
                apiStatus,
                assetCatalogue,
                exchangeRatesDataManager
            )
    }

    @Test
    fun `onViewReady no debug`() {
        // Arrange
        whenever(environmentSettings.isRunningInDebugMode()).thenReturn(false)
        whenever(assetCatalogue.initialise()).thenReturn(Single.error(Exception()))
        // Act
        subject.attachView(view)
        // Assert
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `device is rooted and warnings are off - don't show dialog`() {
        // Arrange
        whenever(rootUtil.isDeviceRooted).thenReturn(true)
        whenever(prefs.disableRootedWarning).thenReturn(false)
        whenever(assetCatalogue.initialise()).thenReturn(Single.error(Exception()))

        subject.attachView(view)

        // Act
        subject.checkForRooted()

        // Assert
        verify(view).showIsRootedWarning()
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `device is not rooted and warnings are off - don't show dialog`() {
        // Arrange
        whenever(rootUtil.isDeviceRooted).thenReturn(false)
        whenever(prefs.disableRootedWarning).thenReturn(false)
        whenever(assetCatalogue.initialise()).thenReturn(Single.error(Exception()))

        subject.attachView(view)

        // Act
        subject.checkForRooted()

        // Assert
        verifyZeroInteractions(view)
    }

    @Test
    fun `device is rooted and warnings are on - show dialog`() {
        // Arrange
        whenever(rootUtil.isDeviceRooted).thenReturn(true)
        whenever(prefs.disableRootedWarning).thenReturn(false)
        whenever(assetCatalogue.initialise()).thenReturn(Single.error(Exception()))

        subject.attachView(view)

        // Act
        subject.checkForRooted()

        // Assert
        verify(view).showIsRootedWarning()
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `device is not rooted and warnings are on - don't show dialog`() {
        // Arrange
        whenever(rootUtil.isDeviceRooted).thenReturn(false)
        whenever(prefs.disableRootedWarning).thenReturn(true)

        // Act
        subject.checkForRooted()

        // Assert
        verifyZeroInteractions(view)
    }
}
