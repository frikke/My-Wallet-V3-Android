package piuk.blockchain.android.data.datamanagers

import com.blockchain.android.testutils.rxInit
import java.lang.Exception
import org.junit.Rule
import org.junit.Test
import piuk.blockchain.android.scan.domain.QrCodeDataService

class QrCodeDataServiceTest {
    private val subject: QrCodeDataService = QrCodeDataManager()

    @get:Rule
    val rx = rxInit {
        ioTrampoline()
        mainTrampoline()
    }

    @Test @Throws(Exception::class) fun generatePairingCode() {
        // Act
        val observer = subject.generatePairingCode(
            guid = GUID,
            password = PASSWORD,
            sharedKey = SHARED_KEY,
            encryptionPhrase = ENCRYPTION_PHRASE
        ).test()

        // Assert
        observer.assertValue {
            it.startsWith("1|$GUID")
        }
    }

    companion object {
        private const val GUID = "5c9a37e2-22e1-4357-a046-cce54ec7d81c"
        private const val SHARED_KEY = "SHARED_KEY"
        private const val PASSWORD = "PASSWORD"
        private const val ENCRYPTION_PHRASE = "ENCRYPTION_PHRASE"
    }
}
