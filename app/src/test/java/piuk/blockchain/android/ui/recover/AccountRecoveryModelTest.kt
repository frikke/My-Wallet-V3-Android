package piuk.blockchain.android.ui.recover

import com.blockchain.android.testutils.rxInit
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.schedulers.Schedulers
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import piuk.blockchain.androidcore.data.api.EnvironmentConfig

class AccountRecoveryModelTest {

    private lateinit var model: AccountRecoveryModel

    private val environmentConfig: EnvironmentConfig = mock {
        on { isRunningInDebugMode() }.thenReturn(false)
    }

    private val interactor: AccountRecoveryInteractor = mock()

    @get:Rule
    val rx = rxInit {
        ioTrampoline()
        computationTrampoline()
    }

    @Before
    fun setUp() {
        model = AccountRecoveryModel(
            initialState = AccountRecoveryState(),
            mainScheduler = Schedulers.io(),
            environmentConfig = environmentConfig,
            crashLogger = mock(),
            interactor = interactor
        )
    }

    @Test
    fun `verify lowercase seedphrase should succeed`() {
        // Arrange
        val seedPhrase = "seed phrase seed phrase seed phrase seed phrase seed phrase seed phrase"

        whenever(interactor.recoverCredentials(seedPhrase)).thenReturn(
            Completable.complete()
        )

        val testState = model.state.test()
        model.process(AccountRecoveryIntents.VerifySeedPhrase(seedPhrase))

        // Assert
        testState.assertValues(
            AccountRecoveryState(),
            AccountRecoveryState(
                seedPhrase = seedPhrase,
                status = AccountRecoveryStatus.VERIFYING_SEED_PHRASE
            ),
            AccountRecoveryState(
                seedPhrase = seedPhrase,
                status = AccountRecoveryStatus.RECOVERING_CREDENTIALS
            )
        )
    }

    @Test
    fun `verify camel case seedphrase should succeed`() {
        // Arrange
        val seedPhrase = "Seed Phrase Seed Phrase Seed Phrase Seed Phrase Seed Phrase Seed Phrase"
        val expectedCorrectedPhrase = seedPhrase.lowercase().trim()

        whenever(interactor.recoverCredentials(expectedCorrectedPhrase)).thenReturn(
            Completable.complete()
        )

        val testState = model.state.test()
        model.process(AccountRecoveryIntents.VerifySeedPhrase(seedPhrase))

        // Assert
        testState.assertValues(
            AccountRecoveryState(),
            AccountRecoveryState(
                seedPhrase = seedPhrase,
                status = AccountRecoveryStatus.VERIFYING_SEED_PHRASE
            ),
            AccountRecoveryState(
                seedPhrase = expectedCorrectedPhrase,
                status = AccountRecoveryStatus.RECOVERING_CREDENTIALS
            )
        )
    }

    @Test
    fun `verify seedphrase with newlines or tabbed spaces should succeed`() {
        // Arrange
        val seedPhrase = "Seed Phrase\nSeed Phrase\rSeed Phrase\t Seed Phrase Seed Phrase Seed Phrase"
        val expectedCorrectedPhrase = seedPhrase.lowercase().trim()

        whenever(interactor.recoverCredentials(expectedCorrectedPhrase)).thenReturn(
            Completable.complete()
        )

        val testState = model.state.test()
        model.process(AccountRecoveryIntents.VerifySeedPhrase(seedPhrase))

        // Assert
        testState.assertValues(
            AccountRecoveryState(),
            AccountRecoveryState(
                seedPhrase = seedPhrase,
                status = AccountRecoveryStatus.VERIFYING_SEED_PHRASE
            ),
            AccountRecoveryState(
                seedPhrase = expectedCorrectedPhrase,
                status = AccountRecoveryStatus.RECOVERING_CREDENTIALS
            )
        )
    }

    @Test
    fun `fail to verify short seedphrase should show word count error`() {
        // Arrange
        val seedPhrase = "seed phrase"

        val testState = model.state.test()
        model.process(AccountRecoveryIntents.VerifySeedPhrase(seedPhrase))

        // Assert
        testState.assertValues(
            AccountRecoveryState(),
            AccountRecoveryState(
                seedPhrase = seedPhrase,
                status = AccountRecoveryStatus.VERIFYING_SEED_PHRASE
            ),
            AccountRecoveryState(
                seedPhrase = seedPhrase,
                status = AccountRecoveryStatus.WORD_COUNT_ERROR
            )
        )
    }

    @Test
    fun `recover wallet successfully`() {
        // Arrange
        val seedPhrase = "seed phrase seed phrase seed phrase seed phrase seed phrase seed phrase"
        whenever(interactor.recoverCredentials(seedPhrase)).thenReturn(
            Completable.complete()
        )
        whenever(interactor.recoverWallet()).thenReturn(
            Completable.complete()
        )

        val testState = model.state.test()
        model.process(AccountRecoveryIntents.VerifySeedPhrase(seedPhrase))

        // Assert
        testState.assertValues(
            AccountRecoveryState(),
            AccountRecoveryState(
                seedPhrase = seedPhrase,
                status = AccountRecoveryStatus.VERIFYING_SEED_PHRASE
            ),
            AccountRecoveryState(
                seedPhrase = seedPhrase,
                status = AccountRecoveryStatus.RECOVERING_CREDENTIALS
            ),
            AccountRecoveryState(
                seedPhrase = seedPhrase,
                status = AccountRecoveryStatus.RESETTING_KYC
            ),
            AccountRecoveryState(
                seedPhrase = seedPhrase,
                status = AccountRecoveryStatus.RECOVERY_SUCCESSFUL
            )
        )
    }

    @Test
    fun `fail to recover wallet credentials should show recovery failed`() {
        // Arrange
        val seedPhrase = "seed phrase seed phrase seed phrase seed phrase seed phrase seed phrase"
        whenever(interactor.recoverCredentials(seedPhrase)).thenReturn(
            Completable.error(Exception())
        )

        val testState = model.state.test()
        model.process(AccountRecoveryIntents.VerifySeedPhrase(seedPhrase))

        // Assert
        testState.assertValues(
            AccountRecoveryState(),
            AccountRecoveryState(
                seedPhrase = seedPhrase,
                status = AccountRecoveryStatus.VERIFYING_SEED_PHRASE
            ),
            AccountRecoveryState(
                seedPhrase = seedPhrase,
                status = AccountRecoveryStatus.RECOVERING_CREDENTIALS
            ),
            AccountRecoveryState(
                seedPhrase = seedPhrase,
                status = AccountRecoveryStatus.RECOVERY_FAILED
            )
        )
    }
}
