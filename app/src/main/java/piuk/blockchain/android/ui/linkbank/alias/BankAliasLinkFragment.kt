package piuk.blockchain.android.ui.linkbank.alias

import android.content.res.Resources
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.blockchain.commonarch.presentation.mvi_v2.MVIFragment
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.NavigationRouter
import com.blockchain.commonarch.presentation.mvi_v2.bindViewModel
import com.blockchain.componentlib.basic.ComposeColors
import com.blockchain.componentlib.basic.ComposeGravities
import com.blockchain.componentlib.basic.ComposeTypographies
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.basic.SimpleText
import com.blockchain.componentlib.button.ButtonState
import com.blockchain.componentlib.button.PrimaryButton
import com.blockchain.componentlib.control.Search
import com.blockchain.componentlib.divider.HorizontalDivider
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.domain.common.model.ServerSideUxErrorInfo
import com.blockchain.domain.paymentmethods.model.AliasInfo
import com.blockchain.koin.payloadScope
import org.koin.android.scope.AndroidScopeComponent
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.scope.Scope
import piuk.blockchain.android.R

class BankAliasLinkFragment : MVIFragment<BankAliasLinkViewState>(), AndroidScopeComponent {

    override val scope: Scope = payloadScope

    private val viewModel: BankAliasLinkViewModel by viewModel()

    private val currency: String by lazy {
        arguments?.getString(CURRENCY).orEmpty()
    }

    private val navigationRouter: NavigationRouter<BankAliasNavigationEvent> by lazy {
        activity as? NavigationRouter<BankAliasNavigationEvent>
            ?: error("host does not implement NavigationRouter<BankAliasNavigationEvent>")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setContent {
                AppTheme {
                    ScreenContent()
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bindViewModel(
            viewModel = viewModel,
            navigator = navigationRouter,
            args = ModelConfigArgs.NoArgs
        )
    }

    override fun onStateUpdated(state: BankAliasLinkViewState) {
    }

    @Composable
    private fun ScreenContent() {
        val state = viewModel.viewState.collectAsState()

        LinkWithAliasScreen(
            viewState = state.value,
            aliasUpdated = ::onAliasUpdated,
            searchAccountClicked = { searchAccount(state.value.alias) },
            continueClicked = ::onAliasInfoRetrieved
        )
    }

    private fun onAliasUpdated(alias: String) {
        viewModel.onIntent(BankAliasLinkIntent.AliasUpdated(alias))
    }

    private fun searchAccount(alias: String?) {
        alias?.let {
            viewModel.onIntent(BankAliasLinkIntent.LoadBeneficiaryInfo(currency = currency, address = it))
        }
    }

    private fun onAliasInfoRetrieved(alias: String) {
        viewModel.onIntent(BankAliasLinkIntent.ActivateBeneficiary(alias))
    }

    companion object {
        private const val CURRENCY: String = "CURRENCY"

        fun newInstance(currency: String): BankAliasLinkFragment =
            BankAliasLinkFragment().apply {
                arguments = Bundle().apply {
                    putString(CURRENCY, currency)
                }
            }
    }
}

fun AliasInfo.toDisplayItems(resources: Resources): List<Pair<String, String>> {
    val displayItems = mutableListOf<Pair<String, String>>()
    bankName?.let { displayItems.add(Pair(resources.getString(R.string.bank_name), it)) }
    alias?.let { displayItems.add(Pair(resources.getString(R.string.alias), it)) }
    accountHolder?.let { displayItems.add(Pair(resources.getString(R.string.account_holder), it)) }
    accountType?.let { displayItems.add(Pair(resources.getString(R.string.account_type), it)) }
    cbu?.let { displayItems.add(Pair(resources.getString(R.string.bank_alias_cbu), it)) }
    cuil?.let { displayItems.add(Pair(resources.getString(R.string.bank_alias_cuil), it)) }

    return displayItems
}

@Composable
fun LinkWithAliasScreen(
    viewState: BankAliasLinkViewState,
    aliasUpdated: (String) -> Unit,
    searchAccountClicked: () -> Unit,
    continueClicked: (String) -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.standard_margin))
    ) {
        Header(
            alias = viewState.alias,
            aliasUpdated = aliasUpdated,
            isReadOnly = viewState.ctaState == ButtonState.Loading
        )

        Crossfade(targetState = viewState) { screen ->
            when {
                screen.showAliasInput -> AliasInputScreen(
                    buttonState = screen.ctaState,
                    searchAccountClicked = searchAccountClicked,
                )
                screen.error is AliasError.ServerSideUxError -> AliasErrorScreen(
                    errorDescription = screen.error.serverSideUxErrorInfo.description,
                    buttonState = screen.ctaState,
                    searchAccountClicked = searchAccountClicked,
                )
                screen.aliasInfo != null -> AliasInfoScreen(
                    bankInfoItems = screen.aliasInfo.toDisplayItems(LocalContext.current.resources),
                    buttonState = screen.ctaState,
                    continueClicked = { screen.alias?.let { continueClicked(it) } }
                )
            }
        }
    }
}

