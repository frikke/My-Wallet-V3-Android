package piuk.blockchain.android.rating.presentaion

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.DialogFragment
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import piuk.blockchain.android.rating.presentaion.composable.AppRatingCompletedScreen
import piuk.blockchain.android.rating.presentaion.composable.AppRatingStarsScreen

class AppRatingFragment : DialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setContent {
                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    startDestination = "11"
                ) {
                    composable("11") { AppRatingStarsScreen({ navController.navigate("22") }, {}) }
                    composable("22") { AppRatingCompletedScreen() }
                }
            }
        }
    }

    companion object {
        fun newInstance() = AppRatingFragment()
    }
}

