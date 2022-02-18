package piuk.blockchain.android.ui.scan

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.commonarch.presentation.base.SlidingModalBottomDialog
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.tag.TagType
import com.blockchain.componentlib.tag.TagViewState
import java.util.Locale
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.ItemScanBenefitLayoutBinding
import piuk.blockchain.android.databinding.ScanAndConnectBottomSheetLayoutBinding

class ScanAndConnectBottomSheet : SlidingModalBottomDialog<ScanAndConnectBottomSheetLayoutBinding>() {

    interface Host : SlidingModalBottomDialog.Host {
        fun onCameraAccessAllowed()
    }

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): ScanAndConnectBottomSheetLayoutBinding =
        ScanAndConnectBottomSheetLayoutBinding.inflate(layoutInflater)

    override fun initControls(binding: ScanAndConnectBottomSheetLayoutBinding) {
        with(binding) {

            sheetHeader.apply {
                title = getString(R.string.scan_connect)
                onClosePress = { dismiss() }
            }
            cta.apply {
                text = getString(R.string.allow_camera_access)
                onClick = {
                    (host as? Host)?.onCameraAccessAllowed()
                }
            }
            recycler.layoutManager = LinearLayoutManager(context)
            recycler.adapter =
                ScanBenefitsAdapter(
                    listOf(
                        ScanBenefit(
                            title = getString(R.string.scan_friends_qr),
                            description = getString(R.string.scan_friends_qr_description),
                            image = ImageResource.Local(R.drawable.ic_union)
                        ),
                        ScanBenefit(
                            title = getString(R.string.web_access),
                            description = getString(R.string.web_access_description),
                            image = ImageResource.Local(R.drawable.ic_subtract_pc)
                        ),
                        ScanBenefit(
                            title = getString(R.string.connect_to_dapps),
                            description = getString(R.string.connect_to_dapps_description),
                            image = ImageResource.Local(R.drawable.ic_walletconnect),
                            isBeta = true
                        )
                    )
                )
        }
    }

    companion object {
        fun newInstance(): ScanAndConnectBottomSheet = ScanAndConnectBottomSheet()
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
            endTag = if (scanBenefit.isBeta)
                TagViewState(
                    context.getString(R.string.beta).uppercase(Locale.UK), TagType.InfoAlt()
                ) else null
        }
    }
}

private data class ScanBenefit(
    val title: String,
    val description: String,
    val isBeta: Boolean = false,
    val image: ImageResource
)