@Composable
fun AliasInputScreen(
    buttonState: ButtonState,
    searchAccountClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        SimpleText(
            text = stringResource(id = R.string.bank_alias_info),
            modifier = Modifier.fillMaxWidth(),
            style = ComposeTypographies.Caption1,
            color = ComposeColors.Body,
            gravity = ComposeGravities.Start
        )

        Spacer(modifier = modifier.weight(1f))

        Footer(
            buttonText = stringResource(id = R.string.bank_alias_search_account),
            buttonState = buttonState,
            onButtonClick = searchAccountClicked
        )
    }
}

@Composable
fun AliasErrorScreen(
    errorDescription: String,
    buttonState: ButtonState,
    searchAccountClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        SimpleText(
            text = errorDescription,
            modifier = Modifier.fillMaxWidth(),
            style = ComposeTypographies.Caption1,
            color = ComposeColors.Error,
            gravity = ComposeGravities.Start
        )

        Spacer(modifier = modifier.weight(1f))

        Footer(
            showBankTransferInfo = false,
            buttonText = stringResource(id = R.string.bank_alias_search_account),
            buttonState = buttonState,
            onButtonClick = searchAccountClicked
        )
    }
}

@Composable
fun AliasInfoScreen(
    bankInfoItems: List<Pair<String, String>>,
    buttonState: ButtonState,
    continueClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .weight(1f)
                .fillMaxWidth()
        ) {
            bankInfoItems.forEachIndexed { index, item ->
                BankInfoItem(
                    title = item.first,
                    description = item.second,
                    showBottomDivider = index == bankInfoItems.size - 1
                )
            }
        }

        Footer(
            buttonText = stringResource(id = R.string.common_continue),
            buttonState = buttonState,
            onButtonClick = continueClicked
        )
    }
}

@Composable
fun Header(
    alias: String? = null,
    isReadOnly: Boolean,
    aliasUpdated: (String) -> Unit
) {
    Column {
        SimpleText(
            text = stringResource(id = R.string.bank_alias_types),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = dimensionResource(id = R.dimen.smallest_margin)),
            style = ComposeTypographies.Paragraph2,
            color = ComposeColors.Body,
            gravity = ComposeGravities.Start
        )
        Search(
            prePopulatedText = alias.orEmpty(),
            onValueChange = aliasUpdated,
            readOnly = isReadOnly
        )
    }
}

@Composable
fun Footer(
    showBankTransferInfo: Boolean = true,
    buttonText: String,
    buttonState: ButtonState,
    onButtonClick: () -> Unit
) {
    Column {
        if (showBankTransferInfo) {
            ConstraintLayout {
                val (image, title, info) = createRefs()

                Image(
                    imageResource = ImageResource.Local(R.drawable.ic_transfer_bank),
                    modifier = Modifier
                        .constrainAs(image) {
                            top.linkTo(parent.top)
                            start.linkTo(parent.start)
                        }
                )

                SimpleText(
                    text = stringResource(id = R.string.bank_transfer_only),
                    style = ComposeTypographies.Caption2,
                    color = ComposeColors.Body,
                    gravity = ComposeGravities.Start,
                    modifier = Modifier
                        .constrainAs(title) {
                            top.linkTo(image.top)
                            start.linkTo(image.end)
                            end.linkTo(parent.end)
                        }
                        .padding(start = dimensionResource(id = R.dimen.medium_margin))
                        .fillMaxWidth()
                )

                SimpleText(
                    text = stringResource(id = R.string.bank_transfer_only_subtitle),
                    style = ComposeTypographies.Caption1,
                    color = ComposeColors.Body,
                    gravity = ComposeGravities.Start,
                    modifier = Modifier
                        .constrainAs(info) {
                            top.linkTo(title.bottom)
                            start.linkTo(image.end)
                            end.linkTo(parent.end)
                        }
                        .padding(start = dimensionResource(id = R.dimen.medium_margin))
                        .fillMaxWidth()
                )
            }
        }

        PrimaryButton(
            text = buttonText,
            state = buttonState,
            onClick = { onButtonClick() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    top = dimensionResource(id = R.dimen.large_margin),
                    bottom = dimensionResource(id = R.dimen.tiny_margin)
                )
        )
    }
}

