package piuk.blockchain.android.cards

import com.blockchain.commonarch.presentation.base.FlowFragment

interface AddCardFlowFragment : FlowFragment {
    val navigator: AddCardNavigator
    val cardDetailsPersistence: CardDetailsPersistence
}
