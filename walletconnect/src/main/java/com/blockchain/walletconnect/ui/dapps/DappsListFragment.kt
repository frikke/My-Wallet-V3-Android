package com.blockchain.walletconnect.ui.dapps

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Text
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rxjava3.subscribeAsState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.commonarch.presentation.base.updateToolbar
import com.blockchain.commonarch.presentation.mvi.MviComposeFragment
import com.blockchain.componentlib.basic.AppDivider
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.sheets.BottomSheetButton
import com.blockchain.componentlib.sheets.BottomSheetHostLayout
import com.blockchain.componentlib.sheets.BottomSheetOneButton
import com.blockchain.componentlib.sheets.BottomSheetTwoButtons
import com.blockchain.componentlib.sheets.ButtonType
import com.blockchain.componentlib.tablerow.DefaultTableRow
import com.blockchain.componentlib.theme.AppColors
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.presentation.koin.scopedInject
import com.blockchain.walletconnect.R
import com.blockchain.walletconnect.domain.WalletConnectAnalytics
import com.blockchain.walletconnect.domain.WalletConnectSession
import kotlinx.coroutines.launch

class DappsListFragment :
    MviComposeFragment<DappsListModel, DappsListIntent, DappsListState>() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        model.process(DappsListIntent.LoadDapps)
    }

    companion object {
        fun newInstance() = DappsListFragment()
    }

    override fun onResume() {
        super.onResume()
        updateToolbar(
            toolbarTitle = getString(com.blockchain.stringResources.R.string.account_wallet_connect),
            menuItems = emptyList()
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        analytics.logEvent(
            WalletConnectAnalytics.ConnectedDappsListViewed
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                Dapps(model)
            }
        }
    }

    override val model: DappsListModel by scopedInject()

    @OptIn(ExperimentalMaterialApi::class)
    @Composable
    private fun Dapps(model: DappsListModel) {
        val state by model.state.subscribeAsState(null)

        val scope = rememberCoroutineScope()

        var currentBottomSheet: SessionBottomSheet? by remember {
            mutableStateOf(null)
        }

        val modalBottomSheetState = rememberModalBottomSheetState(
            ModalBottomSheetValue.Hidden
        )

        fun hideSheet() {
            scope.launch { modalBottomSheetState.hide() }
        }

        fun showSheet() {
            scope.launch { modalBottomSheetState.show() }
        }

        BottomSheetHostLayout(
            modalBottomSheetState = modalBottomSheetState,
            onBackAction = { hideSheet() },
            sheetContent = {
                Spacer(modifier = Modifier.height(1.dp))
                currentBottomSheet?.let {
                    SheetLayout(
                        closeSheet = {
                            hideSheet()
                        },
                        bottomSheet = it
                    )
                }
            },
            content = {
                state?.let {
                    if (it.connectedSessions.isEmpty()) {
                        NoDapps()
                    } else {
                        DappsList(it.connectedSessions) { session ->
                            currentBottomSheet = SessionBottomSheet.Disconnect(
                                session = session,
                                onDisconnectClick = {
                                    currentBottomSheet = SessionBottomSheet.Confirmation(
                                        session,
                                        onConfirmClick = {
                                            model.process(DappsListIntent.Disconnect(session))
                                            hideSheet()
                                            analytics.logEvent(
                                                WalletConnectAnalytics.ConnectedDappActioned(
                                                    dappName = session.dAppInfo.peerMeta.name,
                                                    action =
                                                    WalletConnectAnalytics.DappConnectionAction.DISCONNECT_INTENT
                                                )
                                            )
                                        }
                                    )
                                    showSheet()

                                    analytics.logEvent(
                                        WalletConnectAnalytics.ConnectedDappActioned(
                                            dappName = session.dAppInfo.peerMeta.name,
                                            action = WalletConnectAnalytics.DappConnectionAction.DISCONNECT
                                        )
                                    )
                                }
                            )

                            showSheet()

                            analytics.logEvent(
                                WalletConnectAnalytics.ConnectedDappClicked(
                                    dappName = session.dAppInfo.peerMeta.name
                                )
                            )
                        }
                    }
                }
            }
        )
    }
}

@Composable
private fun SheetLayout(
    bottomSheet: SessionBottomSheet,
    closeSheet: () -> Unit
) {
    when (bottomSheet) {
        is SessionBottomSheet.Disconnect -> DisconnectBottomSheet(
            closeSheet = closeSheet,
            session = bottomSheet.session,
            onDisconnectClick = bottomSheet.onDisconnectClick
        )

        is SessionBottomSheet.Confirmation ->
            ConfirmActionBottomSheet(
                closeSheet = closeSheet,
                onConfirmationClick = bottomSheet.onConfirmClick,
                session = bottomSheet.session
            )
    }
}