@Composable
fun BankInfoItem(
    title: String,
    description: String,
    showBottomDivider: Boolean,
    modifier: Modifier = Modifier
) {
    ConstraintLayout(
        modifier = modifier.fillMaxWidth()
    ) {
        val (topDivider, titleText, descriptionText, bottomDivider) = createRefs()

        HorizontalDivider(
            modifier = Modifier
                .constrainAs(topDivider) {
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    top.linkTo(parent.top)
                    width = Dimension.fillToConstraints
                }
        )

        Text(
            text = AnnotatedString(title),
            style = ComposeTypographies.Body1.toComposeTypography(),
            color = ComposeColors.Body.toComposeColor(),
            textAlign = ComposeGravities.Start.toTextAlignment(),
            modifier = Modifier
                .wrapContentSize()
                .constrainAs(titleText) {
                    top.linkTo(parent.top)
                    start.linkTo(parent.start)
                }
                .padding(vertical = dimensionResource(id = R.dimen.small_margin))
        )

        Text(
            text = description,
            style = ComposeTypographies.Body1.toComposeTypography(),
            color = ComposeColors.Body.toComposeColor(),
            textAlign = ComposeGravities.End.toTextAlignment(),
            modifier = Modifier
                .wrapContentHeight()
                .width(IntrinsicSize.Min)
                .constrainAs(descriptionText) {
                    top.linkTo(parent.top)
                    start.linkTo(titleText.end)
                    end.linkTo(parent.end)
                    width = Dimension.fillToConstraints
                }
                .padding(
                    start = dimensionResource(id = R.dimen.small_margin),
                    top = dimensionResource(id = R.dimen.small_margin),
                    bottom = dimensionResource(id = R.dimen.small_margin)
                )
        )

        if (showBottomDivider) {
            HorizontalDivider(
                modifier = Modifier
                    .constrainAs(bottomDivider) {
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                        top.linkTo(descriptionText.bottom)
                        bottom.linkTo(parent.bottom)
                        width = Dimension.fillToConstraints
                    }
            )
        }
    }
}

@Preview(name = "Alias Input", showBackground = true)
@Composable
fun PreviewAliasInputScreen() {
    LinkWithAliasScreen(
        viewState = BankAliasLinkViewState(
            showAliasInput = true
        ),
        {},
        {},
        {}
    )
}

@Preview(name = "Alias Error", showBackground = true)
@Composable
fun PreviewAliasErrorScreen() {
    LinkWithAliasScreen(
        viewState = BankAliasLinkViewState(
            showAliasInput = false,
            error = AliasError.ServerSideUxError(
                ServerSideUxErrorInfo(
                    id = "123",
                    title = "title",
                    description = "description",
                    iconUrl = "",
                    statusUrl = "",
                    actions = emptyList(),
                    categories = emptyList()
                )
            )
        ),
        aliasUpdated = {},
        searchAccountClicked = {},
        continueClicked = {}
    )
}

@Preview(name = "Alias Details", showBackground = true)
@Composable
fun PreviewAliasDetailsScreen() {
    LinkWithAliasScreen(
        viewState = BankAliasLinkViewState(
            showAliasInput = false,
            aliasInfo = AliasInfo(
                "bankName",
                "alias",
                "accountHolder",
                "accountType",
                "cbu",
                "a realy, realy, realy, realy, realy, realy, realy, really long cuil"
            )
        ),
        {},
        {},
        {}
    )
}
