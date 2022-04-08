package piuk.blockchain.android.ui.maintenance.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.commonarch.presentation.mvi_v2.MVIFragment
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.NavigationRouter
import com.blockchain.commonarch.presentation.mvi_v2.bindViewModel
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.button.PrimaryButton
import com.blockchain.componentlib.button.SecondaryButton
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Grey900
import org.koin.androidx.viewmodel.ext.android.viewModel
import piuk.blockchain.android.R

class AppMaintenanceFragment : MVIFragment<AppMaintenanceViewState>(), NavigationRouter<AppMaintenanceNavigationEvent> {

    private lateinit var composeView: ComposeView

    private val viewModel: AppMaintenanceViewModel by viewModel()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).also { composeView = it }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViews()
        setupViewModel()
    }

    private fun setupViews() {
        composeView.apply {
            setContent {
                ScreenContent()
            }
        }
    }

    @Composable
    private fun ScreenContent() {
        val state = viewModel.viewState.collectAsState()

        Box {
            Image(
                modifier = Modifier.fillMaxSize(),
                imageResource = ImageResource.Local(
                    R.drawable.background_gradient
                ),
                contentScale = ContentScale.FillBounds
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        start = dimensionResource(R.dimen.standard_margin),
                        end = dimensionResource(R.dimen.standard_margin),
                        top = dimensionResource(R.dimen.standard_margin),
                        bottom = dimensionResource(R.dimen.large_margin),
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Image(
                    imageResource = ImageResource.Local(
                        R.drawable.ic_blockchain_logo_with_text
                    )
                )

                Image(
                    imageResource = ImageResource.Local(state.value.image)
                )

                Text(
                    modifier = Modifier.padding(start = dimensionResource(R.dimen.tiny_margin)),
                    style = AppTheme.typography.title3,
                    color = Grey900,
                    text = stringResource(id = state.value.title),
                )

                Spacer(Modifier.size(dimensionResource(R.dimen.tiny_margin)))

                Text(
                    modifier = Modifier.padding(start = dimensionResource(R.dimen.tiny_margin)),
                    style = AppTheme.typography.body1,
                    color = Grey900,
                    textAlign = TextAlign.Center,
                    text = stringResource(id = state.value.description)
                )

                Spacer(modifier = Modifier.weight(1f))

                state.value.button1Text?.let { button1Text ->
                    SecondaryButton(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(id = button1Text),
                        onClick = { },
                    )
                }

                if (state.value.button1Text != null && state.value.button2Text != null) {
                    Spacer(Modifier.size(dimensionResource(R.dimen.tiny_margin)))
                }

                state.value.button2Text?.let { button2Text ->
                    PrimaryButton(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(id = button2Text),
                        onClick = { }
                    )
                }
            }
        }
    }

    private fun setupViewModel() {
        bindViewModel(viewModel, this, ModelConfigArgs.NoArgs)
    }

    override fun onStateUpdated(state: AppMaintenanceViewState) {
    }

    override fun route(navigationEvent: AppMaintenanceNavigationEvent) {
    }

    companion object {
        fun newInstance() = AppMaintenanceFragment()
    }

    @Preview
    @Composable
    private fun PreviewScreenContent() {
        ScreenContent()
    }
}
