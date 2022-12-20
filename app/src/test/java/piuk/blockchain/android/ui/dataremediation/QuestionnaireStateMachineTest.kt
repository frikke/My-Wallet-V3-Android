package piuk.blockchain.android.ui.dataremediation

import android.os.Looper
import com.blockchain.domain.dataremediation.model.NodeId
import io.mockk.every
import io.mockk.mockkStatic
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.ReplaySubject
import java.util.UUID
import org.junit.Before
import org.junit.Test

class QuestionnaireStateMachineTest {

    private val subject = QuestionnaireStateMachine()

    @Before
    fun setup() {
        // This is need because we're asserting that StateMachine is only used in the main thread
        mockkStatic(Looper::class)
        every { Looper.myLooper() } returns null
        every { Looper.getMainLooper() } returns null
    }

    @Test
    fun `setting root should emit new state`() {
        val state = subject.stateAsObservable()
        val root = root(
            singleSelection(
                id = "ss1", isDropdown = true,
                selection(id = "ss1-s1"),
                selection(id = "ss1-s2")
            ),
            singleSelection(
                id = "ss2", isDropdown = false,
                selection(id = "ss2-s1"),
                selection(id = "ss2-s2")
            ),
            multipleSelection(
                id = "ms1", isDropdown = false,
                selection(id = "ms1-s1"),
                selection(id = "ms1-s2")
            ),
            openEnded(id = "oe1")
        )
        subject.setRoot(root)

        state.test()
            .assertValue {
                it.root == root &&
                    it.presentables[0].let {
                        (
                            (it.id == "ss1") && it is FlatNode.Dropdown &&
                                it.choices[0].let { it.id == "ss1-s1" } &&
                                it.choices[1].let { it.id == "ss1-s2" }
                            )
                    } &&
                    it.presentables[1].let { ((it.id == "ss2") && it is FlatNode.SingleSelection) } &&
                    it.presentables[2].let { ((it.id == "ss2-s1") && it is FlatNode.Selection) } &&
                    it.presentables[3].let { ((it.id == "ss2-s2") && it is FlatNode.Selection) } &&
                    it.presentables[4].let { ((it.id == "ms1") && it is FlatNode.MultipleSelection) } &&
                    it.presentables[5].let { ((it.id == "ms1-s1") && it is FlatNode.Selection) } &&
                    it.presentables[6].let { ((it.id == "ms1-s2") && it is FlatNode.Selection) } &&
                    it.presentables[7].let { ((it.id == "oe1") && it is FlatNode.OpenEnded) }
            }
    }

    @Test
    fun `dropdown should not render their children individually but inside the choices field and have a null selected choice by default`() {
        val state = subject.stateAsObservable()
        val root = root(
            singleSelection(
                id = "ss1", isDropdown = true,
                selection(id = "ss1-s1"),
                selection(id = "ss1-s2")
            ),
            multipleSelection(
                id = "ss2", isDropdown = true,
                selection(id = "ss2-s1"),
                selection(id = "ss2-s2")
            )
        )
        subject.setRoot(root)

        state.test()
            .assertValue {
                it.root == root &&
                    it.presentables.size == 2 &&
                    it.presentables.first().let {
                        (
                            (it.id == "ss1") && it is FlatNode.Dropdown && !it.isMultiSelection &&
                                it.choices[0].let { it.id == "ss1-s1" } &&
                                it.choices[1].let { it.id == "ss1-s2" }
                            ) && it.selectedChoices.isEmpty()
                    } &&
                    it.presentables[1].let {
                        (
                            (it.id == "ss2") && it is FlatNode.Dropdown && it.isMultiSelection &&
                                it.choices[0].let { it.id == "ss2-s1" } &&
                                it.choices[1].let { it.id == "ss2-s2" }
                            ) && it.selectedChoices.isEmpty()
                    }
            }
    }

    @Test
    fun `unchecked selection should not render their children`() {
        val state = subject.stateAsObservable()
        val root = root(
            singleSelection(
                id = "ss1", isDropdown = true,
                selection(id = "ss1-s1"),
                selection(
                    id = "ss1-s2", isChecked = false,
                    openEnded(id = "hidden"),
                    openEnded(id = "hidden")
                )
            ),
            multipleSelection(
                id = "ms1", isDropdown = true,
                selection(id = "ms1-s1"),
                selection(
                    id = "ms1-s2", isChecked = false,
                    selection(id = "hidden"),
                    selection(
                        id = "hidden", isChecked = true,
                        openEnded(id = "hidden"),
                        openEnded(id = "hidden")
                    )
                )
            ),
            openEnded(id = "oe1")
        )
        subject.setRoot(root)

        state.test()
            .assertValue { it.presentables.none { it.id == "hidden" } }
    }

    @Test
    fun `unfilled openended should not render their children`() {
        val state = subject.stateAsObservable()
        val root = root(
            singleSelection(
                id = "ss1", isDropdown = true,
                selection(id = "ss1-s1"),
                selection(
                    id = "ss1-s2", isChecked = true,
                    openEnded(
                        id = "ss1-s2-oe1", input = "", hint = "hint", regex = null,
                        selection(id = "hidden"),
                        selection(id = "hidden")
                    )
                )
            )
        )
        subject.setRoot(root)

        state.test()
            .assertValue { it.presentables.none { it.id == "hidden" } }
    }

