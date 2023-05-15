package piuk.blockchain.android.ui.customviews.account

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.FiatAccount
import com.blockchain.coincore.MultipleCurrenciesAccountGroup
import com.blockchain.coincore.NonCustodialAccount
import com.blockchain.coincore.SingleAccount
import com.blockchain.coincore.TradingAccount
import com.blockchain.coincore.fiat.FiatCustodialAccount
import com.blockchain.coincore.fiat.LinkedBankAccount
import com.blockchain.commonarch.presentation.base.ActivityIndicator
import com.blockchain.commonarch.presentation.base.trackProgress
import com.blockchain.componentlib.button.ButtonState
import com.blockchain.componentlib.viewextensions.updateItemBackground
import com.blockchain.componentlib.viewextensions.updateSelectableItemBackground
import com.blockchain.domain.paymentmethods.model.FundsLocks
import com.blockchain.extensions.replace
import com.blockchain.presentation.customviews.BlockchainListDividerDecor
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.kotlin.subscribeBy
import java.io.Serializable
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.ItemAccountAddNewBankBinding
import piuk.blockchain.android.databinding.ItemAccountSelectBankBinding
import piuk.blockchain.android.databinding.ItemAccountSelectCryptoBinding
import piuk.blockchain.android.databinding.ItemAccountSelectFiatBinding
import piuk.blockchain.android.databinding.ItemAccountSelectGroupBinding
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.ui.adapters.AdapterDelegatesManager
import piuk.blockchain.android.ui.adapters.DelegationAdapter
import piuk.blockchain.android.ui.customviews.IntroHeaderView
import timber.log.Timber

typealias StatusDecorator = (BlockchainAccount) -> CellDecorator

interface AccountsListItem

data class AccountLocks(
    val fundsLocks: FundsLocks? = null
) : Serializable, AccountsListItem

internal data class SelectableAccountItem(
    val item: AccountListViewItem,
    val isSelected: Boolean
) : AccountsListItem

object AddBankAccountItem : AccountsListItem

