package com.blockchain.componentlib.filter

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.R
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme

@Composable
fun LabeledFiltersGroup(
    filters: List<LabeledFilterState>,
    modifier: Modifier = Modifier
) {
    val selectedItem = remember {
        mutableStateOf(filters.first { it.state == FilterState.SELECTED })
    }

    Row(
        modifier = modifier.horizontalScroll(rememberScrollState())
    ) {
        filters.forEachIndexed { index, filter ->
            LabeledFilter(
                labelFilteredState = filter,
                selectedItem = selectedItem
            )
            if (index != filters.lastIndex) {
                Spacer(modifier = Modifier.width(8.dp))
            }
        }
    }
}

@Composable
fun LabeledFilter(labelFilteredState: LabeledFilterState, selectedItem: MutableState<LabeledFilterState>) {
    if (labelFilteredState.text == selectedItem.value.text) {
        SelectedFilter(text = labelFilteredState.text)
    } else
        UnSelectedFilter(
            item = labelFilteredState,
            selectedItem = selectedItem
        )
}

@Composable
fun SelectedFilter(text: String) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(size = dimensionResource(R.dimen.small_spacing)))
            .background(
                AppTheme.colors.primary
            )
            .padding(
                horizontal = AppTheme.dimensions.tinySpacing,
                vertical = AppTheme.dimensions.smallestSpacing
            )
            .clickable(
                onClick = {}
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(imageResource = ImageResource.Local(R.drawable.ic_check_light))
        Spacer(modifier = Modifier.width(width = dimensionResource(R.dimen.minuscule_spacing)))
        Text(
            text = text,
            style = AppTheme.typography.body1,
            color = AppTheme.colors.background
        )
    }
}

@Composable
fun UnSelectedFilter(item: LabeledFilterState, selectedItem: MutableState<LabeledFilterState>) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(size = dimensionResource(R.dimen.small_spacing)))
            .background(Color.White)
            .padding(
                horizontal = AppTheme.dimensions.verySmallSpacing,
                vertical = AppTheme.dimensions.smallestSpacing
            )
            .clickable(
                onClick = {
                    item.onSelected()
                    selectedItem.value = item
                }
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = item.text,
            style = AppTheme.typography.body1,
            color = Color.Black
        )
    }
}

@Preview
@Composable
fun LabeledFiltersGroup() {
    AppTheme {
        AppSurface {
            LabeledFiltersGroup(
                filters = listOf(
                    LabeledFilterState(
                        text = "All Prices",
                        onSelected = {},
                        state = FilterState.SELECTED
                    ),
                    LabeledFilterState(
                        text = "Tradable",
                        onSelected = {},
                        state = FilterState.UNSELECTED
                    )
                )
            )
        }
    }
}

@Preview
@Composable
fun SelectedLabeledFilter() {
    AppTheme {
        AppSurface {
            LabeledFilter(
                labelFilteredState = LabeledFilterState(
                    text = "All Prices",
                    onSelected = {},
                    state = FilterState.SELECTED
                ),
                selectedItem = remember {
                    mutableStateOf(
                        LabeledFilterState(
                            text = "All Prices",
                            onSelected = {},
                            state = FilterState.SELECTED
                        )
                    )
                }
            )
        }
    }
}

@Preview
@Composable
fun UnSelectedLabeledFilter() {
    AppTheme {
        AppSurface {
            LabeledFilter(
                labelFilteredState = LabeledFilterState(
                    text = "All Prices",
                    onSelected = {},
                    state = FilterState.UNSELECTED
                ),
                selectedItem = remember {
                    mutableStateOf(
                        LabeledFilterState(
                            text = "All Prices",
                            onSelected = {},
                            state = FilterState.SELECTED
                        )
                    )
                }
            )
        }
    }
}

enum class FilterState {
    SELECTED, UNSELECTED
}

class LabeledFilterState(
    val text: String,
    val onSelected: (() -> Unit),
    val state: FilterState,
)
