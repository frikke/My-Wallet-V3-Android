package piuk.blockchain.android.ui.customviews

import android.content.Context
import android.os.Parcelable
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import androidx.annotation.DrawableRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import com.blockchain.componentlib.viewextensions.gone
import com.blockchain.componentlib.viewextensions.visibleIf
import com.bumptech.glide.Glide
import java.io.Serializable
import kotlinx.parcelize.Parcelize
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.VerifyIdentityBenefitsLayoutBinding

class VerifyIdentityBenefitsView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    ConstraintLayout(context, attrs) {

    private val binding: VerifyIdentityBenefitsLayoutBinding = VerifyIdentityBenefitsLayoutBinding.inflate(
        LayoutInflater.from(context),
        this,
        true
    )

    fun initWithBenefits(
        benefits: List<VerifyIdentityItem>,
        title: String,
        description: String,
        icon: Serializable? = null,
        primaryButton: ButtonOptions,
        secondaryButton: ButtonOptions,
        footerText: String = "",
        showSheetIndicator: Boolean = true,
        titleGravity: Int = Gravity.START,
        descriptionGravity: Int = Gravity.START
    ) {
        with(binding) {
            kycBenefitsIntroTitle.apply {
                text = title
                gravity = titleGravity
            }
            kycBenefitsIntroDescription.apply {
                text = description
                gravity = descriptionGravity
            }
            icon?.let {
                Glide.with(context)
                    .load(icon)
                    .error(R.drawable.ic_cash)
                    .into(kycBenefitsDefaultSymbol)
            } ?: kycBenefitsDefaultSymbol.gone()

            kycBenefitsPositiveAction.apply {
                visibleIf { primaryButton.visible }
                onClick = {
                    primaryButton.cta()
                }
                text = primaryButton.text ?: context.getString(
                    com.blockchain.stringResources.R.string.fiat_funds_no_kyc_positive_action
                )
            }

            kycBenefitsNegativeAction.apply {
                visibleIf { secondaryButton.visible }
                onClick = {
                    secondaryButton.cta()
                }
                text = secondaryButton.text ?: context.getString(
                    com.blockchain.stringResources.R.string.fiat_funds_no_kyc_negative_action
                )
            }

            this.footerText.visibleIf { footerText.isNotEmpty() }
            this.footerText.text = footerText

            val adapter = BenefitsDelegateAdapter().apply {
                items = benefits
            }

            rvBenefits.layoutManager = LinearLayoutManager(context)
            rvBenefits.adapter = adapter

            if (!showSheetIndicator) {
                kycBenefitsSheetIndicator.gone()
            }
        }
    }
}

@Parcelize
data class VerifyIdentityNumericBenefitItem(override val title: String, override val subtitle: String) :
    VerifyIdentityItem,
    Parcelable

data class VerifyIdentityIconedBenefitItem(
    override val title: String,
    override val subtitle: String,
    @DrawableRes val icon: Int
) : VerifyIdentityItem

data class ButtonOptions(val visible: Boolean, val text: String? = null, val cta: () -> Unit = {})

interface VerifyIdentityItem {
    val title: String
    val subtitle: String
}
