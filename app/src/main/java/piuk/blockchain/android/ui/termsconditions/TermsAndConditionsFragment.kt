package piuk.blockchain.android.ui.termsconditions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.componentlib.alert.SnackbarType
import com.blockchain.componentlib.button.ButtonState
import com.blockchain.componentlib.viewextensions.afterMeasured
import com.blockchain.componentlib.viewextensions.goneIf
import com.blockchain.componentlib.viewextensions.visibleIf
import com.blockchain.core.user.NabuUserDataManager
import com.blockchain.koin.scopedInject
import com.blockchain.notifications.analytics.Analytics
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.Markwon
import io.noties.markwon.core.MarkwonTheme
import io.noties.markwon.recycler.MarkwonAdapter
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.kotlin.subscribeBy
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.FragmentTermsConditionsBinding
import piuk.blockchain.android.ui.customviews.BlockchainSnackbar

class TermsAndConditionsFragment : Fragment() {

    interface Host {
        fun termsAndConditionsSigned()
    }

    private lateinit var binding: FragmentTermsConditionsBinding

    private val disposables = CompositeDisposable()

    private val userDataManager: NabuUserDataManager by scopedInject()
    private val analytics: Analytics by inject()

    private val markdown: String by lazy {
        arguments?.getString(ARG_MARKDOWN).orEmpty()
    }

    private val acceptCheckboxAdapter: AcceptCheckboxAdapter by lazy {
        AcceptCheckboxAdapter(
            onChecked = { isChecked ->
                renderBottomButtonState(isAcceptChecked = isChecked)
            }
        )
    }

    private val host: Host
        get() = requireActivity() as Host

    private var hasReachedEndState: Boolean = false
    private var isAcceptCheckedState: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        analytics.logEvent(TermsAndConditionsAnalytics.Viewed)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentTermsConditionsBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(binding) {
            val markwon = Markwon.builder(requireContext())
                .usePlugin(object : AbstractMarkwonPlugin() {
                    override fun configureTheme(builder: MarkwonTheme.Builder) {
                        builder
                            .headingBreakHeight(0)
                            .isLinkUnderlined(false)
                            .linkColor(ContextCompat.getColor(requireContext(), R.color.blue_600))
                    }
                })
                .build()

            val markdownAdapter = MarkwonAdapter.builderTextViewIsRoot(R.layout.item_markwon_default_entry).build()
            val concatAdapter = ConcatAdapter(markdownAdapter, acceptCheckboxAdapter)

            markdownContainer.layoutManager = LinearLayoutManager(requireContext())
            markdownContainer.adapter = concatAdapter

            markdownAdapter.setMarkdown(markwon, markdown)
            markdownAdapter.notifyDataSetChanged()

            markdownContainer.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    super.onScrollStateChanged(recyclerView, newState)
                    val hasReachedEnd = !recyclerView.canScrollVertically(1)
                    renderBottomButtonState(hasReachedEnd = hasReachedEnd)
                }
            })

            reviewButton.apply {
                onClick = {
                    markdownContainer.smoothScrollToPosition(concatAdapter.itemCount - 1)
                }
                text = getString(R.string.terms_and_conditions_review_button)
            }
            continueButton.apply {
                onClick = {
                    signTermsAndConditions()
                }
                text = getString(R.string.common_continue)
            }
            renderBottomButtonState(hasReachedEnd = false, isAcceptChecked = false)

            bottomCard.afterMeasured {
                markdownContainer.setPadding(0, 0, 0, it.height)
            }
        }
    }

    private fun renderBottomButtonState(
        hasReachedEnd: Boolean = hasReachedEndState,
        isAcceptChecked: Boolean = isAcceptCheckedState
    ) {
        hasReachedEndState = hasReachedEnd
        isAcceptCheckedState = isAcceptChecked
        val isContinueVisible = hasReachedEnd || isAcceptChecked
        binding.reviewButton.goneIf { isContinueVisible }
        binding.continueButton.visibleIf { isContinueVisible }
        binding.continueButton.buttonState = if (isAcceptChecked) ButtonState.Enabled else ButtonState.Disabled
    }

    private fun signTermsAndConditions() {
        disposables += userDataManager.signLatestTermsAndConditions()
            .doOnSubscribe {
                binding.continueButton.buttonState = ButtonState.Loading
                acceptCheckboxAdapter.isEnabled = false
            }
            .doOnTerminate {
                binding.continueButton.buttonState = ButtonState.Enabled
                acceptCheckboxAdapter.isEnabled = true
            }
            .subscribeBy(
                onComplete = {
                    analytics.logEvent(TermsAndConditionsAnalytics.Accepted)
                    host.termsAndConditionsSigned()
                },
                onError = {
                    BlockchainSnackbar.make(
                        binding.root,
                        getString(R.string.common_error),
                        type = SnackbarType.Error
                    ).show()
                }
            )
    }

    override fun onDestroyView() {
        disposables.clear()
        super.onDestroyView()
    }

    companion object {
        private const val ARG_MARKDOWN = "ARG_MARKDOWN"

        fun newInstance(markdown: String): TermsAndConditionsFragment =
            TermsAndConditionsFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_MARKDOWN, markdown)
                }
            }
    }
}