class AccountList @JvmOverloads constructor(
    ctx: Context,
    val attr: AttributeSet? = null,
    defStyle: Int = 0
) : RecyclerView(ctx, attr, defStyle) {

    private val disposables = CompositeDisposable()
    private val uiScheduler = AndroidSchedulers.mainThread()
    private var lastSelectedAccount: BlockchainAccount? = null
    var activityIndicator: ActivityIndicator? = null

    init {
        setBackgroundColor(ContextCompat.getColor(context, com.blockchain.common.R.color.grey_000))
        setFadingEdgeLength(resources.getDimensionPixelSize(com.blockchain.componentlib.R.dimen.size_small))
        isVerticalFadingEdgeEnabled = true
        layoutManager = LinearLayoutManager(
            context,
            VERTICAL,
            false
        )
        addItemDecoration(
            BlockchainListDividerDecor(context)
        )
        itemAnimator = null
    }

    // Necessary to make fading edge not affected by padding
    override fun isPaddingOffsetRequired(): Boolean = true
    override fun getTopPaddingOffset(): Int = -paddingTop
    override fun getBottomPaddingOffset(): Int = paddingBottom

    fun initialise(
        source: Single<List<AccountListViewItem>>,
        status: StatusDecorator = { DefaultCellDecorator() },
        accountsLocks: Single<List<AccountLocks>> = Single.just(emptyList()),
        introView: IntroHeaderView? = null,
        shouldShowSelectionStatus: Boolean = false,
        shouldShowAddNewBankAccount: Single<Boolean> = Single.just(false),
        assetAction: AssetAction? = null,
        showTradingAccounts: Boolean = false,
        canSwitchBetweenAccountTypes: Boolean = false
    ) {
        removeAllHeaderDecorations()

        introView?.let {
            addItemDecoration(
                HeaderDecoration.with(context)
                    .parallax(0.5f)
                    .setView(it)
                    .build()
            )
        }

        if (adapter == null) {
            adapter = AccountsDelegateAdapter(
                statusDecorator = status,
                onAccountClicked = { onAccountSelected(it) },
                onLockItemSelected = { onLockItemSelected(it) },
                showSelectionStatus = shouldShowSelectionStatus,
                assetAction = assetAction,
                onAddNewBankAccountClicked = { onAddNewBankAccountClicked() }
            )
        }
        loadItems(
            accountsSource = source,
            accountsLocksSource = accountsLocks,
            showAddNewBankAccount = shouldShowAddNewBankAccount,
            assetAction = assetAction,
            showTradingAccounts = showTradingAccounts,
            canSwitchBetweenAccountTypes = canSwitchBetweenAccountTypes
        )
    }

    fun loadItems(
        accountsSource: Single<List<AccountListViewItem>>,
        accountsLocksSource: Single<List<AccountLocks>>,
        showLoader: Boolean = true,
        showAddNewBankAccount: Single<Boolean> = Single.just(false),
        assetAction: AssetAction? = null,
        showTradingAccounts: Boolean = false,
        canSwitchBetweenAccountTypes: Boolean = false
    ) {
        val loader = if (showLoader) activityIndicator else null
        disposables += Single.zip(
            accountsSource,
            accountsLocksSource.onErrorReturn { emptyList() },
            showAddNewBankAccount
        ) { accounts, extraInfos, showAddBankAccount ->
            LoadedItems(
                accountsSource = accounts,
                accountsLocksSource = extraInfos,
                showAddNewBankAccount = showAddBankAccount && assetAction == AssetAction.FiatWithdraw
            )
        }
            .observeOn(uiScheduler)
            .doOnSubscribe {
                onListLoading()
            }
            .trackProgress(loader)
            .subscribeBy(
                onSuccess = {
                    Timber.d("Account List: Updating list with ${it.accountsSource.size}) selectable accounts")
                    (adapter as? AccountsDelegateAdapter)?.items = it.accountsLocksSource +
                        it.accountsSource
                            .filter { accountItem ->
                                if (canSwitchBetweenAccountTypes) {
                                    if (showTradingAccounts) {
                                        accountItem.account is TradingAccount
                                    } else {
                                        accountItem.account is NonCustodialAccount
                                    }
                                } else {
                                    true
                                }
                            }
                            .map { account -> SelectableAccountItem(account, false) } +
                        listOfNotNull(if (it.showAddNewBankAccount) AddBankAccountItem else null)

                    onListLoaded(it.accountsSource)

                    lastSelectedAccount?.let { account ->
                        updatedSelectedAccount(account)
                        lastSelectedAccount = null
                    }
                },
                onError = {
                    onLoadError(it)
                }
            )
    }

    fun updatedSelectedAccount(selectedAccount: BlockchainAccount) {
        with(adapter as AccountsDelegateAdapter) {
            if (items.isNotEmpty()) {
                val selectableItems = items.filterIsInstance<SelectableAccountItem>()
                val currentSelected = selectableItems.firstOrNull { it.isSelected }
                val newSelected = selectableItems.first { it.item.account == selectedAccount }
                if (currentSelected != null) {
                    val newItem = currentSelected.copy(isSelected = false)
                    items = items.replace(currentSelected, newItem)
                }
                val newItem = newSelected.copy(isSelected = true)
                items = items.replace(newSelected, newItem)
            } else {
                // if list is empty, we're in a race condition between loading and selecting, so store value and check
                // it once items loaded
                lastSelectedAccount = selectedAccount
            }
        }
    }

    fun clearSelectedAccount() {
        (adapter as AccountsDelegateAdapter).items =
            (adapter as AccountsDelegateAdapter).items.map { item ->
                (item as? SelectableAccountItem)?.let { selectableAccountItem ->
                    SelectableAccountItem(
                        selectableAccountItem.item,
                        false
                    )
                } ?: item
            }
    }

    var onLoadError: (Throwable) -> Unit = {}
    var onAccountSelected: (BlockchainAccount) -> Unit = {}
    var onLockItemSelected: (AccountLocks) -> Unit = {}
    var onListLoaded: (List<AccountListViewItem>) -> Unit = {}
    var onListLoading: () -> Unit = {}
    var onAddNewBankAccountClicked: () -> Unit = {}

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        disposables.clear()
    }
}

