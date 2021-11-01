package piuk.blockchain.android.ui.customviews.account

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import info.blockchain.balance.Money
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single
import piuk.blockchain.android.R
import com.blockchain.coincore.BlockchainAccount
import piuk.blockchain.android.databinding.PendingBalanceRowBinding

interface CellDecorator {
    fun view(context: Context): Maybe<View>
    fun isEnabled(): Single<Boolean> = Single.just(true)
}

class DefaultCellDecorator : CellDecorator {
    override fun view(context: Context): Maybe<View> = Maybe.empty()
}

class PendingBalanceAccountDecorator(
    private val account: BlockchainAccount
) : CellDecorator {
    override fun view(context: Context): Maybe<View> {
        return account.pendingBalance.flatMapMaybe {
            if (it.isZero)
                Maybe.empty<View>()
            else Maybe.just(composePendingBalanceView(context, it))
        }
    }

    private fun composePendingBalanceView(context: Context, balance: Money): View {
        val binding = PendingBalanceRowBinding.inflate(LayoutInflater.from(context), null, false)

        binding.pendingBalance.text = balance.toStringWithSymbol()
        return binding.root
    }
}

fun ConstraintLayout.addViewToBottomWithConstraints(
    view: View,
    bottomOfView: View? = null,
    startOfView: View? = null,
    endOfView: View? = null
) {
    val tag = BOTTOM_VIEW_TAG
    val v: View? = this.findViewWithTag(tag)

    if (v == null) {
        view.id = View.generateViewId()
        view.tag = tag
        addView(view, ConstraintLayout.LayoutParams.MATCH_CONSTRAINT, ViewGroup.LayoutParams.WRAP_CONTENT)

        val constraintSet = ConstraintSet()
        constraintSet.clone(this)
        constraintSet.connect(
            view.id,
            ConstraintSet.BOTTOM,
            ConstraintSet.PARENT_ID,
            ConstraintSet.BOTTOM,
            resources.getDimensionPixelSize(VIEW_SPACING)
        )

        bottomOfView?.let {
            constraintSet.clear(it.id, ConstraintSet.BOTTOM)
            constraintSet.connect(
                view.id,
                ConstraintSet.TOP,
                it.id,
                ConstraintSet.BOTTOM,
                resources.getDimensionPixelSize(ADDED_VIEW_MARGIN)
            )
        }

        startOfView?.let {
            constraintSet.connect(view.id, ConstraintSet.START, it.id, ConstraintSet.START)
        } ?: constraintSet.connect(view.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)

        endOfView?.let {
            constraintSet.connect(view.id, ConstraintSet.END, it.id, ConstraintSet.END)
        } ?: constraintSet.connect(view.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)

        constraintSet.applyTo(this)

        if (this.paddingBottom == resources.getDimensionPixelSize(VIEW_SPACING)) {
            this.setPadding(0, 0, 0, 0)
        }
    }
}

fun ConstraintLayout.removePossibleBottomView() {
    val view: View? = findViewWithTag(BOTTOM_VIEW_TAG)
    view?.let {
        removeView(it)
        this.setPadding(0, 0, 0, resources.getDimensionPixelSize(VIEW_SPACING))
    }
}

private const val BOTTOM_VIEW_TAG = "BOTTOM_VIEW"
private const val ADDED_VIEW_MARGIN = R.dimen.smallest_margin
private const val VIEW_SPACING = R.dimen.very_small_margin
