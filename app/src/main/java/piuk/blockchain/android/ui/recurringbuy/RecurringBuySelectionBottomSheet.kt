package piuk.blockchain.android.ui.recurringbuy

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import com.blockchain.commonarch.presentation.base.HostedBottomSheet
import com.blockchain.commonarch.presentation.mvi.MviBottomSheet
import com.blockchain.componentlib.viewextensions.visible
import com.blockchain.componentlib.viewextensions.visibleIf
import com.blockchain.domain.paymentmethods.model.PaymentMethodType
import com.blockchain.core.recurringbuy.EligibleAndNextPaymentRecurringBuy
import com.blockchain.core.recurringbuy.RecurringBuyFrequency
import com.blockchain.presentation.koin.scopedInject
import com.blockchain.utils.capitalizeFirstChar
import com.blockchain.utils.isLastDayOfTheMonth
import info.blockchain.balance.FiatValue
import java.time.ZonedDateTime
import java.time.format.TextStyle
import java.util.Locale
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.DialogSheetRecurringBuyBinding
import piuk.blockchain.android.simplebuy.BuyFrequencySelected
import piuk.blockchain.android.simplebuy.SimpleBuyIntent
import piuk.blockchain.android.simplebuy.SimpleBuyModel
import piuk.blockchain.android.simplebuy.SimpleBuyState

