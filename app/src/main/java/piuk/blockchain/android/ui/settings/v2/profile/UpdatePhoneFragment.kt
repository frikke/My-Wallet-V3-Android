package piuk.blockchain.android.ui.settings.v2.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.blockchain.koin.scopedInject
import piuk.blockchain.android.databinding.FragmentUpdatePhoneBinding
import piuk.blockchain.android.ui.base.mvi.MviFragment

// TODO AND-5625
class UpdatePhoneFragment : MviFragment<ProfileModel, ProfileIntent, ProfileState, FragmentUpdatePhoneBinding>() {

    override val model: ProfileModel by scopedInject()

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentUpdatePhoneBinding =
        FragmentUpdatePhoneBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    override fun render(newState: ProfileState) {
    }
}
