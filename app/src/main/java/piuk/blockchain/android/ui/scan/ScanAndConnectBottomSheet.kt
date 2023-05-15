package piuk.blockchain.android.ui.scan

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.ui.unit.dp
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.commonarch.presentation.base.SlidingModalBottomDialog
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.tag.TagType
import com.blockchain.componentlib.tag.TagViewState
import com.blockchain.componentlib.viewextensions.visibleIf
import java.util.Locale
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.ItemScanBenefitLayoutBinding
import piuk.blockchain.android.databinding.ScanAndConnectBottomSheetLayoutBinding

class ScanAndConnectBottomSheet : SlidingModalBottomDialog<ScanAndConnectBottomSheetLayoutBinding>() {

    interface Host : SlidingModalBottomDialog.Host {
        fun onCameraAccessAllowed()
    }

    private val showCta by lazy {
        arguments?.getBoolean(SHOW_CTA_KEY) ?: false
    }

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): ScanAndConnectBottomSheetLayoutBinding =
        ScanAndConnectBottomSheetLayoutBinding.inflate(layoutInflater)

    override fun initControls(binding: ScanAndConnectBottomSheetLayoutBinding) {
        with(binding) {
            sheetHeader.apply {
                title = getString(com.blockchain.stringResources.R.string.scan_connect)
                onClosePress = { dismiss() }
            }
            cta.apply {
                text = getString(com.blockchain.stringResources.R.string.allow_camera_access)
                onClick = {
                    (host as? Host)?.onCameraAccessAllowed()
                    dismiss()
                }
                visibleIf { showCta }
            }
            recycler.layoutManager = LinearLayoutManager(context)
            recycler.adapter =
                ScanBenefitsAdapter(
                    listOf(
                        ScanBenefit(
                            title = getString(com.blockchain.stringResources.R.string.scan_friends_qr),
                            description = getString(
                                com.blockchain.stringResources.R.string.scan_friends_qr_description
                            ),
                            image = ImageResource.Local(com.blockchain.earn.R.drawable.ic_region, size = 24.dp)
                        ),
                        ScanBenefit(
                            title = getString(com.blockchain.stringResources.R.string.web_access),
                            description = getString(com.blockchain.stringResources.R.string.web_access_description),
                            image = ImageResource.Local(R.drawable.ic_subtract_pc)
                        ),
                        ScanBenefit(
                            title = getString(com.blockchain.stringResources.R.string.connect_to_dapps),
                            description = getString(
                                com.blockchain.stringResources.R.string.connect_to_dapps_description
                            ),
                            image = ImageResource.Local(R.drawable.ic_walletconnect)
                        )
                    )
                )
        }
    }

    companion object {
        private const val SHOW_CTA_KEY = "SHOW_CTA_KEY"
        fun newInstance(showCta: Boolean): ScanAndConnectBottomSheet = ScanAndConnectBottomSheet().apply {
            arguments = Bundle().apply {
                putBoolean(SHOW_CTA_KEY, showCta)
            }
        }
    }
}

private class ScanBenefitsAdapter(private val items: List<ScanBenefit>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        ScanBenefitsViewHolder(
            ItemScanBenefitLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as ScanBenefitsViewHolder).bind(items[position])
    }

    override fun getItemCount(): Int = items.size
}

private class ScanBenefitsViewHolder(private val binding: ItemScanBenefitLayoutBinding) :
    RecyclerView.ViewHolder(binding.root) {
    fun bind(scanBenefit: ScanBenefit) {
        with(binding.row) {
            startImageResource = scanBenefit.image
            primaryText = scanBenefit.title
            secondaryText = scanBenefit.description
            endImageResource = ImageResource.None
            endTag = if (scanBenefit.isBeta) {
                TagViewState(
                    context.getString(com.blockchain.stringResources.R.string.beta).uppercase(Locale.UK),
                    TagType.InfoAlt()
                )
            } else {
                null
            }
        }
    }
}

private data class ScanBenefit(
    val title: String,
    val description: String,
    val isBeta: Boolean = false,
    val image: ImageResource
)
