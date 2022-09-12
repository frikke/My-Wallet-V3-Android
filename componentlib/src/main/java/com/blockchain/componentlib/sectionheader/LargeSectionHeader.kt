@file:JvmName("LargeSectionHeaderTypeKt")

package com.blockchain.componentlib.sectionheader

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.R
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Dark400
import com.blockchain.componentlib.theme.Grey400

@Composable
fun LargeSectionHeader(
    headerType: LargeSectionHeaderType,
    modifier: Modifier = Modifier,
    isDarkMode: Boolean = isSystemInDarkTheme(),
) {
    Row(
        modifier = modifier.padding(vertical = 14.dp, horizontal = dimensionResource(R.dimen.medium_spacing)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = headerType.title,
            style = AppTheme.typography.paragraph2,
            color = AppTheme.colors.body,
            modifier = Modifier.weight(1f)
        )
        when (headerType) {
            is LargeSectionHeaderType.Filter -> {
                var optionSelected by remember(headerType) {
                    mutableStateOf(headerType.optionIndexSelected)
                }

                headerType.options.forEachIndexed { index, optionName ->
                    Text(
                        text = optionName,
                        style = AppTheme.typography.paragraph2,
                        color = if (index == optionSelected) {
                            AppTheme.colors.primary
                        } else {
                            if (isDarkMode) Grey400 else Dark400
                        },
                        modifier = Modifier.clickable {
                            optionSelected = index
                            headerType.onOptionSelected(index)
                        }
                    )

                    if (index != headerType.options.lastIndex) {
                        Spacer(Modifier.width(AppTheme.dimensions.tinySpacing))
                    }
                }
            }
            is LargeSectionHeaderType.Icon -> {
                Image(
                    imageResource = headerType.icon,
                    modifier = Modifier.clickable { headerType.onIconClicked() }
                )
            }
            is LargeSectionHeaderType.Default -> {
                /* no-op */
            }
        }
    }
}

@Preview
@Composable
private fun LargeSectionHeaderPreview() {
    AppTheme {
        AppSurface {
            LargeSectionHeader(
                headerType = LargeSectionHeaderType.Default(
                    title = "Destination Address",
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Preview
@Composable
private fun LargeSectionHeaderIconPreview() {
    AppTheme {
        AppSurface {
            LargeSectionHeader(
                headerType = LargeSectionHeaderType.Icon(
                    title = "Destination Address",
                    icon = ImageResource.Local(
                        id = R.drawable.ic_qr_code,
                        contentDescription = null,
                    ),
                    onIconClicked = {}
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Preview
@Composable
private fun LargeSectionHeaderFilterPreview() {
    var selected by remember { mutableStateOf(0) }
    AppTheme {
        AppSurface {
            LargeSectionHeader(
                headerType = LargeSectionHeaderType.Filter(
                    title = "Destination Address",
                    options = listOf("USD", "GBP", "EUR"),
                    onOptionSelected = { selected = it },
                    optionIndexSelected = selected
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
