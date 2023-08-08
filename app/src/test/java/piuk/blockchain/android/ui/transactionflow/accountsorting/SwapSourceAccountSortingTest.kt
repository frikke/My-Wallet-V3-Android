package piuk.blockchain.android.ui.transactionflow.accountsorting

import com.blockchain.coincore.SingleAccount
import com.blockchain.featureflag.FeatureFlag
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.rxjava3.core.Single
import org.junit.Before
import org.junit.Test
import piuk.blockchain.android.ui.transfer.DefaultAccountsSorting
import piuk.blockchain.android.ui.transfer.SellAccountsSorting
import piuk.blockchain.android.ui.transfer.SwapSourceAccountsSorting

class SwapSourceAccountSortingTest {
    private val assetListOrderingFF: FeatureFlag = mock()
    private val defaultAccountsSorting: DefaultAccountsSorting = mock()
    private val sellAccountsSorting: SellAccountsSorting = mock()

    private lateinit var subject: SwapSourceAccountsSorting

    @Before
    fun setup() {
        subject = SwapSourceAccountsSorting(
            assetListOrderingFF = assetListOrderingFF,
            dashboardAccountsSorter = defaultAccountsSorting,
            sellAccountsSorting = sellAccountsSorting,
        )
    }

    @Test
    fun `when flag is off then dashboard sorter is invoked`() {
        whenever(assetListOrderingFF.enabled).thenReturn(Single.just(false))
        whenever(defaultAccountsSorting.sorter()).thenReturn(mock())

        val list = listOf<SingleAccount>()
        subject.sorter().invoke(list).test()

        verify(defaultAccountsSorting).sorter()
        verifyNoMoreInteractions(defaultAccountsSorting)
        verifyNoMoreInteractions(sellAccountsSorting)
    }

    @Test
    fun `when flag is on sell account sorting is invoked`() {
        whenever(assetListOrderingFF.enabled).thenReturn(Single.just(true))
        whenever(sellAccountsSorting.sorter()).thenReturn(mock())

        val list = listOf<SingleAccount>()
        subject.sorter().invoke(list).test()
        verify(sellAccountsSorting).sorter()
        verifyNoMoreInteractions(defaultAccountsSorting)
    }
}