internal data class LoadedItems(
    val accountsSource: List<AccountListViewItem>,
    val accountsLocksSource: List<AccountLocks>,
    val showAddNewBankAccount: Boolean
)

private class AccountsDelegateAdapter(
    statusDecorator: StatusDecorator,
    onAccountClicked: (BlockchainAccount) -> Unit,
    onLockItemSelected: (AccountLocks) -> Unit,
    onAddNewBankAccountClicked: () -> Unit,
    showSelectionStatus: Boolean,
    assetAction: AssetAction? = null
) : DelegationAdapter<AccountsListItem>(AdapterDelegatesManager(), emptyList()) {

    override var items: List<AccountsListItem> = emptyList()
        set(value) {
            val diffResult =
                DiffUtil.calculateDiff(AccountsDiffUtil(this.items, value))
            field = value
            diffResult.dispatchUpdatesTo(this)
        }

    init {
        with(delegatesManager) {
            addAdapterDelegate(
                CryptoAccountDelegate(
                    statusDecorator,
                    onAccountClicked,
                    showSelectionStatus
                )
            )
            addAdapterDelegate(
                FiatAccountDelegate(
                    statusDecorator,
                    onAccountClicked,
                    showSelectionStatus
                )
            )
            addAdapterDelegate(
                AllWalletsAccountDelegate(
                    statusDecorator,
                    onAccountClicked,
                    compositeDisposable
                )
            )
            addAdapterDelegate(
                BankAccountDelegate(
                    onAccountClicked,
                    showSelectionStatus,
                    assetAction
                )
            )

            addAdapterDelegate(
                AccountLocksDelegate(
                    onLockItemSelected
                )
            )

            addAdapterDelegate(
                AddNewBankAccountDelegate(
                    onAddNewBankAccountClicked
                )
            )
        }
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        super.onViewRecycled(holder)
        (holder as? CryptoSingleAccountViewHolder)?.dispose()
    }
}

private class CryptoAccountDelegate(
    private val statusDecorator: StatusDecorator,
    private val onAccountClicked: (SingleAccount) -> Unit,
    private val showSelectionStatus: Boolean
) : AdapterDelegate<AccountsListItem> {

    override fun isForViewType(items: List<AccountsListItem>, position: Int): Boolean =
        (items[position] as? SelectableAccountItem)?.item?.type == AccountsListViewItemType.Crypto

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        CryptoSingleAccountViewHolder(
            showSelectionStatus,
            ItemAccountSelectCryptoBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )

    override fun onBindViewHolder(
        items: List<AccountsListItem>,
        position: Int,
        holder: RecyclerView.ViewHolder
    ) = (holder as CryptoSingleAccountViewHolder).bind(
        items[position] as SelectableAccountItem,
        statusDecorator,
        onAccountClicked,
        isFirstItemInList = if (items[0] is SelectableAccountItem) position == 0 else position == 1,
        isLastItemInList = items.lastIndex == position
    )
}

private class CryptoSingleAccountViewHolder(
    private val showSelectionStatus: Boolean,
    private val binding: ItemAccountSelectCryptoBinding
) : RecyclerView.ViewHolder(binding.root), DisposableViewHolder {

    fun bind(
        selectableAccountItem: SelectableAccountItem,
        statusDecorator: StatusDecorator,
        onAccountClicked: (SingleAccount) -> Unit,
        isFirstItemInList: Boolean,
        isLastItemInList: Boolean
    ) {
        with(binding) {
            cryptoAccount.updateBackground(
                isFirstItemInList = isFirstItemInList,
                isLastItemInList = isLastItemInList,
                isSelected = showSelectionStatus && selectableAccountItem.isSelected
            )

            cryptoAccount.updateItem(
                item = selectableAccountItem.item,
                onAccountClicked = onAccountClicked,
                cellDecorator = statusDecorator(selectableAccountItem.item.account)
            )
        }
    }

    override fun dispose() {
        binding.cryptoAccount.dispose()
    }
}

