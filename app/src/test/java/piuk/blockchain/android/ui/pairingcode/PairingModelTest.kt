package piuk.blockchain.android.ui.pairingcode

import com.blockchain.android.testutils.rxInit
import com.blockchain.core.auth.AuthDataManager
import com.blockchain.core.payload.PayloadDataManager
import com.blockchain.enviroment.EnvironmentConfig
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import info.blockchain.wallet.payload.data.Wallet
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import piuk.blockchain.android.scan.domain.QrCodeDataService

class PairingModelTest {

    private lateinit var model: PairingModel

    private val environmentConfig: EnvironmentConfig = mock {
        on { isRunningInDebugMode() }.thenReturn(false)
    }

    private val payloadWallet: Wallet = mock {
        on { guid }.thenReturn("1234")
        on { sharedKey }.thenReturn("sharedkey")
    }

    private val payloadDataManager: PayloadDataManager = mock {
        on { wallet }.thenReturn(payloadWallet)
        on { tempPassword }.thenReturn("password")
    }

    private val qrCodeDataService: QrCodeDataService = mock {
        on { generatePairingCode(any(), any(), any(), any()) }.thenReturn(
            Single.just(PAIRING_CODE_URI)
        )
    }

    private val authDataManager: AuthDataManager = mock {
        on { getPairingEncryptionPassword(any()) }.thenReturn(
            Observable.just(
                ResponseBody.create(
                    ("application/text").toMediaTypeOrNull(),
                    "asdasdasd"
                )
            )
        )
    }

    @get:Rule
    val rx = rxInit {
        ioTrampoline()
        computationTrampoline()
    }

    @Before
    fun setUp() {
        model = PairingModel(
            initialState = PairingState(),
            mainScheduler = Schedulers.io(),
            environmentConfig = environmentConfig,
            remoteLogger = mock(),
            qrCodeDataService = qrCodeDataService,
            payloadDataManager = payloadDataManager,
            authDataManager = authDataManager,
            analytics = mock()
        )
    }

    @Test
    fun `generate pairing QR successfully`() {
        val testState = model.state.test()
        model.process(PairingIntents.LoadQrImage)
        testState.assertValueAt(0, PairingState())
        testState.assertValueAt(1, PairingState(imageStatus = QrCodeImageStatus.Loading))
        testState.assertValueAt(2, PairingState(imageStatus = QrCodeImageStatus.Ready(PAIRING_CODE_URI)))
    }

    companion object {
        private const val PAIRING_CODE_URI = "PAIRING_CODE_URI"
    }
}