@Composable
private fun ConfirmActionBottomSheet(
    closeSheet: () -> Unit,
    onConfirmationClick: () -> Unit,
    session: WalletConnectSession
) {
    BottomSheetTwoButtons(
        onCloseClick = closeSheet,
        title = stringResource(com.blockchain.stringResources.R.string.are_you_sure),
        subtitle = stringResource(
            com.blockchain.stringResources.R.string.you_are_about_disconnect,
            session.dAppInfo.peerMeta.name
        ),
        headerImageResource = ImageResource.Local(com.blockchain.componentlib.R.drawable.ic_warning),
        button1 = BottomSheetButton(
            type = ButtonType.DESTRUCTIVE_MINIMAL,
            text = stringResource(com.blockchain.stringResources.R.string.common_disconnect),
            onClick = onConfirmationClick
        ),
        button2 = BottomSheetButton(
            type = ButtonType.MINIMAL,
            text = stringResource(com.blockchain.stringResources.R.string.common_cancel),
            onClick = closeSheet
        )
    )
}

@Composable
private fun DisconnectBottomSheet(
    closeSheet: () -> Unit,
    onDisconnectClick: () -> Unit,
    session: WalletConnectSession
) {
    BottomSheetOneButton(
        onCloseClick = closeSheet,
        title = session.dAppInfo.peerMeta.name,
        subtitle = session.dAppInfo.peerMeta.description,
        headerImageResource = ImageResource.Remote(session.dAppInfo.peerMeta.uiIcon()),
        button = BottomSheetButton(
            type = ButtonType.DESTRUCTIVE_MINIMAL,
            text = stringResource(com.blockchain.stringResources.R.string.common_disconnect),
            onClick = onDisconnectClick
        )
    )
}

@Composable
private fun DappsList(sessions: List<WalletConnectSession>, onClick: (WalletConnectSession) -> Unit) {
    LazyColumn(
        modifier = Modifier.background(AppColors.backgroundSecondary)
    ) {
        items(
            items = sessions,
            itemContent = {
                DappListItem(it) {
                    onClick(it)
                }
            }
        )
    }
}

@Composable
private fun DappListItem(session: WalletConnectSession, onClick: () -> Unit) {
    Column {
        DefaultTableRow(
            startImageResource = ImageResource.Remote(
                session.dAppInfo.peerMeta.uiIcon()
            ),
            primaryText = session.dAppInfo.peerMeta.name,
            secondaryText = session.dAppInfo.peerMeta.url,
            endImageResource = ImageResource.Local(R.drawable.ic_more_horizontal),
            onClick = onClick
        )
        AppDivider()
    }
}

@Preview(showBackground = true)
@Composable
private fun NoDapps() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.backgroundSecondary)
            .padding(bottom = dimensionResource(com.blockchain.componentlib.R.dimen.size_epic)),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            contentScale = ContentScale.None,
            imageResource = ImageResource.Local(R.drawable.ic_world_blue).withTint(AppColors.primary)
        )
        Spacer(Modifier.size(dimensionResource(com.blockchain.componentlib.R.dimen.small_spacing)))
        Text(
            text = stringResource(com.blockchain.stringResources.R.string.no_dapps_connected),
            modifier = Modifier.padding(
                start = dimensionResource(com.blockchain.componentlib.R.dimen.small_spacing),
                end = dimensionResource(com.blockchain.componentlib.R.dimen.small_spacing)
            ),
            textAlign = TextAlign.Center,
            style = AppTheme.typography.title3,
            color = AppTheme.colors.title
        )
        Text(
            text = stringResource(com.blockchain.stringResources.R.string.connect_with_wallet_connect),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(
                start = dimensionResource(com.blockchain.componentlib.R.dimen.standard_spacing),
                end = dimensionResource(com.blockchain.componentlib.R.dimen.standard_spacing)
            ),
            style = AppTheme.typography.paragraph1,
            color = AppTheme.colors.body
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewNoDappsDark() {
    NoDapps()
}

sealed class SessionBottomSheet {
    abstract val session: WalletConnectSession

    class Disconnect(override val session: WalletConnectSession, val onDisconnectClick: () -> Unit) :
        SessionBottomSheet()

    class Confirmation(override val session: WalletConnectSession, val onConfirmClick: () -> Unit) :
        SessionBottomSheet()
}
