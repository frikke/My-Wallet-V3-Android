package piuk.blockchain.android.ui.login

import android.content.Intent
import android.net.Uri
import com.blockchain.core.auth.AuthDataManager
import com.blockchain.preferences.AuthPrefs
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import piuk.blockchain.android.util.AppUtil
import piuk.blockchain.androidcore.data.payload.PayloadDataManager

class LoginInteractorTest {

    private lateinit var subject: LoginInteractor
    private val authDataManager: AuthDataManager = mock()
    private val payloadDataManager: PayloadDataManager = mock()
    private val authPrefs: AuthPrefs = mock()
    private val appUtil: AppUtil = mock()

    private val action = Intent.ACTION_VIEW
    private val data: Uri = mock()

    @Before
    fun setup() {
        subject = LoginInteractor(
            authDataManager = authDataManager,
            payloadDataManager = payloadDataManager,
            authPrefs = authPrefs,
            appUtil = appUtil
        )
    }

    @Test
    fun `check login intent when pin exists and no deeplink data sends PIN intent`() {
        whenever(authPrefs.pinId).thenReturn("12343")

        val result = subject.checkSessionDetails(action, data)
        Assert.assertEquals(LoginIntents.UserLoggedInWithoutDeeplinkData, result)
    }

    @Test
    fun `check login intent when pin exists and intent data exists and session id empty`() {
        whenever(authPrefs.pinId).thenReturn("1234")
        whenever(authPrefs.sessionId).thenReturn("")
        whenever(data.fragment).thenReturn("/login/$BASE_64_FULL_PAYLOAD")

        val result = subject.checkSessionDetails(action, data)
        Assert.assertTrue(result is LoginIntents.ReceivedExternalLoginApprovalRequest)
    }

    @Test
    fun `check login intent when pin exists and intent data exists and session ids mismatch`() {
        whenever(authPrefs.pinId).thenReturn("1234")
        whenever(authPrefs.sessionId).thenReturn("12345")
        whenever(data.fragment).thenReturn("/login/$BASE_64_FULL_PAYLOAD")

        val result = subject.checkSessionDetails(action, data)
        Assert.assertTrue(result is LoginIntents.ReceivedExternalLoginApprovalRequest)
    }

    @Test
    fun `check login intent when pin exists and intent data exists and session ids match`() {
        whenever(authPrefs.pinId).thenReturn("1234")
        whenever(authPrefs.sessionId).thenReturn("1234")
        whenever(data.fragment).thenReturn("/login/$BASE_64_FULL_PAYLOAD")

        val result = subject.checkSessionDetails(action, data)
        val expectedResult = LoginIntents.UserAuthenticationRequired(action, data)

        Assert.assertTrue(result is LoginIntents.UserAuthenticationRequired)
        Assert.assertEquals(expectedResult.action, (result as LoginIntents.UserAuthenticationRequired).action)
        Assert.assertEquals(expectedResult.uri, result.uri)
    }

    @Test
    fun `check login intent when intent data exists and session id empty`() {
        whenever(authPrefs.pinId).thenReturn("")
        whenever(authPrefs.sessionId).thenReturn("")
        whenever(data.fragment).thenReturn("/login/$BASE_64_FULL_PAYLOAD")

        val result = subject.checkSessionDetails(action, data)
        Assert.assertTrue(result is LoginIntents.ReceivedExternalLoginApprovalRequest)
    }

    @Test
    fun `check login intent when intent data exists and session ids mismatch`() {
        whenever(authPrefs.pinId).thenReturn("")
        whenever(authPrefs.sessionId).thenReturn("12345")
        whenever(data.fragment).thenReturn("/login/$BASE_64_FULL_PAYLOAD")

        val result = subject.checkSessionDetails(action, data)
        Assert.assertTrue(result is LoginIntents.ReceivedExternalLoginApprovalRequest)
    }

    @Test
    fun `check login intent when intent data exists and session ids match`() {
        whenever(authPrefs.pinId).thenReturn("")
        whenever(authPrefs.sessionId).thenReturn("1234")
        whenever(data.fragment).thenReturn("/login/$BASE_64_FULL_PAYLOAD")

        val result = subject.checkSessionDetails(action, data)
        val expectedResult = LoginIntents.UserAuthenticationRequired(action, data)

        Assert.assertTrue(result is LoginIntents.UserAuthenticationRequired)
        Assert.assertEquals(expectedResult.action, (result as LoginIntents.UserAuthenticationRequired).action)
        Assert.assertEquals(expectedResult.uri, result.uri)
    }

