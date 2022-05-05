package piuk.blockchain.android.ui.recover

import com.blockchain.android.testutils.rxInit
import com.nhaarman.mockitokotlin2.mock
import info.blockchain.wallet.metadata.MetadataInteractor
import kotlinx.serialization.json.Json
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions
import piuk.blockchain.androidcore.data.payload.PayloadDataManager

@Suppress("LocalVariableName")
class RecoverFundsPresenterTest {

    @get:Rule
    val rxSchedulers = rxInit {
        mainTrampoline()
        ioTrampoline()
        computationTrampoline()
    }

    private val payloadDataManager: PayloadDataManager = mock()
    private val metadataInteractor: MetadataInteractor = mock()

    private val json = Json {
        explicitNulls = false
        ignoreUnknownKeys = true
        isLenient = true
    }

    private var subject: RecoverFundsPresenter = RecoverFundsPresenter(
        payloadDataManager = payloadDataManager,
        prefs = mock(),
        metadataInteractor = metadataInteractor,
        metadataDerivation = mock(),
        json = json
    )

    private val view: RecoverFundsView = mock()

    @Before
    fun setUp() {
        subject.initView(view)
    }

    /**
     * Recovery phrase missing, should inform user.
     */
    @Test
    fun onContinueClickedNoRecoveryPhrase() {
        // Arrange

        // Act
        subject.onContinueClicked("")

        // Assert
        verify(view).showError(anyInt())
        verifyNoMoreInteractions(view)
    }

    /**
     * Recovery phrase is too short to be valid, should inform user.
     */
    @Test
    fun onContinueClickedInvalidRecoveryPhraseLength() {
        // Arrange

        // Act
        subject.onContinueClicked("one two three four")

        // Assert
        verify(view).showError(anyInt())
        verifyNoMoreInteractions(view)
    }

    /**
     * Successful restore. Should take the user to the PIN entry page.
     */
    @Test
    fun onContinueClickedRestoreSuccess() {
        // Currently this is untestable, without some major down stream refactoring
        // There are too many complex static objects to be mockable.
    }

    /**
     * Restore failed, inform the user.
     */
    @Test
    fun onContinueClickedFailed() {
        // Arrange
        // TODO: 13/07/2017 isValidMnemonic not testable
        // Act

        // Assert
    }
}
