package piuk.blockchain.android.ui.blockchaincard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.blockchain.addressverification.ui.AddressDetails
import com.blockchain.addressverification.ui.AddressVerificationFragment
import com.blockchain.blockchaincard.domain.models.BlockchainCardAddress
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.ViewFragmentContainerBinding
import piuk.blockchain.android.support.SupportCentreActivity

class BlockchainCardKycAddressVerificationFragment : Fragment(), AddressVerificationFragment.Host {

    private lateinit var binding: ViewFragmentContainerBinding

    val address: BlockchainCardAddress by lazy {
        (arguments?.getParcelable(ADDRESS) as? BlockchainCardAddress)
            ?: throw IllegalStateException("Missing prefilled address")
    }

    private val addressVerificationFragment: AddressVerificationFragment?
        get() = childFragmentManager.findFragmentById(R.id.fragment_container) as? AddressVerificationFragment

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = ViewFragmentContainerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (addressVerificationFragment == null) {
            childFragmentManager.beginTransaction()
                .add(
                    R.id.fragment_container,
                    AddressVerificationFragment.newInstanceEditMode(
                        address.toAddressVerificationModel(),
                        allowManualOverride = false
                    )
                ).commitAllowingStateLoss()
        }
    }

    override fun launchContactSupport() {
        startActivity(SupportCentreActivity.newIntent(requireContext()))
    }

    override fun addressVerifiedSuccessfully(address: AddressDetails) {
        (requireActivity() as BlockchainCardActivity).updateKycAddress(address.toBlockchainCardAddress())
        requireActivity().supportFragmentManager.popBackStack()
    }

    companion object {
        private const val ADDRESS = "ADDRESS"

        fun newInstance(address: BlockchainCardAddress) =
            BlockchainCardKycAddressVerificationFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ADDRESS, address)
                }
            }
    }

    private fun BlockchainCardAddress.toAddressVerificationModel(): AddressDetails =
        AddressDetails(
            firstLine = line1,
            secondLine = line2,
            city = city,
            postCode = postCode,
            countryIso = country,
            stateIso = state
        )

    private fun AddressDetails.toBlockchainCardAddress(): BlockchainCardAddress =
        BlockchainCardAddress(
            line1 = firstLine,
            line2 = secondLine ?: "",
            postCode = postCode,
            city = city,
            state = stateIso ?: "",
            country = countryIso
        )
}
