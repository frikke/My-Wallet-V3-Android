package piuk.blockchain.blockchain_component_library_catalog.abstract_compose_view

import android.content.Context
import android.content.res.Resources
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity.CENTER
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.view.setPadding
import com.blockchain.componentlib.image.ImageResource
import com.blockchain.componentlib.sheets.SheetHeaderActionType
import com.blockchain.componentlib.sheets.SheetHeaderBackAndActionView
import com.blockchain.componentlib.sheets.SheetHeaderBackAndCloseView
import com.blockchain.componentlib.sheets.SheetHeaderView
import com.blockchain.componentlib.sheets.SheetNubView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import piuk.blockchain.blockchain_component_library_catalog.R

class SheetActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sheet)

        findViewById<MaterialButton>(R.id.default_sheet).apply {
            setOnClickListener {
                DefaultBottomSheetDialogFragment().show(supportFragmentManager, "default_sheet")
            }
        }
        findViewById<MaterialButton>(R.id.byline).apply {
            setOnClickListener {
                BylineBottomSheetDialogFragment().show(supportFragmentManager, "apply")
            }
        }
        findViewById<MaterialButton>(R.id.icon).apply {
            setOnClickListener {
                IconBottomSheetDialogFragment().show(supportFragmentManager, "icon")
            }
        }
        findViewById<MaterialButton>(R.id.back).apply {
            setOnClickListener {
                BackDefaultBottomSheetDialogFragment().show(supportFragmentManager, "icon")
            }
        }
        findViewById<MaterialButton>(R.id.back_byline).apply {
            setOnClickListener {
                BackBylineBottomSheetDialogFragment().show(supportFragmentManager, "icon")
            }
        }
        findViewById<MaterialButton>(R.id.back_cancel).apply {
            setOnClickListener {
                BackCancelBottomSheetDialogFragment().show(supportFragmentManager, "icon")
            }
        }
        findViewById<MaterialButton>(R.id.back_next).apply {
            setOnClickListener {
                BackNextBottomSheetDialogFragment().show(supportFragmentManager, "icon")
            }
        }
    }
}

private val Number.toPx
    get() = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        this.toFloat(),
        Resources.getSystem().displayMetrics
    )

class DefaultBottomSheetDialogFragment() : BottomSheetDialogFragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return BottomSheetDummyView(
            bottomSheetHeaderView = SheetHeaderView(requireContext())
                .apply {
                    this.title = "Title"
                },
            context = requireContext(),
        )
    }
}

class BylineBottomSheetDialogFragment() : BottomSheetDialogFragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return BottomSheetDummyView(
            bottomSheetHeaderView = SheetHeaderView(requireContext())
                .apply {
                    this.title = "Title"
                    this.byline = "Byline"
                },
            context = requireContext(),
        )
    }
}

class IconBottomSheetDialogFragment() : BottomSheetDialogFragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return BottomSheetDummyView(
            bottomSheetHeaderView = SheetHeaderView(requireContext())
                .apply {
                    this.title = "Title"
                    this.startImageResource = ImageResource.Local(
                        id = R.drawable.ic_qr_code,
                        contentDescription = null,
                    )
                },
            context = requireContext(),
        )
    }
}

class BackDefaultBottomSheetDialogFragment() : BottomSheetDialogFragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return BottomSheetDummyView(
            bottomSheetHeaderView = SheetHeaderBackAndCloseView(requireContext())
                .apply {
                    this.title = "Title"
                },
            context = requireContext(),
        )
    }
}

class BackBylineBottomSheetDialogFragment() : BottomSheetDialogFragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return BottomSheetDummyView(
            bottomSheetHeaderView = SheetHeaderBackAndCloseView(requireContext())
                .apply {
                    this.title = "Title"
                    this.byline = "Byline"
                },
            context = requireContext(),
        )
    }
}

class BackCancelBottomSheetDialogFragment() : BottomSheetDialogFragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return BottomSheetDummyView(
            bottomSheetHeaderView = SheetHeaderBackAndActionView(requireContext())
                .apply {
                    this.title = "Title"
                },
            context = requireContext(),
        )
    }
}

class BackNextBottomSheetDialogFragment() : BottomSheetDialogFragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return BottomSheetDummyView(
            bottomSheetHeaderView = SheetHeaderBackAndActionView(requireContext())
                .apply {
                    this.title = "Title"
                    this.actionType = SheetHeaderActionType.Next("Next")
                },
            context = requireContext(),
        )
    }
}

private fun BottomSheetDummyView(bottomSheetHeaderView: View, context: Context) =
    LinearLayoutCompat(context).apply {
        orientation = LinearLayoutCompat.VERTICAL
        addView(SheetNubView(context).apply {
            setPadding(8.toPx.toInt())
            layoutParams = ViewGroup.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
            gravity = CENTER
        })
        addView(bottomSheetHeaderView)
        addView(View(context).apply {
            layoutParams = LinearLayoutCompat.LayoutParams(
                300,
                1000
            )
        })
    }