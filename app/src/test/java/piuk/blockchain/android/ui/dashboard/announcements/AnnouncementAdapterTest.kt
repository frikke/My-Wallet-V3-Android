package piuk.blockchain.android.ui.dashboard.announcements

import com.blockchain.domain.experiments.RemoteConfigService
import com.blockchain.nabu.UserIdentity
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.rxjava3.core.Single
import kotlinx.serialization.json.Json
import org.junit.Before
import org.junit.Test
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementConfigAdapterImpl.Companion.ANNOUNCE_KEY
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementConfigAdapterImpl.Companion.COWBOYS_SEE_ANNOUNCEMENTS_KEY

class AnnouncementAdapterTest {

    private val config: RemoteConfigService = mock()
    private val json: Json = Json
    private val userIdentity: UserIdentity = mock()
    private val expectedConfig = AnnounceConfig(interval = 5, order = listOf("TEST1", "TEST2", "TEST3"))
    private val jsonConfig = "{ \"interval\": 5, \"order\": [\"TEST1\", \"TEST2\", \"TEST3\"]}"

    private lateinit var announcementConfigAdapterImpl: AnnouncementConfigAdapterImpl

    @Before
    fun setup() {
        announcementConfigAdapterImpl = AnnouncementConfigAdapterImpl(
            config = config,
            json = json,
            userIdentity = userIdentity
        )
    }

    @Test
    fun `given a non-cowboys user then announcements are fetched`() {
        whenever(userIdentity.isCowboysUser()).thenReturn(Single.just(false))
        whenever(config.getRawJson(ANNOUNCE_KEY)).thenReturn(Single.just(jsonConfig))

        val result = announcementConfigAdapterImpl.announcementConfig.test()

        result.assertValue {
            it == expectedConfig
        }

        verify(userIdentity).isCowboysUser()
        verify(config).getRawJson(ANNOUNCE_KEY)
        verifyNoMoreInteractions(userIdentity)
        verifyNoMoreInteractions(config)
    }

    @Test
    fun `given a cowboys user with cowboys announcements flag is off then announcements are not fetched`() {
        whenever(userIdentity.isCowboysUser()).thenReturn(Single.just(true))
        whenever(config.getIfFeatureEnabled(COWBOYS_SEE_ANNOUNCEMENTS_KEY)).thenReturn(Single.just(false))

        val result = announcementConfigAdapterImpl.announcementConfig.test()

        result.assertValue {
            it == AnnounceConfig()
        }

        verify(userIdentity).isCowboysUser()
        verify(config).getIfFeatureEnabled(COWBOYS_SEE_ANNOUNCEMENTS_KEY)
        verifyNoMoreInteractions(userIdentity)
        verifyNoMoreInteractions(config)
    }

    @Test
    fun `given a cowboys user with cowboys announcements flag is on then announcements are fetched`() {
        whenever(userIdentity.isCowboysUser()).thenReturn(Single.just(true))
        whenever(config.getIfFeatureEnabled(COWBOYS_SEE_ANNOUNCEMENTS_KEY)).thenReturn(Single.just(true))
        whenever(config.getRawJson(ANNOUNCE_KEY)).thenReturn(Single.just(jsonConfig))

        val result = announcementConfigAdapterImpl.announcementConfig.test()

        result.assertValue {
            it == expectedConfig
        }

        verify(userIdentity).isCowboysUser()
        verify(config).getIfFeatureEnabled(COWBOYS_SEE_ANNOUNCEMENTS_KEY)
        verify(config).getRawJson(ANNOUNCE_KEY)
        verifyNoMoreInteractions(userIdentity)
        verifyNoMoreInteractions(config)
    }
}
