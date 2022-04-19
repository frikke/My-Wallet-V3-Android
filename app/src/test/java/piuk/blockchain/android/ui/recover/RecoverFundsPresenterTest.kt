package piuk.blockchain.android.ui.recover

import com.blockchain.android.testutils.rxInit
import com.blockchain.core.featureflag.IntegratedFeatureFlag
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import info.blockchain.wallet.metadata.MetadataInteractor
import io.reactivex.rxjava3.core.Single
import kotlinx.serialization.json.Json
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions
import piuk.blockchain.androidcore.data.auth.metadata.WalletCredentialsMetadata
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

    private val moshiAdapter: JsonAdapter<WalletCredentialsMetadata> = mock()
    private val moshi: Moshi = mock {
        on { adapter(WalletCredentialsMetadata::class.java) }.doReturn(moshiAdapter)
    }
    private val disableMoshiFeatureFlag: IntegratedFeatureFlag = mock {
        on { enabled }.thenReturn(Single.just(true))
    }
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
        moshi = moshi,
        json = json,
        disableMoshiFeatureFlag = disableMoshiFeatureFlag
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