class RecurringBuySelectionBottomSheet : MviBottomSheet<SimpleBuyModel, SimpleBuyIntent, SimpleBuyState,
    DialogSheetRecurringBuyBinding>() {

    interface Host : HostedBottomSheet.Host {
        fun onIntervalSelected(interval: RecurringBuyFrequency)
    }

    private val firstTimeAmountSpent: FiatValue? by lazy {
        arguments?.getSerializable(FIAT_AMOUNT_SPENT) as? FiatValue
    }

    private val cryptoCode: String? by lazy { arguments?.getString(CRYPTO_CODE) }

    private lateinit var selectedFrequency: RecurringBuyFrequency

    override val model: SimpleBuyModel by scopedInject()

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): DialogSheetRecurringBuyBinding =
        DialogSheetRecurringBuyBinding.inflate(inflater, container, false)

    override val host: Host by lazy {
        super.host as? Host ?: throw IllegalStateException(
            "Host fragment is not a RecurringBuySelectionBottomSheet.Host"
        )
    }

    override fun render(newState: SimpleBuyState) {
        val paymentMethodType = newState.selectedPaymentMethod?.paymentMethodType
        check(paymentMethodType != null)

        hideOrFillFrequencySelectorWithDates(
            newState.eligibleAndNextPaymentRecurringBuy,
            paymentMethodType
        )

        setPreselectedOrFirstFrequencyAvailable(
            currentFrequency = newState.recurringBuyFrequency,
            eligibleAndNextPaymentRecurringBuys = newState.eligibleAndNextPaymentRecurringBuy,
            paymentMethodType = paymentMethodType
        )
    }

    private fun isFirstTimeBuyer(): Boolean = firstTimeAmountSpent != null && cryptoCode != null

    private fun setPreselectedOrFirstFrequencyAvailable(
        currentFrequency: RecurringBuyFrequency,
        eligibleAndNextPaymentRecurringBuys: List<EligibleAndNextPaymentRecurringBuy>,
        paymentMethodType: PaymentMethodType
    ) {
        if (isFirstTimeBuyer()) {
            eligibleAndNextPaymentRecurringBuys.first { it.eligibleMethods.contains(paymentMethodType) }
                .let {
                    binding.recurringBuySelectionGroup.check(intervalToId(it.frequency))
                    selectedFrequency = it.frequency
                }
        } else {
            binding.recurringBuySelectionGroup.check(intervalToId(currentFrequency))
            selectedFrequency = currentFrequency
        }
    }

    private fun setViewForFirstTimeBuyer() {
        binding.apply {
            if (isFirstTimeBuyer()) {
                title.text = getString(
                    R.string.recurring_buy_first_time_title,
                    firstTimeAmountSpent!!.formatOrSymbolForZero(),
                    cryptoCode!!
                )
            } else {
                rbOneTime.visible()
            }
        }
    }

    override fun initControls(binding: DialogSheetRecurringBuyBinding) {
        setViewForFirstTimeBuyer()

        analytics.logEvent(RecurringBuyAnalytics.RecurringBuyViewed)

        with(binding) {
            recurringBuySelectionGroup.setOnCheckedChangeListener { _, checkedId ->
                selectedFrequency = idToInterval(checkedId)
            }
            recurringBuySelectCta.setOnClickListener {
                analytics.logEvent(
                    BuyFrequencySelected(
                        frequency = selectedFrequency.name
                    )
                )
                host.onIntervalSelected(selectedFrequency)
                dismiss()
            }
        }
    }

    private fun hideOrFillFrequencySelectorWithDates(
        eligibleAndNextPaymentRecurringBuys: List<EligibleAndNextPaymentRecurringBuy>,
        paymentMethodType: PaymentMethodType
    ) {

        eligibleAndNextPaymentRecurringBuys.forEach {
            binding.apply {
                when (it.frequency) {
                    RecurringBuyFrequency.DAILY -> {
                        if (it.eligibleMethods.contains(paymentMethodType)) {
                            rbDaily.visibleIf { it.eligibleMethods.contains(paymentMethodType) }
                        }
                    }
                    RecurringBuyFrequency.WEEKLY -> {
                        if (it.eligibleMethods.contains(paymentMethodType)) {
                            rbWeekly.apply {
                                visible()
                                text = getString(
                                    R.string.recurring_buy_frequency_subtitle,
                                    ZonedDateTime.parse(it.nextPaymentDate).dayOfWeek
                                        .getDisplayName(TextStyle.FULL, Locale.getDefault())
                                        .toString().capitalizeFirstChar()
                                )
                            }
                        }
                    }
                    RecurringBuyFrequency.BI_WEEKLY -> {
                        if (it.eligibleMethods.contains(paymentMethodType)) {
                            rbBiWeekly.apply {
                                visible()
                                text = getString(
                                    R.string.recurring_buy_frequency_subtitle_biweekly,
                                    ZonedDateTime.parse(it.nextPaymentDate).dayOfWeek
                                        .getDisplayName(TextStyle.FULL, Locale.getDefault())
                                        .toString().capitalizeFirstChar()
                                )
                            }
                        }
                    }
                    RecurringBuyFrequency.MONTHLY -> {
                        if (it.eligibleMethods.contains(paymentMethodType)) {
                            rbMonthly.apply {
                                visible()
                                text = if (ZonedDateTime.parse(it.nextPaymentDate).isLastDayOfTheMonth()) {
                                    getString(R.string.recurring_buy_frequency_subtitle_last_day_selector)
                                } else {
                                    getString(
                                        R.string.recurring_buy_frequency_subtitle_monthly,
                                        ZonedDateTime.parse(it.nextPaymentDate).dayOfMonth.toString()
                                    )
                                }
                            }
                        }
                    }
                    RecurringBuyFrequency.UNKNOWN, RecurringBuyFrequency.ONE_TIME -> {
                    }
                }
            }
        }
    }

    private fun intervalToId(interval: RecurringBuyFrequency) =
        when (interval) {
            RecurringBuyFrequency.DAILY -> R.id.rb_daily
            RecurringBuyFrequency.ONE_TIME -> R.id.rb_one_time
            RecurringBuyFrequency.WEEKLY -> R.id.rb_weekly
            RecurringBuyFrequency.BI_WEEKLY -> R.id.rb_bi_weekly
            RecurringBuyFrequency.MONTHLY -> R.id.rb_monthly
            RecurringBuyFrequency.UNKNOWN -> R.id.rb_one_time
        }

    private fun idToInterval(checkedId: Int) =
        when (checkedId) {
            R.id.rb_one_time -> RecurringBuyFrequency.ONE_TIME
            R.id.rb_daily -> RecurringBuyFrequency.DAILY
            R.id.rb_weekly -> RecurringBuyFrequency.WEEKLY
            R.id.rb_bi_weekly -> RecurringBuyFrequency.BI_WEEKLY
            R.id.rb_monthly -> RecurringBuyFrequency.MONTHLY
            else -> throw IllegalStateException("option selected RecurringBuyFrequency unknown")
        }

    companion object {
        const val FIAT_AMOUNT_SPENT = "fiat_amount_spent"
        const val CRYPTO_CODE = "crypto_asset_selected"
        fun newInstance(
            firstTimeAmountSpent: FiatValue? = null,
            cryptoValue: String? = null
        ): RecurringBuySelectionBottomSheet =
            RecurringBuySelectionBottomSheet().apply {
                arguments = Bundle().apply {
                    if (firstTimeAmountSpent != null) putSerializable(FIAT_AMOUNT_SPENT, firstTimeAmountSpent)
                    if (cryptoValue != null) putSerializable(CRYPTO_CODE, cryptoValue)
                }
            }
    }
}
