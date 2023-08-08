package com.blockchain.analytics.data

import android.content.SharedPreferences
import com.blockchain.analytics.AnalyticsEvent
import com.google.firebase.analytics.FirebaseAnalytics
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Test

class AnalyticsImplTest {

    private val mockFirebase: FirebaseAnalytics = mock()
    private val mockEditor: SharedPreferences.Editor = mock()

    private val event = object : AnalyticsEvent {
        override val event: String
            get() = "name"
        override val params: Map<String, String> = emptyMap()
    }

    @Test
    fun `should log custom event`() {
        val mockStore = mock<SharedPreferences>()

        AnalyticsImpl(
            firebaseAnalytics = mockFirebase,
            store = mockStore,
            nabuAnalytics = mock(),
            remoteLogger = mock(),
            nabuAnalyticsSettings = mock()
        ).logEvent(event)

        verify(mockFirebase).logEvent(event.event, null)
    }

    @Test
    fun `should log once event once`() {
        val mockStore = mock<SharedPreferences> {
            on { contains(any()) } doReturn false
            on { edit() } doReturn mockEditor
        }

        whenever(mockEditor.putBoolean(any(), any())).thenReturn(mockEditor)

        AnalyticsImpl(
            firebaseAnalytics = mockFirebase,
            store = mockStore,
            nabuAnalytics = mock(),
            remoteLogger = mock(),
            nabuAnalyticsSettings = mock()
        ).logEventOnce(event)
        verify(mockFirebase).logEvent(event.event, null)
    }

    @Test
    fun `should not log once event again`() {
        val mockStore = mock<SharedPreferences> {
            on { contains(any()) } doReturn true
            on { edit() } doReturn mockEditor
        }

        AnalyticsImpl(
            firebaseAnalytics = mockFirebase,
            store = mockStore,
            nabuAnalytics = mock(),
            remoteLogger = mock(),
            nabuAnalyticsSettings = mock()
        ).logEventOnce(event)
        verify(mockFirebase, never()).logEvent(event.event, null)
    }
}
