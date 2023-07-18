package com.blockchain.componentlib.system

import android.content.res.Configuration
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.Card
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.icons.Bell
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.theme.AppColors
import com.blockchain.componentlib.theme.AppTheme

data class DialogueButton(
    val text: String,
    val showIndication: Boolean = false,
    val onClick: () -> Unit
)

@Composable
fun DialogueCard(
    icon: ImageResource.Local? = null,
    title: String? = null,
    body: String,
    firstButton: DialogueButton,
    secondButton: DialogueButton? = null,
    onDismissRequest: () -> Unit = {},
    properties: DialogProperties = DialogProperties()
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = properties
    ) {
        Card(
            elevation = 2.dp,
            shape = AppTheme.shapes.small,
            backgroundColor = AppColors.backgroundSecondary,
            modifier = Modifier.padding(dimensionResource(com.blockchain.componentlib.R.dimen.smallest_spacing))
        ) {
            Surface(
                color = AppColors.backgroundSecondary
            ) {
                Column {
                    Column(
                        modifier = Modifier.padding(
                            top = AppTheme.dimensions.standardSpacing,
                            bottom = AppTheme.dimensions.smallSpacing,
                            start = AppTheme.dimensions.standardSpacing,
                            end = AppTheme.dimensions.standardSpacing,
                        ),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        icon?.let {
                            Image(it.withSize(AppTheme.dimensions.standardSpacing))
                            Spacer(modifier = Modifier.size(AppTheme.dimensions.tinySpacing))
                        }

                        if (title != null) {
                            Text(
                                modifier = Modifier.fillMaxWidth(),
                                text = title,
                                style = AppTheme.typography.body2,
                                color = AppColors.title,
                                textAlign = icon?.let { TextAlign.Center } ?: TextAlign.Start
                            )
                            Spacer(modifier = Modifier.size(AppTheme.dimensions.smallSpacing))
                        }

                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            text = body,
                            style = AppTheme.typography.paragraph1,
                            color = AppColors.body
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(AppTheme.dimensions.tinySpacing),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = firstButton.onClick
                        ) {
                            Text(
                                firstButton.text,
                                style = AppTheme.typography.paragraph2,
                                color = AppTheme.colors.primary
                            )
                        }

                        if (secondButton != null) {
                            Box(contentAlignment = Alignment.Center) {
                                TextButton(
                                    onClick = secondButton.onClick,
                                ) {
                                    Text(
                                        secondButton.text,
                                        style = AppTheme.typography.paragraph2,
                                        color = AppTheme.colors.primary
                                    )
                                }

                                if (secondButton.showIndication) {
                                    val indicationColor = AppTheme.colors.primary
                                    Canvas(modifier = Modifier.wrapContentSize()) {
                                        drawCircle(
                                            color = indicationColor,
                                            radius = 30.dp.toPx(),
                                            style = Stroke(width = 1.dp.toPx())
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun DialogueCardPreview() {
    DialogueCard(
        title = "Some title",
        body = "Body 2: Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ",
        firstButton = DialogueButton("Button 1", false, {}),
        secondButton = DialogueButton("Button2", false, {})
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DialogueCardPreviewDark() {
    DialogueCardPreview()
}

@Preview
@Composable
private fun DialogueCardIconPreview() {
    DialogueCard(
        icon = Icons.Filled.Bell.withTint(AppColors.primary),
        title = "Some title",
        body = "Body 2: Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ",
        firstButton = DialogueButton("Button 1", false, {}),
        secondButton = DialogueButton("Button2", false, {})
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DialogueCardIconPreviewDark() {
    DialogueCardIconPreview()
}