private class FiatAccountDelegate(
    private val statusDecorator: StatusDecorator,
    private val onAccountClicked: (FiatAccount) -> Unit,
    private val showSelectionStatus: Boolean
) : AdapterDelegate<AccountsListItem> {
    override fun isForViewType(items: List<AccountsListItem>, position: Int): Boolean =
        (items[position] as? SelectableAccountItem)?.item?.account is FiatCustodialAccount

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        FiatAccountViewHolder(
            showSelectionStatus,
            ItemAccountSelectFiatBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )

    override fun onBindViewHolder(items: List<AccountsListItem>, position: Int, holder: RecyclerView.ViewHolder) =
        (holder as FiatAccountViewHolder).bind(
            items[position] as SelectableAccountItem,
            statusDecorator,
            onAccountClicked,
            isFirstItemInList = position == 0,
            isLastItemInList = items.lastIndex == position
        )
}

private class FiatAccountViewHolder(
    private val showSelectionStatus: Boolean,
    private val binding: ItemAccountSelectFiatBinding
) : RecyclerView.ViewHolder(binding.root), DisposableViewHolder {

    fun bind(
        selectableAccountItem: SelectableAccountItem,
        statusDecorator: StatusDecorator,
        onAccountClicked: (FiatAccount) -> Unit,
        isFirstItemInList: Boolean,
        isLastItemInList: Boolean
    ) {
        with(binding) {
            if (showSelectionStatus) {
                fiatContainer.updateSelectableItemBackground(
                    isFirstItemInList,
                    isLastItemInList,
                    selectableAccountItem.isSelected
                )
            } else {
                fiatContainer.updateItemBackground(isFirstItemInList, isLastItemInList)
            }
            fiatContainer.alpha = 1f
            fiatAccount.updateAccount(
                selectableAccountItem.item.account as FiatAccount,
                statusDecorator(selectableAccountItem.item.account),
                onAccountClicked
            )
        }
    }

    override fun dispose() {
        binding.fiatAccount.dispose()
    }
}

private class BankAccountDelegate(
    private val onAccountClicked: (LinkedBankAccount) -> Unit,
    private val showSelectionStatus: Boolean,
    private val assetAction: AssetAction?
) : AdapterDelegate<AccountsListItem> {

    override fun isForViewType(items: List<AccountsListItem>, position: Int): Boolean =
        (items[position] as? SelectableAccountItem)?.item?.account is LinkedBankAccount

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        BankAccountViewHolder(
            showSelectionStatus,
            ItemAccountSelectBankBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )

    override fun onBindViewHolder(
        items: List<AccountsListItem>,
        position: Int,
        holder: RecyclerView.ViewHolder
    ) = (holder as BankAccountViewHolder).bind(
        items[position] as SelectableAccountItem,
        onAccountClicked,
        assetAction,
        isFirstItemInList = position == 0,
        isLastItemInList = items.lastIndex == position
    )
}

private class BankAccountViewHolder(
    private val showSelectionStatus: Boolean,
    private val binding: ItemAccountSelectBankBinding
) : RecyclerView.ViewHolder(binding.root), DisposableViewHolder {

    fun bind(
        selectableAccountItem: SelectableAccountItem,
        onAccountClicked: (LinkedBankAccount) -> Unit,
        assetAction: AssetAction?,
        isFirstItemInList: Boolean,
        isLastItemInList: Boolean
    ) {
        val account = selectableAccountItem.item.account as LinkedBankAccount
        val isDisabled = account.capabilities?.withdrawal?.enabled == false

        with(binding) {
            bankContainer.updateItemBackground(isFirstItemInList, isLastItemInList)

            if (showSelectionStatus) {
                bankContainer.updateSelectableItemBackground(
                    isFirstItemInList,
                    isLastItemInList,
                    selectableAccountItem.isSelected
                )
            } else {
                bankContainer.updateItemBackground(isFirstItemInList, isLastItemInList)
            }
            bankContainer.alpha = if (isDisabled) 0.4f else 1f
            bankAccount.updateAccount(
                account = account,
                action = assetAction,
                onAccountClicked = if (isDisabled) null else onAccountClicked
            )
        }
    }

    override fun dispose() {
        // nothing to dispose
    }
}

