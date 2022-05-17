package piuk.blockchain.blockchain_component_library_catalog.preview.expandables

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.expandables.ExpandableItem
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme

@Preview(name = "expandable item", group = "Expandable")
@Composable
fun ExpandablesPreview() {
    AppTheme {
        AppSurface {
            ExpandableItem(
                text = "Lorem Ipsum is simply dummy text of the printing and typesetting industry. Lorem Ipsum " +
                    "has been the industry's standard dummy text ever since the 1500s, when an unknown printer " +
                    "took a galley of type and scrambled it to make a type specimen book. ",
                numLinesVisible = 3,
                textButtonToExpand = "Read More",
                textButtonToCollapse = "Read Less"
            )
        }
    }
}
