package piuk.blockchain.android.ui.transfer.send

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.TxSourceState
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.StatusLineInfoBinding
import piuk.blockchain.android.ui.customviews.account.CellDecorator

class SendCellDecorator(private val cryptoAccount: CryptoAccount) : CellDecorator {

    override fun view(context: Context): Maybe<View> = cryptoAccount.sourceState.flatMapMaybe {
        when (it) {
            TxSourceState.NO_FUNDS -> statusInfoText(
                context.getString(com.blockchain.stringResources.R.string.send_state_no_funds),
                context
            )
            TxSourceState.NOT_SUPPORTED -> statusInfoText(
                context.getString(com.blockchain.stringResources.R.string.send_state_not_supported),
                context
            )
            TxSourceState.FUNDS_LOCKED -> statusInfoText(
                context.getString(com.blockchain.stringResources.R.string.send_state_locked_funds_1),
                context
            )
            TxSourceState.NOT_ENOUGH_GAS -> statusInfoText(
                context.getString(com.blockchain.stringResources.R.string.send_state_not_enough_gas),
                context
            )
            TxSourceState.TRANSACTION_IN_FLIGHT -> statusInfoText(
                context.getString(com.blockchain.stringResources.R.string.send_state_send_in_flight),
                context
            )
            else -> Maybe.empty<View>()
        }
    }

    private fun statusInfoText(text: String, context: Context): Maybe<View> {
        val binding = StatusLineInfoBinding.inflate(
            LayoutInflater.from(context),
            null,
            false
        )
        binding.message.text = text
        return Maybe.just(binding.root)
    }

    override fun isEnabled(): Single<Boolean> = cryptoAccount.sourceState.map {
        it == TxSourceState.CAN_TRANSACT
    }
}
