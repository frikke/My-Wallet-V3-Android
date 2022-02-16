/*
 * Copyright 2019 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package piuk.blockchain.android.ui.home.ui_tour

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import com.blockchain.componentlib.viewextensions.px
import com.google.android.material.R
import com.google.android.material.card.MaterialCardView
import com.google.android.material.shape.RoundedCornerTreatment
import com.google.android.material.shape.ShapeAppearanceModel
import com.google.android.material.shape.ShapeAppearancePathProvider

/**
 * A Card view that clips the content of any shape, this should be done upstream in card,
 * working around it for now.
 */
class UiTourCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = R.attr.materialCardViewStyle
) : MaterialCardView(context, attrs, defStyle) {
    @SuppressLint("RestrictedApi")
    private val pathProvider = ShapeAppearancePathProvider()
    private val path: Path = Path()
    private val defaultShapeAppearanceBuilder: ShapeAppearanceModel.Builder
        get() = this.shapeAppearanceModel.toBuilder()
            .setAllCorners(RoundedCornerTreatment())
            .setAllCornerSizes(8.px.toFloat())

    private val rectF = RectF(0f, 0f, 0f, 0f)

    fun updateEdgeTriangle(indexSelected: Int, maxIndex: Int) {
        shapeAppearanceModel = createShapeAppearanceModel(indexSelected, maxIndex)
    }

    private fun createShapeAppearanceModel(indexSelected: Int, maxIndex: Int): ShapeAppearanceModel {
        val translate = indexSelected.toFloat() / maxIndex.toFloat()
        // We have to invert because we're drawing on the bottom edge which has the coordinates flipped
        val inverted = 1.0f - translate
        return defaultShapeAppearanceBuilder
            .setBottomEdge(
                TriangleTranslationEdgeTreatment(
                    triangleHeight = 12.px.toFloat(),
                    inset = 4.px.toFloat(),
                    translate = inverted
                )
            )
            .build()
    }

    override fun onDraw(canvas: Canvas) {
        canvas.clipPath(path)
        super.onDraw(canvas)
    }

    @SuppressLint("RestrictedApi")
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        rectF.right = w.toFloat()
        rectF.bottom = h.toFloat()
        pathProvider.calculatePath(defaultShapeAppearanceBuilder.build(), 1f, rectF, path)
        super.onSizeChanged(w, h, oldw, oldh)
    }
}