private class AddNewBankAccountDelegate(
    private val onAddNewBankAccountButtonClick: () -> Unit
) : AdapterDelegate<AccountsListItem> {

    override fun isForViewType(items: List<AccountsListItem>, position: Int): Boolean =
        items[position] is AddBankAccountItem

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        AddNewBankAccountViewHolder(
            ItemAccountAddNewBankBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )

    override fun onBindViewHolder(items: List<AccountsListItem>, position: Int, holder: RecyclerView.ViewHolder) =
        (holder as AddNewBankAccountViewHolder).bind(
            onAddNewBankAccountButtonClick,
            isFirstItemInList = position == 0,
            isLastItemInList = items.lastIndex == position
        )
}

private class AddNewBankAccountViewHolder(
    private val binding: ItemAccountAddNewBankBinding
) : RecyclerView.ViewHolder(binding.root), DisposableViewHolder {

    fun bind(onAddNewBankAccountButtonClick: () -> Unit, isFirstItemInList: Boolean, isLastItemInList: Boolean) {
        binding.root.updateItemBackground(isFirstItemInList, isLastItemInList)
        with(binding.addNewBankAccountButton) {
            buttonState = ButtonState.Enabled
            text = context.getString(com.blockchain.stringResources.R.string.add_new_bank_account)
            onClick = onAddNewBankAccountButtonClick
        }
    }

    override fun dispose() {
        // nothing to dispose
    }
}

private class AllWalletsAccountDelegate(
    private val statusDecorator: StatusDecorator,
    private val onAccountClicked: (BlockchainAccount) -> Unit,
    private val compositeDisposable: CompositeDisposable
) : AdapterDelegate<AccountsListItem> {

    override fun isForViewType(items: List<AccountsListItem>, position: Int): Boolean =
        (items[position] as? SelectableAccountItem)?.item?.account is MultipleCurrenciesAccountGroup

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        MultipleCurrenciesAccountGroupViewHolder(
            compositeDisposable,
            ItemAccountSelectGroupBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )

    override fun onBindViewHolder(
        items: List<AccountsListItem>,
        position: Int,
        holder: RecyclerView.ViewHolder
    ) = (holder as MultipleCurrenciesAccountGroupViewHolder).bind(
        items[position] as SelectableAccountItem,
        statusDecorator,
        onAccountClicked
    )
}

private class MultipleCurrenciesAccountGroupViewHolder(
    private val compositeDisposable: CompositeDisposable,
    private val binding: ItemAccountSelectGroupBinding
) : RecyclerView.ViewHolder(binding.root), DisposableViewHolder {

    fun bind(
        selectableAccountItem: SelectableAccountItem,
        statusDecorator: StatusDecorator,
        onAccountClicked: (BlockchainAccount) -> Unit
    ) {
        with(binding) {
            accountGroup.updateAccount(selectableAccountItem.item.account as MultipleCurrenciesAccountGroup)
            accountGroup.alpha = 1f

            compositeDisposable += statusDecorator(selectableAccountItem.item.account).isEnabled()
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { root.setOnClickListener { } }
                .subscribeBy(
                    onSuccess = { isEnabled ->
                        if (isEnabled) {
                            root.setOnClickListener { onAccountClicked(selectableAccountItem.item.account) }
                            accountGroup.alpha = 1f
                        } else {
                            accountGroup.alpha = .6f
                            root.setOnClickListener { }
                        }
                    }
                )
        }
    }

    override fun dispose() {
        binding.accountGroup.dispose()
    }
}

interface DisposableViewHolder {
    fun dispose()
}
