package piuk.blockchain.blockchain_component_library_catalog.abstract_compose_view

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.blockchain.componentlib.tag.TagSize
import com.blockchain.componentlib.tag.TagType
import com.blockchain.componentlib.tag.TagViewState
import com.blockchain.componentlib.tag.TagsRowView
import piuk.blockchain.blockchain_component_library_catalog.R

class TagsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tags)

        findViewById<TagsRowView>(R.id.tags_row).apply {
            tags = listOf(
                TagViewState(
                    value = "Default",
                    type = TagType.Default()
                ),
                TagViewState(
                    value = "Success",
                    type = TagType.Success()
                ),
                TagViewState(
                    value = "Warning",
                    type = TagType.Warning()
                ),
                TagViewState(
                    value = "Error",
                    type = TagType.Error()
                ),
                TagViewState(
                    value = "Info Alt",
                    type = TagType.InfoAlt()
                ),
            )
        }

        findViewById<TagsRowView>(R.id.large_tags_row).apply {
            tags = listOf(
                TagViewState(
                    value = "Default Large",
                    type = TagType.Default(size = TagSize.Large)
                ),
                TagViewState(
                    value = "Success Large",
                    type = TagType.Success(size = TagSize.Large)
                ),
                TagViewState(
                    value = "Warning Large",
                    type = TagType.Warning(size = TagSize.Large)
                ),
                TagViewState(
                    value = "Error Large",
                    type = TagType.Error(size = TagSize.Large)
                ),
                TagViewState(
                    value = "Info Alt Large",
                    type = TagType.InfoAlt(size = TagSize.Large)
                ),
            )
        }
    }
}