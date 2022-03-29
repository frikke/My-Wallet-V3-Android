package com.blockchain.walletconnect.ui.dapps

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Divider
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Text
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.unit.dp
import com.blockchain.commonarch.presentation.base.FlowFragment
import com.blockchain.commonarch.presentation.base.updateToolbar
import com.blockchain.commonarch.presentation.mvi.MviComposeFragment
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.sheets.BottomSheet
import com.blockchain.componentlib.sheets.BottomSheetButton
import com.blockchain.componentlib.sheets.BottomSheetHostLayout
import com.blockchain.componentlib.sheets.BottomSheetText
import com.blockchain.componentlib.sheets.ButtonType
import com.blockchain.componentlib.tablerow.DefaultTableRow
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.koin.scopedInject
import com.blockchain.walletconnect.R
import com.blockchain.walletconnect.domain.WalletConnectAnalytics
import com.blockchain.walletconnect.domain.WalletConnectSession

class DappsListFragment :
    MviComposeFragment<DappsListModel, DappsListIntent, DappsListState>(), FlowFragment {

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
            toolbarTitle = getString(R.string.account_wallet_connect),
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

    override fun onBackPressed(): Boolean = false

    @OptIn(ExperimentalMaterialApi::class)
    @Composable
    private fun Dapps(model: DappsListModel) {
        val state by model.state.subscribeAsState(null)

        var currentBottomSheet: SessionBottomSheet? by remember {
            mutableStateOf(null)
        }

        val modalBottomSheetState = rememberModalBottomSheetState(
            ModalBottomSheetValue.Hidden
        )

        var bottomSheetState by remember { mutableStateOf(ModalBottomSheetValue.Hidden) }

        BottomSheetHostLayout(
            modalBottomSheetState = modalBottomSheetState,
            onBackAction = { bottomSheetState = ModalBottomSheetValue.Hidden },
            stateFlow = bottomSheetState,
            sheetContent = {
                Spacer(modifier = Modifier.height(1.dp))
                currentBottomSheet?.let {
                    SheetLayout(
                        closeSheet = {
                            bottomSheetState = ModalBottomSheetValue.Hidden
                        },
                        bottomSheet = it
                    )
                }
            }, content = {
            state?.let {
                if (it.connectedSessions.isEmpty()) {
                    renderNoDapps()
                } else {
                    DappsList(it.connectedSessions) { session ->
                        currentBottomSheet = SessionBottomSheet.Disconnect(
                            session = session,
                            onDisconnectClick = {
                                currentBottomSheet = SessionBottomSheet.Confirmation(
                                    session,
                                    onConfirmClick = {
                                        model.process(DappsListIntent.Disconnect(session))
                                        bottomSheetState = ModalBottomSheetValue.Hidden
                                        analytics.logEvent(
                                            WalletConnectAnalytics.ConnectedDappActioned(
                                                dappName = session.dAppInfo.peerMeta.name,
                                                action = WalletConnectAnalytics.DappConnectionAction.DISCONNECT_INTENT
                                            )
                                        )
                                    }
                                )
                                bottomSheetState = ModalBottomSheetValue.Hidden
                                bottomSheetState = ModalBottomSheetValue.Expanded

                                analytics.logEvent(
                                    WalletConnectAnalytics.ConnectedDappActioned(
                                        dappName = session.dAppInfo.peerMeta.name,
                                        action = WalletConnectAnalytics.DappConnectionAction.DISCONNECT
                                    )
                                )
                            }
                        )
                        bottomSheetState = ModalBottomSheetValue.Expanded

                        analytics.logEvent(
                            WalletConnectAnalytics.ConnectedDappClicked(
                                dappName = session.dAppInfo.peerMeta.name
                            )
                        )
                    }
                }
            }
        },
            onCollapse = {
                bottomSheetState = ModalBottomSheetValue.Hidden
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
            ConfirmaActionBottomSheet(
                closeSheet = closeSheet,
                onConfirmationClick = bottomSheet.onConfirmClick,
                session = bottomSheet.session
            )
    }
}

@Composable
private fun ConfirmaActionBottomSheet(
    closeSheet: () -> Unit,
    onConfirmationClick: () -> Unit,
    session: WalletConnectSession
) {
    BottomSheet(
        onCloseClick = closeSheet,
        title = BottomSheetText(stringResource(R.string.are_you_sure)),
        subtitle = BottomSheetText(stringResource(R.string.you_are_about_disconnect, session.dAppInfo.peerMeta.name)),
        imageResource = ImageResource.Local(R.drawable.ic_warning),
        topButton = BottomSheetButton(
            text = stringResource(R.string.common_disconnect),
            onClick = onConfirmationClick,
            type = ButtonType.DESTRUCTIVE_MINIMAL
        ),
        bottomButton = BottomSheetButton(
            text = stringResource(R.string.common_cancel),
            onClick = closeSheet,
            type = ButtonType.MINIMAL
        )
    )
}

@Composable
private fun DisconnectBottomSheet(
    closeSheet: () -> Unit,
    onDisconnectClick: () -> Unit,
    session: WalletConnectSession
) {
    BottomSheet(
        onCloseClick = closeSheet,
        title = BottomSheetText(session.dAppInfo.peerMeta.name),
        subtitle = BottomSheetText(session.dAppInfo.peerMeta.description),
        imageResource = ImageResource.Remote(session.dAppInfo.peerMeta.uiIcon()),
        topButton = BottomSheetButton(
            text = stringResource(R.string.common_disconnect),
            onClick = onDisconnectClick,
            type = ButtonType.DESTRUCTIVE_MINIMAL
        ),
        bottomButton = null
    )
}

@Composable
private fun DappsList(sessions: List<WalletConnectSession>, onClick: (WalletConnectSession) -> Unit) {
    LazyColumn {
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
        Divider(color = AppTheme.colors.light, thickness = 1.dp)
    }
}

@Composable
private fun renderNoDapps() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = dimensionResource(R.dimen.size_epic)),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            contentScale = ContentScale.None,
            imageResource = ImageResource.Local(R.drawable.ic_world_blue)
        )
        Spacer(Modifier.size(dimensionResource(R.dimen.small_margin)))
        Text(
            text = stringResource(R.string.no_dapps_connected),
            modifier = Modifier.padding(
                start = dimensionResource(R.dimen.small_margin),
                end = dimensionResource(R.dimen.small_margin)
            ),
            textAlign = TextAlign.Center,
            style = AppTheme.typography.title3,
            color = AppTheme.colors.title
        )
        Text(
            text = stringResource(R.string.connect_with_wallet_connect),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(
                start = dimensionResource(R.dimen.standard_margin),
                end = dimensionResource(R.dimen.standard_margin)
            ),
            style = AppTheme.typography.paragraph1,
            color = AppTheme.colors.medium
        )
    }
}

sealed class SessionBottomSheet {
    abstract val session: WalletConnectSession

    class Disconnect(override val session: WalletConnectSession, val onDisconnectClick: () -> Unit) :
        SessionBottomSheet()

    class Confirmation(override val session: WalletConnectSession, val onConfirmClick: () -> Unit) :
        SessionBottomSheet()
}