    @Test
    @Throws(IllegalStateException::class)
    fun `check login intent when exception is thrown`() {
        whenever(authPrefs.pinId).thenReturn("")
        whenever(authPrefs.sessionId).thenThrow(IllegalStateException())
        whenever(data.fragment).thenReturn("/login/abcd")

        val result = subject.checkSessionDetails(action, data)
        Assert.assertTrue(result is LoginIntents.UnknownError)
    }

    @Test
    fun `check login intent when intent data corrupt`() {
        whenever(authPrefs.pinId).thenReturn("")
        whenever(data.fragment).thenReturn("/login/$BASE_64_CORRUPT_PAYLOAD")
        whenever(authPrefs.sessionId).thenReturn("1234")

        val result = subject.checkSessionDetails(action, data)
        Assert.assertTrue(result is LoginIntents.UnknownError)
    }

    @Test
    fun `check login intent when no intent delimiter exists sends error intent`() {
        whenever(authPrefs.pinId).thenReturn("")

        val uri: Uri = mock {
            on { fragment }.thenReturn("abcd")
        }

        val intent: Intent = mock {
            on { data }.thenReturn(uri)
        }

        val result = subject.checkSessionDetails(action, intent.data!!)
        Assert.assertEquals(LoginIntents.UnknownError, result)
    }

    companion object {
        private const val BASE_64_FULL_PAYLOAD = "ewogICAgIndhbGxldCI6IHsKICAgICAgICAiZ3VpZCI6ICI5OTE5ZWY0YS0wMjA2L" +
            "TQ5OTMtYTAzZC01ODI5YTgxYjBhMzYiLAogICAgICAgICJlbWFpbCI6ICJmb29AZXhhbXBsZS5jb20iLAoJICJzZXNzaW9uX2lkIjo" +
            "gIjEyMzQiLAogICAgICAgICJlbWFpbF9jb2RlIjogIjEyMzQiLAogICAgICAgICJpc19tb2JpbGVfc2V0dXAiOiB0cnVlLAogICAgI" +
            "CAgICJtb2JpbGVfZGV2aWNlX3R5cGUiOiAwLAogICAgICAgICJsYXN0X21uZW1vbmljX2JhY2t1cCI6IDE2MjE5NTQ3ODAsCiAgICA" +
            "gICAgImhhc19jbG91ZF9iYWNrdXAiOiB0cnVlLAogICAgICAgICJ0d29fZmFfdHlwZSI6IDQsCiAgICAgICAgIm5hYnUiOiB7CiAgI" +
            "CAgICAgICAgICJ1c2VyX2lkIjogImQ5N2ExYzQyLTg5MjgtNDE1OS05MDUzLTM5Y2QyYzMzZjk5NyIsCgkJCQkJCSJyZWNvdmVyeV9" +
            "0b2tlbiI6ICIwM2ZjODZiMi00ZGNiLTRmYWItOGE1NC1lMGM5NjFhM2IxODMiCiAgICAgICAgfSwKICAgICAgICAiZXhjaGFuZ2UiO" +
            "iB7CiAgICAgICAgICAgICJ1c2VyX2NyZWRlbnRpYWxzX2lkIjogImIzMTVhOTVjLTMyZWEtNDM4ZC1iY2IxLWYzMTM1ZGViOGM5YiI" +
            "sCiAgICAgICAgICAgICJ0d29fZmFfbW9kZSI6IHRydWUKICAgICAgICB9CiAgICB9LAogICAgInVuaWZpZWQiOiBmYWxzZSwKICAgI" +
            "CJ1cGdyYWRlYWJsZSI6IGZhbHNlLAogICAgIm1lcmdlYWJsZSI6IGZhbHNlLAogICAgInVzZXJfdHlwZSI6ICJXQUxMRVRfRVhDSEF" +
            "OR0VfTElOS0VEIgp9"
        private const val BASE_64_CORRUPT_PAYLOAD = "ewogICBHQVJCQUdFCn0="
    }
}
