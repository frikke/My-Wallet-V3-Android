package com.blockchain.payments.googlepay

import android.app.Activity
import android.content.Intent
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.blockchain.payments.googlepay.interceptor.GooglePayResponseInterceptor
import com.blockchain.payments.googlepay.interceptor.GooglePayResponseInterceptorImpl
import com.blockchain.payments.googlepay.interceptor.OnPaymentDataReceivedListener
import com.blockchain.payments.googlepay.interceptor.PaymentDataMapper
import com.blockchain.payments.googlepay.interceptor.response.PaymentDataResponse
import com.blockchain.testutils.CoroutineTestRule
import com.blockchain.testutils.MockKRule
import com.google.android.gms.wallet.PaymentData
import io.mockk.Called
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule

class GooglePayResponseInterceptorImplTest {

    @get:Rule
    val mockKRule = MockKRule()

    @get:Rule
    val instantTaskExecutorRule: TestRule = InstantTaskExecutorRule()

    @ExperimentalCoroutinesApi
    @get:Rule
    var coroutineTestRule = CoroutineTestRule()

    companion object {
        private const val PAYMENT_DATA_TOKEN = "TOKEN"
    }

    @RelaxedMockK
    lateinit var onPaymentDataReceivedListener: OnPaymentDataReceivedListener

    @RelaxedMockK
    lateinit var paymentDataMapper: PaymentDataMapper

    @RelaxedMockK
    lateinit var intent: Intent

    @RelaxedMockK
    lateinit var paymentData: PaymentData

    @RelaxedMockK
    lateinit var paymentDataResponse: PaymentDataResponse

    private lateinit var subject: GooglePayResponseInterceptor

    @Before
    fun setUp() {
        subject = GooglePayResponseInterceptorImpl(paymentDataMapper, Dispatchers.IO).apply {
            setPaymentDataReceivedListener(onPaymentDataReceivedListener)
        }
    }

    @Test
    fun `interceptActivityResult - success`() {
        // Arrange
        val paymentMethodData = mockk<PaymentDataResponse.PaymentMethodData>(relaxed = true)
        val tokenizationData = mockk<PaymentDataResponse.PaymentMethodData.TokenizationData>(relaxed = true)
        coEvery { paymentDataMapper.getPaymentDataFromIntent(intent) } returns paymentData
        coEvery { paymentDataMapper.getPaymentDataResponse(paymentData) } returns paymentDataResponse
        coEvery { paymentDataResponse.paymentMethodData } returns paymentMethodData
        coEvery { paymentMethodData.tokenizationData } returns tokenizationData
        coEvery { tokenizationData.token } returns PAYMENT_DATA_TOKEN

        // Act
        subject.interceptActivityResult(
            GooglePayResponseInterceptor.GOOGLE_PAY_REQUEST_CODE, Activity.RESULT_OK, intent
        )

        // Assert
        coVerify {
            onPaymentDataReceivedListener.onPaymentTokenReceived(PAYMENT_DATA_TOKEN)
        }
        verify {
            onPaymentDataReceivedListener.onPaymentSheetClosed()
        }
    }

    @Test
    fun `interceptActivityResult - invalid requestCode`() {
        // Act
        subject.interceptActivityResult(0, 0, null)

        // Assert
        verify { onPaymentDataReceivedListener wasNot Called }
    }

    @Test
    fun `interceptActivityResult - cancelled`() {
        // Act
        subject.interceptActivityResult(
            GooglePayResponseInterceptor.GOOGLE_PAY_REQUEST_CODE, Activity.RESULT_CANCELED, null
        )

        // Assert
        verify {
            onPaymentDataReceivedListener.onPaymentCancelled()
            onPaymentDataReceivedListener.onPaymentSheetClosed()
        }
    }
}