    @Test
    fun `single selection without checked children should be invalid`() {
        val state = subject.stateAsObservable()
        val root = root(
            singleSelection(
                id = "ss1", isDropdown = true,
                selection(id = "ss1-s1", isChecked = false),
                selection(id = "ss1-s2", isChecked = false)
            )
        )
        subject.setRoot(root)

        state.test()
            .assertValue { !it.isValid && it.invalidNodes.contains("ss1") }
    }

    @Test
    fun `single selection one checked children should be valid`() {
        val state = subject.stateAsObservable()
        val root = root(
            singleSelection(
                id = "ss1", isDropdown = true,
                selection(id = "ss1-s1", isChecked = true),
                selection(id = "ss1-s2", isChecked = false)
            )
        )
        subject.setRoot(root)

        state.test()
            .assertValue { it.isValid }
    }

    @Test
    fun `multiple selection with checked children should be valid`() {
        val state = subject.stateAsObservable()
        val root = root(
            multipleSelection(
                id = "ss1", isDropdown = false,
                selection(id = "ss1-s1", isChecked = true),
                selection(id = "ss1-s2", isChecked = true)
            )
        )
        subject.setRoot(root)

        state.test()
            .assertValue { it.isValid }
    }

    @Test
    fun `multiple selection without checked children should be invalid`() {
        val state = subject.stateAsObservable()
        val root = root(
            multipleSelection(
                id = "ss1", isDropdown = false,
                selection(id = "ss1-s1", isChecked = false),
                selection(id = "ss1-s2", isChecked = false)
            )
        )
        subject.setRoot(root)

        state.test()
            .assertValue { !it.isValid && it.invalidNodes.contains("ss1") }
    }

    @Test
    fun `unfilled openended should be invalid`() {
        val state = subject.stateAsObservable()
        val root = root(
            singleSelection(
                id = "ss1", isDropdown = true,
                selection(id = "ss1-s1"),
                selection(
                    id = "ss1-s2", isChecked = true,
                    openEnded(id = "oe1", input = ""),
                )
            )
        )
        subject.setRoot(root)

        state.test()
            .assertValue { !it.isValid && it.invalidNodes.contains("oe1") }
    }

    @Test
    fun `children of unchecked selection should not be validated`() {
        val state = subject.stateAsObservable()
        val root = root(
            singleSelection(
                id = "ss1", isDropdown = true,
                selection(id = "ss1-s1", isChecked = true),
                selection(
                    id = "ss1-s2", isChecked = false,
                    openEnded(id = "oe1"),
                )
            )
        )
        subject.setRoot(root)

        state.test()
            .assertValue { it.isValid }
    }

    @Test
    fun `invalid form should be invalid and contain invalid nodes`() {
        val state = subject.stateAsObservable()
        val root = root(
            singleSelection(
                id = "ss1", isDropdown = true,
                selection(id = "ss1-s1"),
                selection(
                    id = "ss1-s2", isChecked = false,
                    openEnded(id = "hidden"),
                    openEnded(id = "hidden")
                )
            ),
            multipleSelection(
                id = "ms1", isDropdown = true,
                selection(id = "ms1-s1"),
                selection(
                    id = "ms1-s2", isChecked = false,
                    selection(id = "hidden"),
                    selection(
                        id = "hidden", isChecked = true,
                        openEnded(id = "hidden"),
                        openEnded(id = "hidden")
                    )
                )
            ),
            openEnded(id = "oe1")
        )
        subject.setRoot(root)

        state.test()
            .assertValue { !it.isValid && it.invalidNodes.contains("ss1") }
    }

    // Helper
    private fun QuestionnaireStateMachine.stateAsObservable(): Observable<State> {
        val subject = ReplaySubject.create<State>()
        onStateChanged = {
            subject.onNext(State(it, getRoot(), isValid(), invalidNodes))
        }
        return subject
    }

    private data class State(
        val presentables: List<FlatNode>,
        val root: TreeNode.Root,
        val isValid: Boolean,
        val invalidNodes: List<NodeId>
    )

    private fun randomId(): String = UUID.randomUUID().toString()
    private fun root(vararg children: TreeNode): TreeNode.Root = TreeNode.Root(children.toList())

    private fun singleSelection(
        id: NodeId = randomId(),
        isDropdown: Boolean = false,
        vararg children: TreeNode
    ): TreeNode.SingleSelection =
        TreeNode.SingleSelection(id, id, children.toList(), id, isDropdown)

    private fun multipleSelection(
        id: NodeId = randomId(),
        isDropdown: Boolean = false,
        vararg children: TreeNode
    ): TreeNode.MultipleSelection =
        TreeNode.MultipleSelection(id, id, children.toList(), id, isDropdown)

    private fun openEnded(
        id: NodeId = randomId(),
        input: String = "",
        hint: String = "",
        regex: Regex? = null,
        vararg children: TreeNode
    ): TreeNode.OpenEnded =
        TreeNode.OpenEnded(id, id, children.toList(), input, hint, regex)

    private fun selection(
        id: NodeId = randomId(),
        isChecked: Boolean = false,
        vararg children: TreeNode
    ): TreeNode.Selection =
        TreeNode.Selection(id, id, children.toList(), isChecked)
}
