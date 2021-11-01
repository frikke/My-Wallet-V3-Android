package piuk.blockchain.android.ui.login

import android.content.Intent
import android.net.Uri
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import piuk.blockchain.android.util.AppUtil
import piuk.blockchain.androidcore.data.auth.AuthDataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.utils.PersistentPrefs

class LoginInteractorTest {

    private lateinit var subject: LoginInteractor
    private val authDataManager: AuthDataManager = mock()
    private val payloadDataManager: PayloadDataManager = mock()
    private val prefs: PersistentPrefs = mock()
    private val appUtil: AppUtil = mock()
    private val persistentPrefs: PersistentPrefs = mock()

    private val action = Intent.ACTION_VIEW
    private val data: Uri = mock()

    @Before
    fun setup() {
        subject = LoginInteractor(
            authDataManager = authDataManager,
            payloadDataManager = payloadDataManager,
            prefs = prefs,
            appUtil = appUtil,
            persistentPrefs = persistentPrefs
        )
    }

    @Test
    fun `check session when pin exists sends PIN intent`() {
        whenever(persistentPrefs.pinId).thenReturn("12343")

        val result = subject.checkSessionDetails(action, data)
        Assert.assertEquals(LoginIntents.UserIsLoggedIn, result)
    }

    @Test
    fun `check session when intent exists sends deeplink intent`() {
        whenever(persistentPrefs.pinId).thenReturn("")

        val uri: Uri = mock {
            on { fragment }.thenReturn("/login/abcd")
        }

        val intent: Intent = mock {
            on { data }.thenReturn(uri)
            on { this.action }.thenReturn(action)
        }

        val result = subject.checkSessionDetails(intent.action!!, intent.data!!)
        val expectedResult = LoginIntents.UserAuthenticationRequired(action, uri)
        Assert.assertTrue(result is LoginIntents.UserAuthenticationRequired)
        Assert.assertEquals(expectedResult.action, (result as LoginIntents.UserAuthenticationRequired).action)
        Assert.assertEquals(expectedResult.uri, result.uri)
    }

    @Test
    fun `check session when no intent delimiter exists sends error intent`() {
        whenever(persistentPrefs.pinId).thenReturn("")

        val uri: Uri = mock {
            on { fragment }.thenReturn("abcd")
        }

        val intent: Intent = mock {
            on { data }.thenReturn(uri)
        }

        val result = subject.checkSessionDetails(action, intent.data!!)
        Assert.assertEquals(LoginIntents.UnknownError, result)
    }
}