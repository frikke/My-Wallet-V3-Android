package piuk.blockchain.android.rating.presentaion

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.DialogFragment
import com.blockchain.koin.payloadScope
import org.koin.androidx.viewmodel.ViewModelOwner
import org.koin.androidx.viewmodel.scope.getViewModel
import piuk.blockchain.android.rating.presentaion.composable.AppRatingNavHost

class AppRatingFragment : DialogFragment() {

    private val viewModel: AppRatingViewModel by lazy {
        payloadScope.getViewModel(owner = { ViewModelOwner.from(this) })
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setContent {
                AppRatingNavHost(
                    viewModel = viewModel,
                    onDismiss = { dismiss() }
                )
            }
        }
    }

    companion object {
        fun newInstance() = AppRatingFragment()
    }
}

