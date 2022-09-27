package com.blockchain.walletconnect.ui.networks

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import com.blockchain.commonarch.presentation.mvi_v2.MVIBottomSheet
import com.blockchain.componentlib.basic.ComposeColors
import com.blockchain.componentlib.basic.ComposeGravities
import com.blockchain.componentlib.basic.ComposeTypographies
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.basic.SimpleText
import com.blockchain.componentlib.control.Radio
import com.blockchain.componentlib.control.RadioButtonState
import com.blockchain.componentlib.divider.HorizontalDivider
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Grey000
import com.blockchain.componentlib.utils.clickableNoEffect
import com.blockchain.koin.payloadScope
import com.blockchain.walletconnect.R
import com.blockchain.walletconnect.domain.WalletConnectSession
import org.koin.android.scope.AndroidScopeComponent
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.scope.Scope

class SelectNetworkBottomSheet :
    MVIBottomSheet<SelectNetworkViewState>(),
    AndroidScopeComponent {

    interface Host : MVIBottomSheet.Host {
        fun onNetworkSelected(session: WalletConnectSession, networkInfo: NetworkInfo)
    }

    override val host: Host by lazy {
        super.host as? Host ?: throw IllegalStateException(
            "Host fragment is not a SelectNetworkBottomSheet.Host"
        )
    }

    override val scope: Scope
        get() = payloadScope

    private val viewModel: SelectNetworkViewModel by viewModel()

    private val session: WalletConnectSession by lazy {
        arguments?.getSerializable(SESSION_KEY) as WalletConnectSession
    }

    private lateinit var selectedNetwork: NetworkInfo

    override fun onStateUpdated(state: SelectNetworkViewState) {}

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        viewModel.onIntent(SelectNetworkIntents.LoadSupportedNetworks(session.dAppInfo.chainId))
        return ComposeView(requireContext()).apply {
            setContent {
                SheetContent()
            }
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        host.onNetworkSelected(
            session = WalletConnectSession(
                url = session.url,
                dAppInfo = session.dAppInfo.copy(
                    chainId = selectedNetwork.chainId
                ),
                walletInfo = session.walletInfo
            ),
            networkInfo = selectedNetwork
        )
    }

    @Composable
    private fun SheetContent() {
        val lifecycleOwner = LocalLifecycleOwner.current
        val stateFlowLifeCycleAware = remember(viewModel.viewState, lifecycleOwner) {
            viewModel.viewState.flowWithLifecycle(lifecycleOwner.lifecycle, Lifecycle.State.STARTED)
        }
        val viewState: SelectNetworkViewState? by stateFlowLifeCycleAware.collectAsState(initial = null)
        viewState?.let { currentState ->
            selectedNetwork = currentState.selectedNetwork ?: NetworkInfo.defaultEvmNetworkInfo
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .background(Color.White, RoundedCornerShape(dimensionResource(id = R.dimen.tiny_spacing)))
                    .padding(AppTheme.dimensions.smallSpacing)
            ) {
                Image(
                    modifier = Modifier
                        .padding(top = AppTheme.dimensions.tinySpacing)
                        .align(Alignment.CenterHorizontally),
                    imageResource = ImageResource.Local(R.drawable.vector_sheet_indicator_small)
                )
                Row(
                    modifier = Modifier
                        .padding(bottom = AppTheme.dimensions.standardSpacing)
                ) {
                    SimpleText(
                        modifier = Modifier
                            .padding(top = AppTheme.dimensions.smallestSpacing)
                            .weight(1.0f),
                        text = stringResource(
                            com.blockchain.stringResources.R.string.wallet_connect_switch_network_title
                        ),
                        style = ComposeTypographies.Title3,
                        color = ComposeColors.Title,
                        gravity = ComposeGravities.Start
                    )
                    Image(
                        modifier = Modifier
                            .padding(
                                top = AppTheme.dimensions.smallestSpacing
                            )
                            .clickableNoEffect { dismiss() },
                        imageResource = ImageResource.Local(R.drawable.ic_close_circle)
                    )
                }
                NetworksList(
                    currentState.networks.map {
                        NetworkItem(
                            it.name,
                            it.chainId,
                            it.logo,
                            it.chainId == currentState.selectedNetwork?.chainId
                        )
                    }
                )
            }
        }
    }

    @Composable
    private fun NetworksList(networkItems: List<NetworkItem>) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(bottom = AppTheme.dimensions.epicSpacing)
                .background(Grey000, RoundedCornerShape(dimensionResource(id = R.dimen.tiny_spacing))),
        ) {
            itemsIndexed(
                items = networkItems,
                itemContent = { index, networkItem ->
                    NetworkListItem(networkItem)
                    if (index < networkItems.lastIndex)
                        HorizontalDivider(
                            modifier = Modifier.fillMaxWidth(),
                            dividerColor = AppTheme.colors.medium
                        )
                }
            )
        }
    }

    @Composable
    private fun NetworkListItem(item: NetworkItem) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(AppTheme.dimensions.standardSpacing),
            horizontalArrangement = Arrangement.Center
        ) {
            Image(
                imageResource = item.image?.let { image -> ImageResource.Remote(image) }
                    ?: ImageResource.Local(R.drawable.ic_default_asset_logo),
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .size(dimensionResource(com.blockchain.componentlib.R.dimen.standard_spacing))
            )
            Spacer(modifier = Modifier.size(AppTheme.dimensions.standardSpacing))
            SimpleText(
                modifier = Modifier
                    .weight(1.0f)
                    .padding(top = AppTheme.dimensions.verySmallSpacing),
                text = item.networkName,
                style = ComposeTypographies.Body1,
                color = ComposeColors.Body,
                gravity = ComposeGravities.Start
            )
            Radio(
                state = if (item.isSelected) {
                    RadioButtonState.Selected
                } else {
                    RadioButtonState.Unselected
                },
                onSelectedChanged = { isChecked ->
                    if (isChecked) {
                        viewModel.onIntent(SelectNetworkIntents.SelectNetwork(item.chainId))
                    }
                }
            )
        }
    }

    data class NetworkItem(
        val networkName: String,
        val chainId: Int,
        val image: String?,
        val isSelected: Boolean
    )

    companion object {
        private const val SESSION_KEY = "SESSION_KEY"
        fun newInstance(session: WalletConnectSession) =
            SelectNetworkBottomSheet().apply {
                arguments = Bundle().also {
                    it.putSerializable(SESSION_KEY, session)
                }
            }
    }

    @Preview
    @Composable
    fun NetworkListItem_Preview() {
        AppTheme {
            AppSurface {
                NetworkListItem(
                    item = NetworkItem(
                        networkName = "Ethereum",
                        chainId = 1,
                        image = null,
                        isSelected = false
                    )
                )
            }
        }
    }

    @Preview
    @Composable
    fun NetworkList_Preview() {
        AppTheme {
            AppSurface {
                NetworksList(
                    networkItems = listOf(
                        NetworkItem(
                            networkName = "Ethereum",
                            chainId = 1,
                            image = null,
                            isSelected = false
                        ),
                        NetworkItem(
                            networkName = "Polygon",
                            chainId = 137,
                            image = null,
                            isSelected = true
                        ),
                        NetworkItem(
                            networkName = "BSC",
                            chainId = 56,
                            image = null,
                            isSelected = false
                        )
                    )
                )
            }
        }
    }
}
