package piuk.blockchain.android.ui.settings.v2.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.blockchain.koin.scopedInject
import piuk.blockchain.android.databinding.FragmentUpdateEmailBinding
import piuk.blockchain.android.ui.base.mvi.MviFragment

// TODO AND-5624
class UpdateEmailFragment : MviFragment<ProfileModel, ProfileIntent, ProfileState, FragmentUpdateEmailBinding>() {

    override val model: ProfileModel by scopedInject()

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentUpdateEmailBinding =
        FragmentUpdateEmailBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    override fun render(newState: ProfileState) {
    }
}
