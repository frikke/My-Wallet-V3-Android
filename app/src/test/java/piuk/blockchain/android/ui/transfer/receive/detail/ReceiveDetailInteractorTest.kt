package piuk.blockchain.android.ui.transfer.receive.detail

import com.blockchain.coincore.loader.UniversalDynamicAssetRepository
import com.blockchain.core.chains.EvmNetwork
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.rxjava3.core.Maybe
import org.junit.Before
import org.junit.Test

class ReceiveDetailInteractorTest {

    private lateinit var subject: ReceiveDetailInteractor
    private val evmNetwork = mock<EvmNetwork>()
    private val dynamicAssetRepository = mock<UniversalDynamicAssetRepository>()

    @Before
    fun setup() {
        subject = ReceiveDetailInteractor(
            dynamicAssetRepository = dynamicAssetRepository
        )
    }

    @Test
    fun `getEvmNetworkForCurrency calls dynamicAssetRepository`() {
        whenever(dynamicAssetRepository.getEvmNetworkForCurrency("USDC")).thenReturn(Maybe.just(evmNetwork))
        val test = subject.getEvmNetworkForCurrency("USDC").test()
        test.assertValue {
            it == evmNetwork
        }

        verify(dynamicAssetRepository).getEvmNetworkForCurrency("USDC")
        verifyNoMoreInteractions(dynamicAssetRepository)
    }
}
